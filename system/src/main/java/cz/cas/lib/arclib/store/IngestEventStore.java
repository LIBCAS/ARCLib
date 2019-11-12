package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.ingestWorkflow.IngestEvent;
import cz.cas.lib.arclib.domain.ingestWorkflow.QIngestEvent;
import cz.cas.lib.arclib.domainbase.store.DatedStore;
import cz.cas.lib.core.store.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class IngestEventStore extends DatedStore<IngestEvent, QIngestEvent> {
    public IngestEventStore() {
        super(IngestEvent.class, QIngestEvent.class);
    }

    @Override
    @Transactional
    public IngestEvent save(IngestEvent event) {
        return super.save(event);
    }

    public List<IngestEvent> findAllOfIngestWorkflow(String iwExternalId) {
        QIngestEvent qIngestEvent = qObject();

        List<IngestEvent> fetch = query().select(qIngestEvent).where(qIngestEvent.ingestWorkflow.externalId.eq(iwExternalId)).orderBy(qIngestEvent.created.asc()).fetch();
        detachAll();
        return fetch;
    }
}
