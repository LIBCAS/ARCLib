package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.export.ExportTemplate;
import cz.cas.lib.arclib.domainbase.exception.ForbiddenOperation;
import cz.cas.lib.arclib.exception.BadRequestException;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.store.ExportTemplateStore;
import cz.cas.lib.core.store.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;

import static cz.cas.lib.arclib.utils.ArclibUtils.hasRole;
import static cz.cas.lib.core.util.Utils.eq;
import static cz.cas.lib.core.util.Utils.notNull;

@Slf4j
@Service
public class ExportTemplateService {
    private ExportTemplateStore store;
    private UserDetails userDetails;

    @Transactional
    public ExportTemplate save(ExportTemplate entity) throws IOException {
        notNull(entity.getProducer(), () -> new BadRequestException("ExportTemplate must have producer assigned"));
        // if user is not SUPER_ADMIN then entity must be assigned to user's producer
        if (!hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE)) {
            eq(entity.getProducer().getId(), userDetails.getUser().getProducer().getId(), () -> new ForbiddenOperation("Producer of ExportTemplate must be the same as producer of logged-in user."));
        }

        ExportTemplate exportTemplateFound = store.find(entity.getId());
        if (exportTemplateFound != null) {
            // if user is not SUPER_ADMIN then change of producer is forbidden
            if (!hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE)) {
                eq(entity.getProducer().getId(), exportTemplateFound.getProducer().getId(), () -> new ForbiddenOperation("Cannot change ExportTemplate's Producer"));
            }
        }

        return store.save(entity);
    }

    @Transactional
    public void delete(ExportTemplate entity) {
        store.delete(entity);
    }

    public ExportTemplate find(String id) {
        return store.find(id);
    }

    public ExportTemplate findWithDeletedFilteringOff(String id) {
        return store.findWithDeletedFilteringOff(id);
    }

    public Collection<ExportTemplate> findAll() {
        return store.findAll();
    }

    @Transactional
    public void hardDelete(ExportTemplate entity) {
        store.hardDelete(entity);
    }

    public Collection<ExportTemplate> listExportTemplateDtos() {
        return this.findFilteredByProducer();
    }

    public Collection<ExportTemplate> findFilteredByProducer() {
        if (hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE)) {
            return store.findAll();
        } else {
            return store.findByProducerId(userDetails.getProducerId());
        }
    }

    @Inject
    public void setStore(ExportTemplateStore store) {
        this.store = store;
    }

    @Inject
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }
}
