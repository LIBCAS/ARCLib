package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.QSipProfile;
import cz.cas.lib.arclib.domain.SipProfile;
import cz.inqool.uas.store.DatedStore;
import org.springframework.stereotype.Repository;

@Repository
public class SipProfileStore extends DatedStore<SipProfile, QSipProfile> {
    public SipProfileStore() {
        super(SipProfile.class, QSipProfile.class);
    }
}
