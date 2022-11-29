package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.FavoritesBucket;
import cz.cas.lib.arclib.domain.QFavoritesBucket;
import cz.cas.lib.arclib.domainbase.store.DomainStore;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class FavoritesBucketStore extends DomainStore<FavoritesBucket, QFavoritesBucket> {
    public FavoritesBucketStore() {
        super(FavoritesBucket.class, QFavoritesBucket.class);
    }

    public FavoritesBucket findFavoritesBucketOfUser(String userId) {
        QFavoritesBucket qFavoritesBucket = qObject();
        return query().select(qFavoritesBucket)
                .where(qFavoritesBucket.user.id.eq(userId))
                .fetchOne();
    }
}
