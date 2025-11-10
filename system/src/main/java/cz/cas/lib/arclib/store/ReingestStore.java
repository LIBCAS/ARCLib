package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.reingest.QReingest;
import cz.cas.lib.arclib.domain.reingest.Reingest;
import cz.cas.lib.arclib.domainbase.store.DatedStore;
import org.springframework.stereotype.Repository;

@Repository
public class ReingestStore extends DatedStore<Reingest, QReingest> {

    public ReingestStore() {
        super(Reingest.class, QReingest.class);
    }

    public Reingest getCurrent() {
        QReingest q = qObject();
        Reingest reingest = query().select(q).where(q.deleted.isNull()).fetchOne();
        detachAll();
        return reingest;
    }
}
