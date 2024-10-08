package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.profiles.ValidationProfile;
import cz.cas.lib.arclib.domainbase.exception.ForbiddenOperation;
import cz.cas.lib.arclib.dto.ValidationProfileDto;
import cz.cas.lib.arclib.exception.BadRequestException;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.store.ValidationProfileStore;
import cz.cas.lib.arclib.utils.XmlUtils;
import cz.cas.lib.core.store.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import static cz.cas.lib.arclib.utils.ArclibUtils.hasRole;
import static cz.cas.lib.core.util.Utils.eq;
import static cz.cas.lib.core.util.Utils.notNull;

@Service
public class ValidationProfileService {
    private ValidationProfileStore store;
    private BeanMappingService beanMappingService;
    private Resource validationProfileSchema;
    private UserDetails userDetails;

    @Transactional
    public ValidationProfile save(ValidationProfile entity) throws IOException {
        notNull(entity.getProducer(), () -> new BadRequestException("ValidationProfile must have producer assigned"));
        // if user is not SUPER_ADMIN then entity must be assigned to user's producer
        if (!hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE)) {
            eq(entity.getProducer().getId(), userDetails.getUser().getProducer().getId(), () -> new ForbiddenOperation("Producer of ValidationProfile must be the same as producer of logged-in user."));
        }

        ValidationProfile validationProfileFound = store.find(entity.getId());
        if (validationProfileFound != null) {
            // if user is not SUPER_ADMIN then change of producer is forbidden
            if (!hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE)) {
                eq(entity.getProducer().getId(), validationProfileFound.getProducer().getId(), () -> new ForbiddenOperation("Cannot change ValidationProfile's Producer"));
            }
            if (!validationProfileFound.isEditable()) {
                throw new ForbiddenOperation(ValidationProfile.class, entity.getId());
            }
        }

        String validationProfileXml = entity.getXml();
        XmlUtils.validateWithXMLSchema(validationProfileXml, new InputStream[]{validationProfileSchema.getInputStream()}, "Validation profile XSD");

        entity.setEditable(true);
        return store.save(entity);
    }

    @Transactional
    public void delete(ValidationProfile entity) {
        store.delete(entity);
    }

    public ValidationProfile find(String id) {
        return store.find(id);
    }

    public ValidationProfile findWithDeletedFilteringOff(String id) {
        return store.findWithDeletedFilteringOff(id);
    }

    public Collection<ValidationProfile> findAll() {
        return store.findAll();
    }

    @Transactional
    public void hardDelete(ValidationProfile entity) {
        store.hardDelete(entity);
    }

    public Collection<ValidationProfileDto> listValidationProfileDtos() {
        Collection<ValidationProfile> all = this.findFilteredByProducer();
        return beanMappingService.mapTo(all, ValidationProfileDto.class);
    }

    public Collection<ValidationProfile> findFilteredByProducer() {
        if (hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE)) {
            return store.findAll();
        } else {
            return store.findByProducerId(userDetails.getProducerId());
        }
    }


    @Autowired
    public void setBeanMappingService(BeanMappingService beanMappingService) {
        this.beanMappingService = beanMappingService;
    }

    @Autowired
    public void setStore(ValidationProfileStore store) {
        this.store = store;
    }

    @Autowired
    public void setValidationProfileSchema(@Value("${arclib.validationProfileSchema}") Resource validationProfileSchema) {
        this.validationProfileSchema = validationProfileSchema;
    }

    @Autowired
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }


}
