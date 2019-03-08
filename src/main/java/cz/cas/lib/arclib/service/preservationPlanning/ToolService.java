package cz.cas.lib.arclib.service.preservationPlanning;

import cz.cas.lib.arclib.domain.IngestToolFunction;
import cz.cas.lib.arclib.domain.preservationPlanning.Tool;
import cz.cas.lib.arclib.dto.ToolUpdateDto;
import cz.cas.lib.arclib.mail.ArclibMailCenter;
import cz.cas.lib.arclib.store.ToolStore;
import cz.cas.lib.core.exception.MissingObject;
import cz.cas.lib.core.rest.data.DelegateAdapter;
import cz.cas.lib.core.store.Transactional;
import lombok.Getter;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import static cz.cas.lib.core.util.Utils.notNull;

@Service
public class ToolService implements DelegateAdapter<Tool> {
    @Getter
    private ToolStore delegate;
    private ArclibMailCenter arclibMailCenter;

    @Transactional
    public Tool createNewToolVersionIfNeeded(String toolName, String newVersion, IngestToolFunction function) {
        Tool latestToolVersion = delegate.findLatestToolByName(toolName);
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
        arclibMailCenter.sendNewToolVersionNotification(latestToolVersion, newToolVersion);
        return delegate.save(newToolVersion);
    }

    @Transactional
    public Tool update(ToolUpdateDto dto) {
        Tool tool = find(dto.getId());
        notNull(tool, () -> new MissingObject(getType(), dto.getId()));
        tool.setDescription(dto.getDescription());
        tool.setDocumentation(dto.getDocumentation());
        tool.setLicenseInformation(dto.getLicenseInformation());
        save(tool);
        return tool;
    }

    /**
     * returns the first instance with given name and version.
     *
     * @param name
     * @return
     */
    public Tool findByNameAndVersion(String name, String version) {
        notNull(name, () -> new IllegalArgumentException("name cant be null"));
        return delegate.findByNameAndVersion(name, version);
    }

    @Inject
    public void setToolStore(ToolStore toolStore) {
        this.delegate = toolStore;
    }

    @Inject
    public void setArclibMailCenter(ArclibMailCenter arclibMailCenter) {
        this.arclibMailCenter = arclibMailCenter;
    }
}
