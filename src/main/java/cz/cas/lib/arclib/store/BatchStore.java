package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.Batch;
import cz.cas.lib.arclib.domain.QBatch;
import cz.inqool.uas.store.DatedStore;
import org.springframework.stereotype.Repository;

@Repository
public class BatchStore extends DatedStore<Batch, QBatch> {
    public BatchStore() {
        super(Batch.class, QBatch.class);
    }
}
