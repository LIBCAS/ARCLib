package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.profiles.QValidationProfile;
import cz.cas.lib.arclib.domain.profiles.ValidationProfile;
import cz.cas.lib.arclib.domainbase.store.NamedStore;
import cz.cas.lib.core.sequence.Generator;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;

@Repository
public class ValidationProfileStore extends NamedStore<ValidationProfile, QValidationProfile> {
    @Getter
    private final String SEQUENCE_ID = "9124111c-aca2-4f64-9e81-c7293aa24f47";

    private Generator generator;

    public ValidationProfileStore() {
        super(ValidationProfile.class, QValidationProfile.class);
    }

    @Override
    public ValidationProfile save(ValidationProfile entity) {
        if (entity.getExternalId() == null) {
            entity.setExternalId(generator.generate(SEQUENCE_ID));
        }
        return super.save(entity);
    }

    public ValidationProfile findByExternalId(@NonNull String number) {
        ValidationProfile entity = query().select(qObject()).where(qObject().externalId.eq(number)).fetchOne();
        detachAll();
        return entity;
    }

    @Transactional
    public List<ValidationProfile> findByProducerId(String producerId) {
        QValidationProfile validationProfile = qObject();

        List<ValidationProfile> fetch = query()
                .select(validationProfile)
                .where(validationProfile.producer.id.eq(producerId))
                .where(validationProfile.deleted.isNull())
                .fetch();

        detachAll();
        return fetch;
    }


    @Inject
    public void setGenerator(Generator generator) {
        this.generator = generator;
    }

}
