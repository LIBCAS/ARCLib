package cz.cas.lib.arclib.store;

import com.querydsl.jpa.impl.JPAQuery;
import cz.cas.lib.arclib.domain.packages.AuthorialPackage;
import cz.cas.lib.arclib.domain.packages.QAuthorialPackage;
import cz.cas.lib.arclib.domainbase.store.DatedStore;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;

@Repository
public class AuthorialPackageStore
        extends DatedStore<AuthorialPackage, QAuthorialPackage> {
    public AuthorialPackageStore() {
        super(AuthorialPackage.class, QAuthorialPackage.class);
    }


    @Transactional
    public AuthorialPackage findByAuthorialIdAndProducerId(String authorialId, String producerId) {
        QAuthorialPackage authorialPackage = qObject();

        JPAQuery<AuthorialPackage> query = query()
                .select(authorialPackage)
                .where(authorialPackage.authorialId.eq(authorialId))
                .where(authorialPackage.producerProfile.producer.id.eq(producerId))
                .where(authorialPackage.deleted.isNull());

        AuthorialPackage authorialPackageFound = query.fetchFirst();

        detachAll();
        return authorialPackageFound;
    }

    @Transactional
    public AuthorialPackage findByUuidAndProducerId(String uuid, String producerId) {
        QAuthorialPackage authorialPackage = qObject();

        JPAQuery<AuthorialPackage> query = query()
                .select(authorialPackage)
                .where(authorialPackage.id.eq(uuid))
                .where(authorialPackage.producerProfile.producer.id.eq(producerId))
                .where(authorialPackage.deleted.isNull());

        AuthorialPackage authorialPackageFound = query.fetchFirst();

        detachAll();
        return authorialPackageFound;
    }
}
