package cz.cas.lib.arclib.store;

import com.querydsl.jpa.impl.JPAQuery;
import cz.cas.lib.arclib.domain.packages.QAipDeletionRequest;
import cz.cas.lib.arclib.domain.packages.AipDeletionRequest;
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
     * Finds all unresolved deletion requests - requests that have not been deleted.
     *
     * @return list of non deleted deletion requests
     */
    @Transactional
    public List<AipDeletionRequest> findUnresolved() {
        QAipDeletionRequest deletionRequest = qObject();

        JPAQuery<AipDeletionRequest> query = query()
                .select(deletionRequest)
                .where(deletionRequest.deleted.isNull());

        List<AipDeletionRequest> aipDeletionRequestsFound = query.fetch();

        detachAll();
        return aipDeletionRequestsFound;
    }

    /**
     * Finds deletion request by id of AIP and id of requester
     *
     * @return deletion requester matching id of AIP and id of requester
     */
    @Transactional
    public AipDeletionRequest findByAipIdAndRequesterId(String aipId, String requesterId) {
        QAipDeletionRequest deletionRequest = qObject();

        JPAQuery<AipDeletionRequest> query = query()
                .select(deletionRequest)
                .where(deletionRequest.aipId.eq(aipId))
                .where(deletionRequest.requester.id.eq(requesterId))
                .where(deletionRequest.deleted.isNull());

        AipDeletionRequest aipDeletionRequestFound = query.fetchFirst();

        detachAll();
        return aipDeletionRequestFound;
    }
}
