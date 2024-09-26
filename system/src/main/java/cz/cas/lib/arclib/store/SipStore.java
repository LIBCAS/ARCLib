package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.packages.QSip;
import cz.cas.lib.arclib.domain.packages.Sip;
import cz.cas.lib.arclib.domainbase.store.DatedStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;



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

    @Autowired
    public void setHashStore(HashStore hashStore) {
        this.hashStore = hashStore;
    }
}
