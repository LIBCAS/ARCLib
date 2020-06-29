package cz.cas.lib.arclib.store;

import com.querydsl.jpa.impl.JPAQuery;
import cz.cas.lib.arclib.domain.packages.AipDeletionRequest;
import cz.cas.lib.arclib.domain.packages.QAipDeletionRequest;
import cz.cas.lib.arclib.domainbase.store.DatedStore;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public class AipDeletionRequestStore
        extends DatedStore<AipDeletionRequest, QAipDeletionRequest> {
    public AipDeletionRequestStore() {
        super(AipDeletionRequest.class, QAipDeletionRequest.class);
    }

    /**
     * Finds all deletion requests - even the ones marked as deleted
     *
     * @return list of non deleted deletion requests
     */
    @Transactional
    public List<AipDeletionRequest> findAllIncludingDeleted() {
        QAipDeletionRequest deletionRequest = qObject();

        JPAQuery<AipDeletionRequest> query = query()
                .select(deletionRequest);
        List<AipDeletionRequest> aipDeletionRequestsFound = query.fetch();

        detachAll();
        return aipDeletionRequestsFound;
    }

    /**
     * Finds deletion request by id of AIP
     *
     * @return deletion requester matching id of AIP
     */
    @Transactional
    public AipDeletionRequest findByAipId(String aipId) {
        QAipDeletionRequest deletionRequest = qObject();

        JPAQuery<AipDeletionRequest> query = query()
                .select(deletionRequest)
                .where(deletionRequest.aipId.eq(aipId))
                .where(deletionRequest.deleted.isNull());

        AipDeletionRequest aipDeletionRequestFound = query.fetchFirst();

        detachAll();
        return aipDeletionRequestFound;
    }
}
