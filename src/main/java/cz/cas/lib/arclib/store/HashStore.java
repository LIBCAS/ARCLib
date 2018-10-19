package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.Hash;
import cz.cas.lib.arclib.domain.QHash;
import cz.cas.lib.core.store.DatedStore;
import org.springframework.stereotype.Repository;

@Repository
public class HashStore
        extends DatedStore<Hash, QHash> {
    public HashStore() {
        super(Hash.class, QHash.class);
    }
}
