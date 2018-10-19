package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.ExportRoutine;
import cz.cas.lib.arclib.domain.QExportRoutine;
import cz.cas.lib.core.store.DatedStore;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public class ExportRoutineStore
        extends DatedStore<ExportRoutine, QExportRoutine> {
    public ExportRoutineStore() {
        super(ExportRoutine.class, QExportRoutine.class);
    }

    @Transactional
    public List<ExportRoutine> findByProducerId(String producerId) {
        QExportRoutine exportRoutine = qObject();

        List<ExportRoutine> exportRoutinesFound = query()
                .select(exportRoutine)
                .where(exportRoutine.creator.producer.id.eq(producerId))
                .where(exportRoutine.deleted.isNull())
                .fetch();

        detachAll();
        return exportRoutinesFound;
    }

    @Transactional
    public ExportRoutine findByAipQueryId(String aipQueryId) {
        QExportRoutine exportRoutine = qObject();

        ExportRoutine exportRoutineFound = query()
                .select(exportRoutine)
                .where(exportRoutine.aipQuery.id.eq(aipQueryId))
                .where(exportRoutine.deleted.isNull())
                .fetchFirst();

        detachAll();
        return exportRoutineFound;
    }
}
