package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.reingest.QReingestItem;
import cz.cas.lib.arclib.domain.reingest.Reingest;
import cz.cas.lib.arclib.domain.reingest.ReingestItem;
import cz.cas.lib.arclib.domainbase.store.DomainStore;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ReingestItemStore extends DomainStore<ReingestItem, QReingestItem> {

    public ReingestItemStore() {
        super(ReingestItem.class, QReingestItem.class);
    }

    public List<ReingestItem> findNext(Reingest reingest, int offset, int limit) {
        QReingestItem q = qObject();
        List<ReingestItem> fetch = query().select(q)
                .where(q.reingest.eq(reingest))
                .orderBy(q.ingestWorkflow.created.asc())
                .offset(offset)
                .limit(limit)
                .fetch();
        detachAll();
        return fetch;
    }

    public ReingestItem findNext(Reingest reingest, int offset) {
        List<ReingestItem> next = findNext(reingest, offset, 1);
        if (next.isEmpty()) {
            return null;
        }
        return next.get(0);
    }
}
