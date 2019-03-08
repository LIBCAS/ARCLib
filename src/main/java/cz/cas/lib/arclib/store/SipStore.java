package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.packages.QSip;
import cz.cas.lib.arclib.domain.packages.Sip;
import cz.cas.lib.core.store.DatedStore;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;

@Repository
public class SipStore
        extends DatedStore<Sip, QSip> {

    private HashStore hashStore;

    public SipStore() {
        super(Sip.class, QSip.class);
    }

    @Override
    public void hardDelete(Sip entity) {
        entity.getHashes().forEach(h ->
                hashStore.delete(h));
        super.hardDelete(entity);
    }

    @Inject
    public void setHashStore(HashStore hashStore) {
        this.hashStore = hashStore;
    }
}
