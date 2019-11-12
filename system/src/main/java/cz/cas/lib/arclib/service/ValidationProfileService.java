package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.profiles.ValidationProfile;
import cz.cas.lib.arclib.dto.ValidationProfileDto;
import cz.cas.lib.arclib.store.ValidationProfileStore;
import cz.cas.lib.core.store.Transactional;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Collection;

@Service
public class ValidationProfileService {
    private ValidationProfileStore store;
    private BeanMappingService beanMappingService;

    @Transactional
    public ValidationProfile save(ValidationProfile entity) {
        return store.save(entity);
    }

    @Transactional
    public void delete(ValidationProfile entity) {
        store.delete(entity);
    }

    public ValidationProfile find(String id) {
        return store.find(id);
    }

    public Collection<ValidationProfile> findAll() {
        return store.findAll();
    }

    @Transactional
    public void hardDelete(ValidationProfile entity) {
        store.hardDelete(entity);
    }

    public Collection<ValidationProfileDto> listValidationProfileDtos() {
        Collection<ValidationProfile> all = store.findAll();
        return beanMappingService.mapTo(all, ValidationProfileDto.class);
    }

    @Inject
    public void setBeanMappingService(BeanMappingService beanMappingService) {
        this.beanMappingService = beanMappingService;
    }

    @Inject
    public void setStore(ValidationProfileStore store) {
        this.store = store;
    }
}
