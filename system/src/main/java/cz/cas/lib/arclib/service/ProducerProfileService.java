package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.ingestWorkflow.WorkflowDefinition;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.domain.profiles.SipProfile;
import cz.cas.lib.arclib.domain.profiles.ValidationProfile;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.dto.ProducerProfileDto;
import cz.cas.lib.arclib.store.ProducerProfileStore;
import cz.cas.lib.arclib.store.SipProfileStore;
import cz.cas.lib.arclib.store.ValidationProfileStore;
import cz.cas.lib.arclib.store.WorkflowDefinitionStore;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import cz.cas.lib.core.rest.data.DelegateAdapter;
import cz.cas.lib.core.store.Transactional;
import lombok.Getter;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

import static cz.cas.lib.arclib.domainbase.util.DomainBaseUtils.notNull;

@Service
public class ProducerProfileService implements DelegateAdapter<ProducerProfile> {
    @Getter
    private ProducerProfileStore delegate;
    private SipProfileStore sipProfileStore;
    private ValidationProfileStore validationProfileStore;
    private WorkflowDefinitionStore workflowDefinitionStore;
    private BeanMappingService beanMappingService;

    @Transactional
    @Override
    public void delete(ProducerProfile entity) {
        delegate.delete(entity);
    }

    @Transactional
    @Override
    public void hardDelete(ProducerProfile entity) {
        delegate.hardDelete(entity);
    }

    @Transactional
    @Override
    public Collection<? extends ProducerProfile> save(Collection<? extends ProducerProfile> entities) {
        return delegate.save(entities);
    }

    /**
     * Saves producer profile and if attribute <code>debuggingMode</code> equals 'false', sets respective SIP profile as non editable
     *
     * @param producerProfile producer profile
     * @return created producer profile
     */
    @Transactional
    @Override
    public ProducerProfile save(ProducerProfile producerProfile) {
        if (!producerProfile.isDebuggingModeActive()) {

            SipProfile sipProfileFound = sipProfileStore.find(producerProfile.getSipProfile().getId());
            notNull(sipProfileFound, () -> new MissingObject(SipProfile.class, producerProfile.getSipProfile().getId()));
            if (sipProfileFound.isEditable()) {
                sipProfileFound.setEditable(false);
                sipProfileStore.save(sipProfileFound);
            }

            ValidationProfile validationProfileFound = validationProfileStore.find(producerProfile.getValidationProfile().getId());
            notNull(validationProfileFound, () -> new MissingObject(ValidationProfile.class, producerProfile.getValidationProfile().getId()));
            if (validationProfileFound.isEditable()) {
                validationProfileFound.setEditable(false);
                validationProfileStore.save(validationProfileFound);
            }

            WorkflowDefinition workflowDefinitionFound = workflowDefinitionStore.find(producerProfile.getWorkflowDefinition().getId());
            notNull(workflowDefinitionFound, () -> new MissingObject(WorkflowDefinition.class, producerProfile.getWorkflowDefinition().getId()));
            if (workflowDefinitionFound.isEditable()) {
                workflowDefinitionFound.setEditable(false);
                workflowDefinitionStore.save(workflowDefinitionFound);
            }

        }
        return delegate.save(producerProfile);
    }

    public Result<ProducerProfileDto> listProducerProfileDtos(Params params) {
        Result<ProducerProfile> all = delegate.findAll(params);
        List<ProducerProfileDto> allAsDtos = beanMappingService.mapTo(all.getItems(), ProducerProfileDto.class);

        Result<ProducerProfileDto> result = new Result<>();
        result.setItems(allAsDtos);
        result.setCount(all.getCount());
        return result;
    }

    @Transactional
    public ProducerProfile findByExternalId(String number) {
        return delegate.findByExternalId(number);
    }

    public ProducerProfile findWithDeletedFilteringOff(String id) {
        return delegate.findWithDeletedFilteringOff(id);
    }

    @Inject
    public void setBeanMappingService(BeanMappingService beanMappingService) {
        this.beanMappingService = beanMappingService;
    }

    @Inject
    public void setSipProfileStore(SipProfileStore sipProfileStore) {
        this.sipProfileStore = sipProfileStore;
    }

    @Inject
    public void setDelegate(ProducerProfileStore delegate) {
        this.delegate = delegate;
    }

    @Inject
    public void setValidationProfileStore(ValidationProfileStore validationProfileStore) {
        this.validationProfileStore = validationProfileStore;
    }

    @Inject
    public void setWorkflowDefinitionStore(WorkflowDefinitionStore workflowDefinitionStore) {
        this.workflowDefinitionStore = workflowDefinitionStore;
    }
}
