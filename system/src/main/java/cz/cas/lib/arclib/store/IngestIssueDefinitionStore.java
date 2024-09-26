package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.preservationPlanning.IngestIssueDefinition;
import cz.cas.lib.arclib.domain.preservationPlanning.IngestIssueDefinitionCode;
import cz.cas.lib.arclib.domain.preservationPlanning.QIngestIssueDefinition;
import cz.cas.lib.core.sequence.Generator;
import cz.cas.lib.arclib.domainbase.store.NamedStore;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;



@Repository
public class IngestIssueDefinitionStore extends NamedStore<IngestIssueDefinition, QIngestIssueDefinition> {

    public IngestIssueDefinitionStore() {
        super(IngestIssueDefinition.class, QIngestIssueDefinition.class);
    }

    @Getter
    private final String SEQUENCE_ID = "3a68a552-5f9a-4760-8661-a1ccf622b729";

    private Generator generator;

    @Override
    public IngestIssueDefinition save(IngestIssueDefinition entity) {
        if (entity.getNumber() == null) {
            entity.setNumber(generator.generate(SEQUENCE_ID));
        }
        return super.save(entity);
    }

    public IngestIssueDefinition findByCode(IngestIssueDefinitionCode code){
        IngestIssueDefinition d = query().select(qObject()).where(qObject().code.eq(code)).fetchFirst();
        detachAll();
        return d;
    }

    @Autowired
    public void setGenerator(Generator generator) {
        this.generator = generator;
    }
}
