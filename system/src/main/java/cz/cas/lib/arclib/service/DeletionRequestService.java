package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.domain.packages.AipDeletionRequest;
import cz.cas.lib.arclib.domainbase.exception.ConflictException;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.dto.AipDeletionRequestDto;
import cz.cas.lib.arclib.exception.AipStateChangeException;
import cz.cas.lib.arclib.exception.ForbiddenException;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedAipState;
import cz.cas.lib.arclib.mail.ArclibMailCenter;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.store.AipDeletionRequestStore;
import cz.cas.lib.core.store.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static cz.cas.lib.arclib.utils.ArclibUtils.hasRole;
import static cz.cas.lib.core.util.Utils.notNull;

@Slf4j
@Service
public class DeletionRequestService {

    private UserDetails userDetails;
    private AipDeletionRequestStore aipDeletionRequestStore;
    private ArclibMailCenter arclibMailCenter;
    private BeanMappingService beanMappingService;
    private AipService aipService;

    /**
     * Create request for deletion for AIP.
     *
     * @param aipId id of the AIP to delete
     */
    @Transactional
    public void createDeletionRequest(String aipId) {
        User requester = new User(userDetails.getId());
        AipDeletionRequest existingRequestForThisAip = aipDeletionRequestStore.findByAipId(aipId);
        if (existingRequestForThisAip != null) {
            throw new ConflictException(AipDeletionRequest.class, existingRequestForThisAip.getId());
        }
        AipDeletionRequest deletionRequest = new AipDeletionRequest();
        deletionRequest.setAipId(aipId);
        deletionRequest.setRequester(requester);
        aipDeletionRequestStore.save(deletionRequest);
        log.info("Created request for AIP deletion " + deletionRequest.getId() + " for requester " +
                requester.getId() + " and AIP " + aipId + ".");
    }

    /**
     * Reverts request for deletion for AIP, i.e. deletes the record about request from DB.
     *
     * @param deletionRequestId request id
     * @throws ConflictException if the request does not exist or it was already processed (acknowledged by two other users)
     */
    @Transactional
    public void revertDeletionRequest(String deletionRequestId) {
        String requesterId = userDetails.getId();
        AipDeletionRequest request = aipDeletionRequestStore.find(deletionRequestId);
        if (request == null) {
            throw new ConflictException(AipDeletionRequest.class, deletionRequestId);
        }
        if (!requesterId.equals(request.getRequester().getId())) {
            throw new ForbiddenException("Can't revert deletion request of other user");
        }
        request.setRejectedBy(userDetails.getUser());
        request.setDeleted(Instant.now());
        aipDeletionRequestStore.save(request);
        log.info("Reverted request: " + request.getId() + " for AIP deletion of requester: " +
                requesterId + " and AIP " + request.getAipId() + ".");
    }

    /**
     * Lists unresolved deletion requests (that have not yet been resolved) and also historical (resolved) request
     * if the caller is the requester.
     * <p>
     * If the logged on user has not Roles.SUPER_ADMIN  returns only requests of which the requester
     * has the same producer as the logged on user.
     *
     * @return list of deletion requests
     */
    @Transactional
    public List<AipDeletionRequestDto> listDeletionRequests() {
        //uncomment this if we want to show history of own request, FE would need changes too (revert button would
        // must be hidden if rejectedBy is set and the table text should change from "rejected" to "reverted" if rejectedBy = the user himself)
//        Collection<AipDeletionRequest> deletionRequests = aipDeletionRequestStore.findAllIncludingDeleted();
//        deletionRequests = deletionRequests.stream().filter(r -> r.getDeleted() == null ||
//                r.getRequester().getId().equals(userDetails.getId())).collect(Collectors.toList());
        Collection<AipDeletionRequest> deletionRequests = aipDeletionRequestStore.findAll();
        if (!hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE)) {
            deletionRequests = deletionRequests.stream()
                    .filter(request -> {
                        User requester = request.getRequester();
                        return requester.getProducer().getId().equals(userDetails.getProducerId());
                    })
                    .collect(Collectors.toList());
        }

