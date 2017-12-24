package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.QSip;
import cz.cas.lib.arclib.domain.Sip;
import cz.inqool.uas.store.DomainStore;
import org.springframework.stereotype.Repository;

@Repository
public class SipStore extends DomainStore<Sip, QSip> {
    public SipStore() {
        super(Sip.class, QSip.class);
    }
}
