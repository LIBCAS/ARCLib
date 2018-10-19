package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.AipQuery;
import cz.cas.lib.arclib.domain.QAipQuery;
import cz.cas.lib.core.store.NamedStore;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AipQueryStore extends NamedStore<AipQuery, QAipQuery> {
    public AipQueryStore() {
        super(AipQuery.class, QAipQuery.class);
    }

    public List<AipQuery> findQueriesOfUser(String userId) {
        QAipQuery qAipQuery = qObject();
        return query().select(qAipQuery)
                .where(qAipQuery.user.id.eq(userId))
                .where(qAipQuery.deleted.isNull())
                .fetch();
    }
}