        return deletionRequests.stream()
                .map(o -> beanMappingService.mapTo(o, AipDeletionRequestDto.class))
                .collect(Collectors.toList());
    }

    /**
     * Acknowledges the request for deletion of AIP
     *
     * @param deletionRequestId id of the deletion request to acknowledge
     */
    @Transactional
    public void acknowledgeDeletion(String deletionRequestId) {
        AipDeletionRequest deletionRequest = aipDeletionRequestStore.find(deletionRequestId);
        notNull(deletionRequest, () -> new MissingObject(AipDeletionRequest.class, deletionRequestId));

        User requester = deletionRequest.getRequester();
        User confirmer1 = deletionRequest.getConfirmer1();
        User confirmer2 = deletionRequest.getConfirmer2();

        if (userDetails.getId().equals(requester.getId())) {
            throw new IllegalArgumentException("Cannot acknowledge own AIP deletion request. Deletion request "
                    + deletionRequestId + ", user " + userDetails.getId() + ".");
        }
        if (confirmer1 != null && userDetails.getId().equals(confirmer1.getId())) {
            throw new IllegalArgumentException("Cannot acknowledge the same AIP deletion request more than once. Deletion request "
                    + deletionRequestId + ", user " + userDetails.getId() + ".");
        }
        log.info("User " + userDetails.getId() + " has acknowledged deletion of AIP " + deletionRequest.getAipId() + ".");

        if (confirmer1 == null) {
            deletionRequest.setConfirmer1(new User(userDetails.getId()));
            aipDeletionRequestStore.save(deletionRequest);
            log.debug("User " + userDetails.getId() + " has been set as the first confirmer of deletion request "
                    + deletionRequest.getId() + ".");

        } else if (confirmer2 == null) {
            deletionRequest.setConfirmer2(new User(userDetails.getId()));
            deletionRequest.setDeleted(Instant.now());
            aipDeletionRequestStore.save(deletionRequest);
            log.debug("User " + userDetails.getId() + " has been set as the second confirmer of deletion request "
                    + deletionRequest.getId());
            log.debug("Deletion request " + deletionRequestId + " has been set to DELETED.");

            log.info("Triggered deletion of AIP " + deletionRequest.getAipId() + ".");
            String result = null;
            try {
                aipService.changeAipState(deletionRequest.getAipId(), IndexedAipState.DELETED);
                result = "Deletion success.";
                log.info(result);
            } catch (IOException | AipStateChangeException e) {
                result = "Deletion of AIP " + deletionRequest.getAipId() + " at archival storage failed. Reason: " + e.toString();
                log.error(result);
            } finally {
                arclibMailCenter.sendAipDeletionAcknowledgedNotification(requester.getEmail(), deletionRequest.getAipId(), result, Instant.now());
            }
        }
    }

    /**
     * Disacknowledges the request for deletion of AIP
     *
     * @param deletionRequestId id of the deletion request to disacknowledge
     */
    @Transactional
    public void disacknowledgeDeletion(String deletionRequestId) {
        AipDeletionRequest deletionRequest = aipDeletionRequestStore.find(deletionRequestId);
        notNull(deletionRequest, () -> new MissingObject(AipDeletionRequest.class, deletionRequestId));

        User requester = deletionRequest.getRequester();
        if (userDetails.getId().equals(requester.getId())) {
            throw new IllegalArgumentException("Cannot disacknowledge own AIP deletion request. Deletion request "
                    + deletionRequestId + ", user " + userDetails.getId() + ".");
        }

        deletionRequest.setDeleted(Instant.now());
        deletionRequest.setRejectedBy(userDetails.getUser());
        aipDeletionRequestStore.save(deletionRequest);
        log.debug("Deletion request " + deletionRequestId + " has been set to DELETED.");

        String message = "User " + userDetails.getId() + " has disacknowledged deletion of AIP " + deletionRequest.getAipId();
        log.info(message);
        arclibMailCenter.sendAipDeletionDisacknowledgedNotification(requester.getEmail(), deletionRequest.getAipId(), message, Instant.now());
    }

    @Inject
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }

    @Inject
    public void setAipDeletionRequestStore(AipDeletionRequestStore aipDeletionRequestStore) {
        this.aipDeletionRequestStore = aipDeletionRequestStore;
    }

    @Inject
    public void setArclibMailCenter(ArclibMailCenter arclibMailCenter) {
        this.arclibMailCenter = arclibMailCenter;
    }

    @Inject
    public void setBeanMappingService(BeanMappingService beanMappingService) {
        this.beanMappingService = beanMappingService;
    }

    @Inject
    public void setAipService(AipService aipService) {
        this.aipService = aipService;
    }
}
