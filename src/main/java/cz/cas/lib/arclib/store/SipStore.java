package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.packages.QSip;
import cz.cas.lib.arclib.domain.packages.Sip;
import cz.cas.lib.core.store.DatedStore;
import org.springframework.stereotype.Repository;

@Repository
public class SipStore
        extends DatedStore<Sip, QSip> {
    public SipStore() {
        super(Sip.class, QSip.class);
    }
}
