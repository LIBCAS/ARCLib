package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.IngestRoutine;
import cz.cas.lib.arclib.domain.QIngestRoutine;
import cz.cas.lib.core.store.NamedStore;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public class IngestRoutineStore
        extends NamedStore<IngestRoutine, QIngestRoutine> {
    public IngestRoutineStore() {
        super(IngestRoutine.class, QIngestRoutine.class);
    }

    @Transactional
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

    @Transactional
    public IngestRoutine findByName(String name) {
        QIngestRoutine ingestRoutine = qObject();

        IngestRoutine ingestRoutineFound = query()
                .select(ingestRoutine)
                .where(ingestRoutine.name.eq(name))
                .fetchFirst();

        detachAll();
        return ingestRoutineFound;
    }
}