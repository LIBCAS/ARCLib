package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.profiles.QValidationProfile;
import cz.cas.lib.arclib.domain.profiles.ValidationProfile;
import cz.cas.lib.core.store.NamedStore;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

@Repository
public class ValidationProfileStore extends NamedStore<ValidationProfile, QValidationProfile> {
    @Transactional
    public ValidationProfile findByName(String name) {
        QValidationProfile validationProfile = qObject();

        ValidationProfile validationProfileFound = query()
                .select(validationProfile)
                .where(validationProfile.name.eq(name))
                .fetchFirst();

        detachAll();
        return validationProfileFound;
    }

    public ValidationProfileStore() {
        super(ValidationProfile.class, QValidationProfile.class);
    }
}
