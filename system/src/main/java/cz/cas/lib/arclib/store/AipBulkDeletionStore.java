package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.packages.AipBulkDeletion;
import cz.cas.lib.arclib.domain.packages.AipBulkDeletionState;
import cz.cas.lib.arclib.domain.packages.QAipBulkDeletion;
import cz.cas.lib.arclib.domainbase.store.DatedStore;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AipBulkDeletionStore extends DatedStore<AipBulkDeletion, QAipBulkDeletion> {

    public AipBulkDeletionStore() {
        super(AipBulkDeletion.class, QAipBulkDeletion.class);
    }

    public long setAllRunningToFail() {
        QAipBulkDeletion q = qObject();
        long updated = queryFactory.update(q).set(q.state, AipBulkDeletionState.FAILED).where(q.state.eq(AipBulkDeletionState.RUNNING)).execute();
        detachAll();
        return updated;
    }

    @Transactional
    public List<AipBulkDeletion> findByProducerId(String producerId) {
        QAipBulkDeletion q = qObject();

        List<AipBulkDeletion> fetch = query()
                .select(q)
                .where(q.producer.id.eq(producerId))
                .where(q.deleted.isNull())
                .fetch();

        detachAll();
        return fetch;
    }

    public boolean isAnyRunning() {
        QAipBulkDeletion q = qObject();
        return query()
                .select(q)
                .where(q.state.eq(AipBulkDeletionState.RUNNING))
                .fetchCount() > 0;
    }
}
