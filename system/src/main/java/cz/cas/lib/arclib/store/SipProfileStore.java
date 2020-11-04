package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.profiles.QSipProfile;
import cz.cas.lib.arclib.domain.profiles.SipProfile;
import cz.cas.lib.arclib.domainbase.store.NamedStore;
import cz.cas.lib.core.sequence.Generator;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;

@Repository
public class SipProfileStore extends NamedStore<SipProfile, QSipProfile> {

    @Getter
    private final String SEQUENCE_ID = "c6eda528-df7e-4fa8-9aff-92c138a01771";

    private Generator generator;

    public SipProfileStore() {
        super(SipProfile.class, QSipProfile.class);
    }

    @Override
    public SipProfile save(SipProfile entity) {
        if (entity.getExternalId() == null) {
            entity.setExternalId(generator.generate(SEQUENCE_ID));
        }
        return super.save(entity);
    }

    public SipProfile findByExternalId(@NonNull String number) {
        SipProfile entity = query().select(qObject()).where(qObject().externalId.eq(number)).fetchOne();
        detachAll();
        return entity;
    }

    @Inject
    public void setGenerator(Generator generator) {
        this.generator = generator;
    }
}
