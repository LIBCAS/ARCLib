package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.domain.ingestWorkflow.WorkflowDefinition;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.domain.profiles.QProducerProfile;
import cz.cas.lib.arclib.domain.profiles.SipProfile;
import cz.cas.lib.arclib.domain.profiles.ValidationProfile;
import cz.cas.lib.arclib.index.solr.entity.SolrProducerProfile;
import cz.cas.lib.core.index.dto.Filter;
import cz.cas.lib.core.index.dto.FilterOperation;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import cz.cas.lib.core.index.solr.SolrNamedStore;
import cz.cas.lib.core.sequence.Generator;
import lombok.Getter;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;

import static cz.cas.lib.core.util.Utils.asList;

@Repository
public class ProducerProfileStore extends SolrNamedStore<ProducerProfile, QProducerProfile, SolrProducerProfile> {

    @Getter
    private final String SEQUENCE_ID = "7c5a958a-7b8b-41f7-a8be-49aa07a13261";

    private Generator generator;

    public ProducerProfileStore() {
        super(ProducerProfile.class, QProducerProfile.class, SolrProducerProfile.class);
    }

    @Override
    public ProducerProfile save(ProducerProfile entity) {
        if (entity.getExternalId() == null) {
            entity.setExternalId(generator.generate(SEQUENCE_ID));
        }
        return super.save(entity);
    }

    @Transactional
    public ProducerProfile findByExternalId(String number) {
        Params params = new Params();
        params.setPageSize(null);

        Filter filter = new Filter();
        filter.setField("externalId");
        filter.setOperation(FilterOperation.EQ);
        filter.setValue(number);
        params.setFilter(asList(filter));

        Result<ProducerProfile> all = findAll(params);

        List<ProducerProfile> items = all.getItems();
        ProducerProfile producerProfile = null;

        if (items.size() > 0) {
            producerProfile = items.get(0);
        }
        return producerProfile;
    }

    @Override
    public SolrProducerProfile toIndexObject(ProducerProfile obj) {
        SolrProducerProfile indexObject = super.toIndexObject(obj);

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

    @Inject
    public void setGenerator(Generator generator) {
        this.generator = generator;
    }
}
