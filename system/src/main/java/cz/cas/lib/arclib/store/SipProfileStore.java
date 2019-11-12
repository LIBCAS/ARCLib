package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.profiles.QSipProfile;
import cz.cas.lib.arclib.domain.profiles.SipProfile;
import cz.cas.lib.arclib.domainbase.store.NamedStore;
import org.springframework.stereotype.Repository;

@Repository
public class SipProfileStore extends NamedStore<SipProfile, QSipProfile> {

    public SipProfileStore() {
        super(SipProfile.class, QSipProfile.class);
    }
}
