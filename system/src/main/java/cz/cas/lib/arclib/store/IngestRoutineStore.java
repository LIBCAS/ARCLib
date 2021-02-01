package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.Batch;
import cz.cas.lib.arclib.domain.IngestRoutine;
import cz.cas.lib.arclib.domain.QBatch;
import cz.cas.lib.arclib.domain.QIngestRoutine;
import cz.cas.lib.arclib.domainbase.store.NamedStore;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class IngestRoutineStore extends NamedStore<IngestRoutine, QIngestRoutine> {

    public IngestRoutineStore() { super(IngestRoutine.class, QIngestRoutine.class); }

    public List<IngestRoutine> findByProducerId(String producerId) {
        QIngestRoutine ingestRoutine = qObject();

        List<IngestRoutine> ingestRoutinesFound = query()
                .select(ingestRoutine)
                .where(ingestRoutine.producerProfile.producer.id.eq(producerId))
                .where(ingestRoutine.deleted.isNull())
                .fetch();

        detachAll();
        return ingestRoutinesFound;
    }

    /**
     * Initializes LAZY field {@link IngestRoutine#currentlyProcessingBatches}
     *
     * @return routine with initialized LAZY field and respecting 'deleted' flag.
     */
    public IngestRoutine findWithBatchesFilled(String id) {
        QBatch qBatch = QBatch.batch;
        IngestRoutine ingestRoutinesFound = query()
                .select(qObject())
                .where(qObject().id.eq(id))
                .where(qObject().deleted.isNull())
                .leftJoin(qObject().currentlyProcessingBatches, qBatch)
                .fetchJoin()
                .fetchOne();
        detachAll();
        return ingestRoutinesFound;
    }

}
