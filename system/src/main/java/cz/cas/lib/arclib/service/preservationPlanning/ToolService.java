package cz.cas.lib.arclib.service.preservationPlanning;

import cz.cas.lib.arclib.domain.IngestToolFunction;
import cz.cas.lib.arclib.domain.preservationPlanning.Tool;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.dto.ToolUpdateDto;
import cz.cas.lib.arclib.mail.ArclibMailCenter;
import cz.cas.lib.arclib.store.ToolStore;
import cz.cas.lib.core.store.Transactional;
import cz.cas.lib.core.store.TransactionalNew;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Collection;

import static cz.cas.lib.core.util.Utils.notNull;

@Service
@Slf4j
public class ToolService {
    private ToolStore store;
    private ArclibMailCenter arclibMailCenter;

    public Tool save(Tool tool) {
        return store.save(tool);
    }

    public Tool find(String id) {
        return store.find(id);
    }

    public Collection<Tool> findAll() {
        return store.findAll();
    }

    @TransactionalNew
    public Tool createNewToolVersionIfNeeded(String toolName, String newVersion, IngestToolFunction function) {
        Tool latestToolVersion = store.findLatestToolByName(toolName);
        if (latestToolVersion != null && newVersion.trim().equalsIgnoreCase(latestToolVersion.getVersion()))
            return latestToolVersion;
        Tool newToolVersion = new Tool();
        newToolVersion.setName(toolName);
        newToolVersion.setVersion(newVersion.trim());
        newToolVersion.setToolFunction(function);
        if (latestToolVersion != null) {
            newToolVersion.setInternal(latestToolVersion.isInternal());
            newToolVersion.setDescription(latestToolVersion.getDescription());
            newToolVersion.setDocumentation(latestToolVersion.getDocumentation());
            newToolVersion.setFormatRelationType(latestToolVersion.getFormatRelationType());
            newToolVersion.setFormatRelationValue(latestToolVersion.getFormatRelationValue());
            newToolVersion.setLicenseInformation(latestToolVersion.getLicenseInformation());
            newToolVersion.setPossibleIssues(latestToolVersion.getPossibleIssues());
            newToolVersion.setToolFunction(latestToolVersion.getToolFunction());
        }
        Tool tool = store.save(newToolVersion);
        arclibMailCenter.sendNewToolVersionNotification(latestToolVersion, newToolVersion);
        return tool;
    }

    @Transactional
    public Tool update(ToolUpdateDto dto) {
        Tool tool = store.find(dto.getId());
        notNull(tool, () -> new MissingObject(Tool.class, dto.getId()));
        tool.setDescription(dto.getDescription());
        tool.setDocumentation(dto.getDocumentation());
        tool.setLicenseInformation(dto.getLicenseInformation());
        store.save(tool);
        return tool;
    }

    /**
     * @param name    tool name
     * @param version tool version
     * @return the first instance with given name and version
     * @throws IllegalArgumentException if no such tool exists in DB
     */
    public Tool getByNameAndVersion(@NonNull String name, String version) {
        Tool tool = store.findByNameAndVersion(name, version);
        notNull(tool, () -> new IllegalArgumentException("DB record of tool: " + name + " version: " + version + " not found in tool table although it is expected to be present."));
        return tool;
    }

    public Tool findByNameAndVersion(@NonNull String name, String version) {
        return store.findByNameAndVersion(name, version);
    }

    @Inject
    public void setToolStore(ToolStore toolStore) {
        this.store = toolStore;
    }

    @Inject
    public void setArclibMailCenter(ArclibMailCenter arclibMailCenter) {
        this.arclibMailCenter = arclibMailCenter;
    }
}
