package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.profiles.QValidationProfile;
import cz.cas.lib.arclib.domain.profiles.ValidationProfile;
import cz.cas.lib.arclib.domainbase.store.NamedStore;
import org.springframework.stereotype.Repository;

@Repository
public class ValidationProfileStore extends NamedStore<ValidationProfile, QValidationProfile> {

    public ValidationProfileStore() {
        super(ValidationProfile.class, QValidationProfile.class);
    }
}
