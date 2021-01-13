package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.domain.ingestWorkflow.WorkflowDefinition;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.domain.profiles.QProducerProfile;
import cz.cas.lib.arclib.domain.profiles.SipProfile;
import cz.cas.lib.arclib.domain.profiles.ValidationProfile;
import cz.cas.lib.arclib.index.solr.entity.IndexedProducerProfile;
import cz.cas.lib.core.index.solr.IndexedNamedStore;
import cz.cas.lib.core.sequence.Generator;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;

@Repository
public class ProducerProfileStore extends IndexedNamedStore<ProducerProfile, QProducerProfile, IndexedProducerProfile> {

    @Getter
    private final String SEQUENCE_ID = "7c5a958a-7b8b-41f7-a8be-49aa07a13261";
    @Getter
    private final String indexType = "producerProfile";

    private Generator generator;

    public ProducerProfileStore() {
        super(ProducerProfile.class, QProducerProfile.class, IndexedProducerProfile.class);
    }

    @Override
    public ProducerProfile save(ProducerProfile entity) {
        if (entity.getExternalId() == null) {
            entity.setExternalId(generator.generate(SEQUENCE_ID));
        }
        return super.save(entity);
    }

    public ProducerProfile findByExternalId(@NonNull String number) {
        ProducerProfile entity = query().select(qObject()).where(qObject().externalId.eq(number)).fetchOne();
        detachAll();
        return entity;
    }

    @Override
    public IndexedProducerProfile toIndexObject(ProducerProfile obj) {
        IndexedProducerProfile indexObject = super.toIndexObject(obj);

        String externalId = obj.getExternalId();
        if (externalId != null) {
            indexObject.setExternalId(externalId);
        }

        Producer producer = obj.getProducer();
        if (producer != null) {
            indexObject.setProducerName(producer.getName());
            indexObject.setProducerId(producer.getId());
            indexObject.setProducerTransferAreaPath(producer.getTransferAreaPath());
        }

        SipProfile sipProfile = obj.getSipProfile();
        if (sipProfile != null) {
            indexObject.setSipProfileId(sipProfile.getId());
            indexObject.setSipProfileName(sipProfile.getName());
        }

        ValidationProfile validationProfile = obj.getValidationProfile();

        if (validationProfile != null) {
            indexObject.setValidationProfileId(validationProfile.getId());
            indexObject.setValidationProfileName(validationProfile.getName());
        }

        WorkflowDefinition workflowDefinition = obj.getWorkflowDefinition();
        if (workflowDefinition != null) {
            indexObject.setWorkflowDefinitionId(workflowDefinition.getId());
            indexObject.setWorkflowDefinitionName(workflowDefinition.getName());
        }

        indexObject.setDebuggingModeActive(obj.isDebuggingModeActive());
        return indexObject;
    }

    /**
     * Queries for ProducerProfile of given id, entity can be deleted as well (deleted flag is not null).
     *
     * @param id of entity
     * @return ProducerProfile entity, even deleted. Or null if no SipProfile matches provided id.
     */
    public ProducerProfile findWithDeletedFilteringOff(String id) {
        ProducerProfile entity = query()
                .select(qObject())
                .where(qObject().id.eq(id))
                .fetchFirst();
        detachAll();
        return entity;
    }

    @Inject
    public void setGenerator(Generator generator) {
        this.generator = generator;
    }
}
