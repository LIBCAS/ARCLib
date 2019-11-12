package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.*;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflowState;
import cz.cas.lib.arclib.domain.packages.AipDeletionRequest;
import cz.cas.lib.arclib.domain.packages.AuthorialPackage;
import cz.cas.lib.arclib.domain.packages.AuthorialPackageUpdateLock;
import cz.cas.lib.arclib.domainbase.exception.ConflictException;
import cz.cas.lib.arclib.domainbase.exception.ForbiddenObject;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.dto.AipDeletionRequestDto;
import cz.cas.lib.arclib.dto.AipDetailDto;
import cz.cas.lib.arclib.exception.AuthorialPackageLockedException;
import cz.cas.lib.arclib.exception.ForbiddenException;
import cz.cas.lib.arclib.index.IndexArclibXmlStore;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedAipState;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlDocument;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlStore;
import cz.cas.lib.arclib.mail.ArclibMailCenter;
import cz.cas.lib.arclib.security.authorization.Roles;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageException;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageResponse;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageService;
import cz.cas.lib.arclib.service.arclibxml.ArclibXmlGenerator;
import cz.cas.lib.arclib.service.arclibxml.ArclibXmlValidator;
import cz.cas.lib.arclib.service.fixity.Crc32Counter;
import cz.cas.lib.arclib.service.fixity.FixityCounter;
import cz.cas.lib.arclib.service.fixity.Md5Counter;
import cz.cas.lib.arclib.service.fixity.Sha512Counter;
import cz.cas.lib.arclib.store.AipDeletionRequestStore;
import cz.cas.lib.arclib.store.AuthorialPackageStore;
import cz.cas.lib.arclib.store.AuthorialPackageUpdateLockStore;
import cz.cas.lib.arclib.store.ProducerStore;
import cz.cas.lib.arclib.utils.ArclibUtils;
import cz.cas.lib.core.scheduling.job.Job;
import cz.cas.lib.core.scheduling.job.JobService;
import cz.cas.lib.core.script.ScriptType;
import cz.cas.lib.core.store.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.dom4j.DocumentException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cz.cas.lib.arclib.utils.ArclibUtils.hasRole;
import static cz.cas.lib.core.util.Utils.bytesToHexString;
import static cz.cas.lib.core.util.Utils.notNull;

@Slf4j
@Service
public class AipService {
    private AipDeletionRequestStore aipDeletionRequestStore;
    private AuthorialPackageUpdateLockStore authorialPackageUpdateLockStore;
    private AuthorialPackageStore authorialPackageStore;
    private IndexArclibXmlStore indexedArclibXmlStore;
    private IngestWorkflowService ingestWorkflowService;
    private IndexArclibXmlStore indexArclibXmlStore;
    private ProducerStore producerStore;

    private JobService jobService;
    private ArclibXmlGenerator arclibXmlGenerator;
    private ArchivalStorageService archivalStorageService;
    private ArclibXmlValidator arclibXmlValidator;
    private ArclibMailCenter arclibMailCenter;

    private Crc32Counter crc32Counter;
    private Sha512Counter sha512Counter;
    private Md5Counter md5Counter;

    private Resource keepAliveUpdateScript;
    private int keepAliveUpdateTimeout;
    private int keepAliveNetworkDelay;
    private UserDetails userDetails;
    private BeanMappingService beanMappingService;

    /**
     * Gets all fields of ARCLib XML index record together with corresponding IW entity containing SIPs folder structure.
     * Hash of the XML content stored in index is validated to the hash stored in the IW entity.
     *
     * @param xmlId od of the ARCLib XML
     * @return DTO with indexed fields and IW entity
     * @throws GeneralException if the hash stored in the the IW entity does not match the hash of the indexed XML
     */
    @Transactional
    public AipDetailDto get(String xmlId, UserDetails userDetails) throws IOException {
        IngestWorkflow ingestWorkflow = ingestWorkflowService.findByExternalId(xmlId);
        notNull(ingestWorkflow, () -> new MissingObject(IngestWorkflow.class, xmlId));

        Map<String, Object> arclibXmlIndexDocument = indexArclibXmlStore.findArclibXmlIndexDocument(xmlId);

        notNull(arclibXmlIndexDocument, () -> new MissingObject(IndexedArclibXmlDocument.class, xmlId));
        String producerId = (String) ((ArrayList) arclibXmlIndexDocument.get(IndexedArclibXmlDocument.PRODUCER_ID)).get(0);
        if (!hasRole(userDetails, Roles.SUPER_ADMIN) && !userDetails.getProducerId().equals(producerId)) {
            throw new ForbiddenObject(IngestWorkflow.class, xmlId);
        }

        String xml = (String) ((ArrayList) arclibXmlIndexDocument.get(IndexedArclibXmlDocument.DOCUMENT)).get(0);

        Hash expectedHash = ingestWorkflow.getArclibXmlHash();
        if (expectedHash != null) {
            if (!verifyHash(xml, expectedHash))
                throw new GeneralException("Invalid checksum of indexed XML with id " + xmlId + ".");
        }

        return new AipDetailDto(arclibXmlIndexDocument, ingestWorkflow);
    }

    /**
     * Exports AIP data as a .zip
     *
     * @param aipId id of the AIP package to export
     * @param all   true to return all XMLs, otherwise only the latest is returned
     * @return response from the archival storage
     * @throws ArchivalStorageException
     */
    @Transactional
    public ArchivalStorageResponse exportSingleAip(String aipId, boolean all) throws ArchivalStorageException {
        return archivalStorageService.exportSingleAip(aipId, all);
    }

    /**
     * Exports specified XML
     *
     * @param aipId   id of the AIP package
     * @param version version number of XML, if not set the latest version is returned
     * @return response from the archival storage
     * @throws ArchivalStorageException
     */
    @Transactional
    public ArchivalStorageResponse exportSingleXml(String aipId, Integer version) throws ArchivalStorageException {
        return archivalStorageService.exportSingleXml(aipId, version);
    }

    /**
     * Exports multiple XMLs to folder
     *
     * @param aipIdsAndVersions  map of ids of AIPs as keys and numbers of versions as values
     * @param exportLocationPath location to export the AIPs to
     * @throws
     */
    @Transactional
    public void exportMultipleXmls(Map<String, List<Integer>> aipIdsAndVersions, String exportLocationPath)
            throws ArchivalStorageException, IOException {
        for (Map.Entry<String, List<Integer>> aipAndVersionId : aipIdsAndVersions.entrySet()) {
            String aipId = aipAndVersionId.getKey();
            List<Integer> versions = aipAndVersionId.getValue();
            for (Integer version : versions) {
                ArchivalStorageResponse response = archivalStorageService.exportSingleXml(aipId, version);
                if (response.getStatusCode().is2xxSuccessful()) {
                    InputStream is = response.getBody();
                    String fileName = ArclibUtils.getXmlExportName(aipId, version);
                    File file = new File(exportLocationPath + "/" + fileName);
                    FileUtils.copyInputStreamToFile(is, file);
                    log.info("XML of AIP with ID " + aipId + " has been exported from archival storage.");
                } else {
                    String exceptionMsg = response.getBody() == null ? "" : IOUtils.toString(response.getBody());
                    log.error("error during export of XML of AIP with ID" + aipId + ": " +
                            response.getStatusCode() + " " + exceptionMsg);
                }
            }

        }
    }

    /**
     * Exports multiple AIPs to folder
     *
     * @param aipIds             ids of the AIP packages to export
     * @param all                true to return all XMLs, otherwise only the latest is returned
     * @param exportLocationPath location to export the AIPs to
     * @throws ArchivalStorageException
     */
    @Transactional
    public void exportMultipleAips(List<String> aipIds, boolean all, String exportLocationPath)
            throws ArchivalStorageException, IOException {
        for (String aipId : aipIds) {
            ArchivalStorageResponse response = archivalStorageService.exportSingleAip(aipId, all);
            if (response.getStatusCode().is2xxSuccessful()) {
                InputStream is = response.getBody();
                File file = new File(exportLocationPath + "/" + ArclibUtils.getAipExportName(aipId));
                FileUtils.copyInputStreamToFile(is, file);
                log.info("AIP " + aipId + " has been exported from archival storage.");
            } else {
                String exceptionMsg = response.getBody() == null ? "" : IOUtils.toString(response.getBody());
                log.error("error during export of AIP " + aipId + ": " + response.getStatusCode() + " " + exceptionMsg);
            }
        }
    }

    /**
     * Removes AIP from archival storage and changes state of all associated indexed XML documents to REMOVED
     *
     * @param aipId id of the AIP to remove
     * @return HTTP response from the archival storage
     * @throws ArchivalStorageException

     */
    @Transactional
    public ArchivalStorageResponse removeAip(String aipId) throws ArchivalStorageException {
        ArchivalStorageResponse response = archivalStorageService.remove(aipId);
        if (response.getStatusCode().is2xxSuccessful()) {
            ingestWorkflowService.findBySipId(aipId).forEach(iw -> {
                indexedArclibXmlStore.changeAipState(iw.getExternalId(), IndexedAipState.REMOVED);
                log.info("State of XML of AIP " + aipId + " version " + iw.getXmlVersionNumber() + " has changed to REMOVED.");
            });
        }
        return response;
    }

    /**
     * Renews previously removed AIP at archival storage and changes processingState of all associated indexed XML documents to PERSISTED
     *
     * @param aipId id of the AIP to renew
     * @return HTTP response from the archival storage
     * @throws ArchivalStorageException
     */
    @Transactional
    public ArchivalStorageResponse renewAip(String aipId) throws ArchivalStorageException {
        ArchivalStorageResponse response = archivalStorageService.renew(aipId);
        if (response.getStatusCode().is2xxSuccessful()) {
            ingestWorkflowService.findBySipId(aipId).forEach(iw -> {
                indexedArclibXmlStore.changeAipState(iw.getExternalId(), IndexedAipState.ARCHIVED);
                log.info("State of XML of AIP " + aipId + " version " + iw.getXmlVersionNumber() + " has changed to PERSISTED.");
            });
        }
        return response;
    }

    /**
     * Deletes AIP at archival storage and changes state of all associated indexed XML documents to DELETED
     *
     * @param aipId id of the AIP to delete
     * @return HTTP response from the archival storage
     * @throws ArchivalStorageException
     */
    @Transactional
    private ArchivalStorageResponse deleteAip(String aipId) throws ArchivalStorageException {
        ArchivalStorageResponse response = archivalStorageService.delete(aipId);
        if (response.getStatusCode().is2xxSuccessful()) {
            ingestWorkflowService.findBySipId(aipId).forEach(iw -> {
                indexedArclibXmlStore.changeAipState(iw.getExternalId(), IndexedAipState.DELETED);
                log.info("State of XML of AIP " + aipId + " version " + iw.getXmlVersionNumber() + " has changed to DELETED.");
            });
        }
        return response;
    }

    /**
     * Registers XML update by activating lock at the respective authorial package entity
     *
     * @param authorialPackageId id of the authorial package to update
     * @throws IllegalStateException if the associated authorial package update lock is already locked because of another update process
     *                               in progress
     */
    @Transactional
    public void registerXmlUpdate(String authorialPackageId) throws IOException {
        AuthorialPackage authorialPackage = authorialPackageStore.find(authorialPackageId);
        notNull(authorialPackage, () -> new MissingObject(AuthorialPackage.class, authorialPackageId));

        activateLock(authorialPackageId, true, userDetails.getId());
        log.debug("Registered update of XML of authorial package " + authorialPackageId + ". Activated update lock.");
    }

    /**
     * Finishes AIP update by deactivating lock at the respective authorial package entity and saving a new version of ArclibXml to DB
     *
     * @param aipId      id of the AIP being updated
     * @param xmlId      xml id of the latest version XML of the AIP being updated
     * @param xml        content of the XML document
     * @param reason     reason for updating specified by user
     * @param hash       hash of the XML document
     * @param xmlVersion xml version of the updated XML document
     * @return response entity from the archival storage
     * @throws IllegalStateException if there is no update in progress
     */
    @Transactional
    public ResponseEntity<String> finishXmlUpdate(String aipId, String xmlId, String xml, Hash hash, Integer xmlVersion,
                                                  String reason)
            throws ParserConfigurationException, SAXException, IOException, DocumentException {
        IngestWorkflow originalIngestWorkflow = ingestWorkflowService.findByExternalId(xmlId);
        verifyHash(xml, hash);

        Map<String, Object> originalArclibXml = indexedArclibXmlStore.findArclibXmlIndexDocument(xmlId);
        String producerId = (String) ((ArrayList) originalArclibXml.get(IndexedArclibXmlDocument.PRODUCER_ID)).get(0);
        Integer sipVersionNumber = (Integer) ((ArrayList) originalArclibXml.get(IndexedArclibXmlDocument.SIP_VERSION_NUMBER)).get(0);
        String sipVersionOf = (String) ((ArrayList) originalArclibXml.get(IndexedArclibXmlDocument.SIP_VERSION_OF)).get(0);
        String authorialId = (String) ((ArrayList) originalArclibXml.get(IndexedArclibXmlDocument.AUTHORIAL_ID)).get(0);

        arclibXmlValidator.validateArclibXml(new ByteArrayInputStream(xml.getBytes()), aipId, authorialId, sipVersionNumber, sipVersionOf);

        IngestWorkflow newIngestWorkflow = new IngestWorkflow();
        newIngestWorkflow.setProcessingState(IngestWorkflowState.NEW);
        newIngestWorkflow.setXmlVersionNumber(xmlVersion);
        newIngestWorkflow.setRelatedWorkflow(originalIngestWorkflow);
        newIngestWorkflow.setSip(originalIngestWorkflow.getSip());
        newIngestWorkflow.setFileName(originalIngestWorkflow.getFileName());
        newIngestWorkflow.setVersioningLevel(VersioningLevel.ARCLIB_XML_VERSIONING);
        newIngestWorkflow.setBatch(null);
        newIngestWorkflow.setFailureInfo(null);
        ingestWorkflowService.save(newIngestWorkflow);
        log.debug("New index workflow with external id " + newIngestWorkflow.getExternalId()
                + " for XML version " + xmlVersion + " of AIP " + aipId + " created. The processing state is NEW.");

        xml = arclibXmlGenerator.addUpdateMetadata(xml, reason, userDetails.getUsername(), newIngestWorkflow);
        String hashValue = bytesToHexString(sha512Counter.computeDigest(new ByteArrayInputStream(xml.getBytes())));
        Hash arclibXmlHash = new Hash(hashValue, HashType.Sha512);

        ResponseEntity<String> archivalStorageResponse = archivalStorageService.updateXml(aipId, new ByteArrayInputStream(
                xml.getBytes()), arclibXmlHash, xmlVersion, true);

        if (archivalStorageResponse.getStatusCode().is2xxSuccessful()) {
            originalIngestWorkflow.setLatestVersion(false);
            ingestWorkflowService.save(originalIngestWorkflow);

            newIngestWorkflow.setArclibXmlHash(arclibXmlHash);
            newIngestWorkflow.setLatestVersion(true);
            newIngestWorkflow.setProcessingState(IngestWorkflowState.PERSISTED);
            ingestWorkflowService.save(newIngestWorkflow);
            log.debug("State of ingest workflow " + newIngestWorkflow.getExternalId() + " changed to PERSISTED.");
            Producer producer = producerStore.find(producerId);
            indexedArclibXmlStore.createIndex(xml, producer.getId(), producer.getName(), userDetails.getUser().getUsername(), IndexedAipState.ARCHIVED, originalIngestWorkflow.getBatch().isDebuggingModeActive());
            log.debug("State of XML of AIP " + aipId + " version " + xmlVersion + " in index has changed to PERSISTED.");

            deactivateLock(newIngestWorkflow.getSip().getAuthorialPackage().getId());
            log.info("Finished update of AIP " + aipId + ".");
        } else {
            log.error("Update of AIP " + aipId + " has failed to finish because failure of storing to archival storage." +
                    " Error code " + archivalStorageResponse.getStatusCode() + ", reason "
                    + archivalStorageResponse.getBody() + ".");
        }

        return archivalStorageResponse;
    }

    /**
     * Refreshes keep alive update timeout
     *
     * @param authorialPackageId id of the authorial package being updated
     * @throws IllegalStateException no update process is in progress
     */
    @Transactional
    public void refreshKeepAliveUpdate(String authorialPackageId) {
        AuthorialPackage authorialPackage = authorialPackageStore.find(authorialPackageId);
        notNull(authorialPackage, () -> new MissingObject(AuthorialPackage.class, authorialPackageId));

        AuthorialPackageUpdateLock updateLock =
                authorialPackageUpdateLockStore.findByAuthorialPackageId(authorialPackageId);
        if (updateLock == null || !updateLock.isLocked()) {
            throw new IllegalStateException("Cannot keep alive update of XML of authorial package with id " +
                    authorialPackageId + ". No update process is in progress.");
        }
        updateLock.setLatestLockedInstant(Instant.now());
        authorialPackageUpdateLockStore.save(updateLock);
        log.debug("Refreshed keep alive of update for XML of authorial package with id " + authorialPackageId + ".");
    }

    /**
     * Cancels XML update by deactivating lock at the respective authorial package
     *
     * @param authorialPackageId of the authorial package
     */
    @Transactional
    public void cancelXmlUpdate(String authorialPackageId) {
        deactivateLock(authorialPackageId);
        log.debug("Canceled XML update for authorial package " + authorialPackageId + ".");
    }

    /**
     * Cancels XML update if the update lock has expired
     *
     * @param authorialPackageId
     */
    @Transactional
    public void testAndCancelXmlUpdate(String authorialPackageId) {
        AuthorialPackageUpdateLock authorialPackageUpdateLock =
                authorialPackageUpdateLockStore.findByAuthorialPackageId(authorialPackageId);
        Instant latestLockedInstant = authorialPackageUpdateLock.getLatestLockedInstant();

        if (latestLockedInstant != null && latestLockedInstant.plusSeconds(Long.valueOf(keepAliveUpdateTimeout))
                .isBefore(Instant.now().plusSeconds(keepAliveNetworkDelay))) {
            cancelXmlUpdate(authorialPackageId);
        }
    }

    /**
     * Activates the update lock for the AIP
     *
     * @param authorialPackageId id of the AIP being updated
     * @param timeLimited        if <code>true</code>, the update lock is active only for a specified time
     *                           if not refreshed manually using method <code>refreshKeepAliveUpdate</code>
     * @param userId             id of the user that activates the lock
     */
    @Transactional
    public void activateLock(String authorialPackageId, boolean timeLimited, String userId) throws IOException {
        AuthorialPackageUpdateLock updateLock = authorialPackageUpdateLockStore
                .findByAuthorialPackageId(authorialPackageId);

        if (updateLock != null) {
            if (updateLock.isLocked())
                throw new AuthorialPackageLockedException(updateLock.getLockedByUser().getUsername(), updateLock.getLatestLockedInstant());
        } else updateLock = new AuthorialPackageUpdateLock();

        AuthorialPackage authorialPackage = authorialPackageStore.find(authorialPackageId);
        updateLock.setAuthorialPackage(authorialPackage);
        updateLock.setLockedByUser(new User(userId));

        if (timeLimited) {
            Job timeoutCheckJob = createTimeoutCheckJob(authorialPackageId);
            updateLock.setTimeoutCheckJob(timeoutCheckJob);
        } else {
            updateLock.setTimeoutCheckJob(null);
        }
        updateLock.setLocked(true);
        updateLock.setLatestLockedInstant(Instant.now());
        authorialPackageUpdateLockStore.save(updateLock);
        log.debug("Lock for authorial package " + authorialPackageId + " activated.");
    }

    /**
     * Creates timeout job.
     *
     * @param authorialPackageId id of the authorial package
     * @return created timeout check job
     * @throws ArchivalStorageException keep alive update script is unreadable
     */
    @Transactional
    private Job createTimeoutCheckJob(String authorialPackageId) throws IOException {
        Job timeoutCheckJob = new Job();
        timeoutCheckJob.setName("Authorial package keepalive check");
        timeoutCheckJob.setScriptType(ScriptType.GROOVY);
        String script = StreamUtils.copyToString(keepAliveUpdateScript.getInputStream(), Charset.defaultCharset());
        timeoutCheckJob.setScript(script);

        Map<String, String> jobParams = new HashMap<>();
        jobParams.put("keepAliveUpdateTimeout", String.valueOf(keepAliveUpdateTimeout));
        jobParams.put("authorialPackageId", authorialPackageId);
        timeoutCheckJob.setParams(jobParams);

        String cronExpression = "*/" + keepAliveUpdateTimeout + " * * * * *";
        timeoutCheckJob.setTiming(cronExpression);

        timeoutCheckJob.setActive(true);
        jobService.save(timeoutCheckJob);
        return timeoutCheckJob;
    }

    /**
     * Deactivates the update lock for the authorial package and deletes associated timeout check job
     *
     * @param authorialPackageId id of the authorial package
     */
    @Transactional
    public void deactivateLock(String authorialPackageId) throws ForbiddenException {
        AuthorialPackageUpdateLock updateLock =
                authorialPackageUpdateLockStore.findByAuthorialPackageId(authorialPackageId);
        if (updateLock != null) {
            updateLock.setLocked(false);
            authorialPackageUpdateLockStore.save(updateLock);

            Job timeoutCheckJob = updateLock.getTimeoutCheckJob();
            if (timeoutCheckJob != null) {
                jobService.delete(timeoutCheckJob);
                log.debug("Timeout check job " + timeoutCheckJob.getId() + " deleted.");
            }
            log.debug("Lock for authorial package " + authorialPackageId + " deactivated.");
        }
    }

    /**
     * Create request for deletion for AIP.
     *
     * @param aipId id of the AIP to delete
     */
    @Transactional
    public void createDeletionRequest(String aipId) {
        User requester = new User(userDetails.getId());
        AipDeletionRequest byAipAndRequester = aipDeletionRequestStore.findByAipIdAndRequesterId(aipId, userDetails.getId());
        if (byAipAndRequester != null) {
            throw new ConflictException(AipDeletionRequest.class, byAipAndRequester.getId());
        }
        AipDeletionRequest deletionRequest = new AipDeletionRequest();
        deletionRequest.setAipId(aipId);
        deletionRequest.setRequester(requester);
        aipDeletionRequestStore.save(deletionRequest);
        log.info("Created request for AIP deletion " + deletionRequest.getId() + " for requester " +
                requester.getId() + " and AIP " + aipId + ".");
    }

    /**
     * Lists unresolved deletion requests (that have not yet been resolved and deleted)
     * <p>
     * If the logged on user has not Roles.SUPER_ADMIN  returns only requests of which the requester
     * has the same producer as the logged on user.
     *
     * @return list of deletion requests
     */
    @Transactional
    public List<AipDeletionRequestDto> listDeletionRequests() {
        List<AipDeletionRequest> unresolvedDeletionRequests = aipDeletionRequestStore.findUnresolved();

        if (!hasRole(userDetails, Roles.SUPER_ADMIN)) {
            unresolvedDeletionRequests = unresolvedDeletionRequests.stream()
                    .filter(request -> {
                        User requester = request.getRequester();
                        return requester.getProducer().getId().equals(userDetails.getProducerId());
                    })
                    .collect(Collectors.toList());
        }

        return unresolvedDeletionRequests.stream()
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
                ArchivalStorageResponse response = deleteAip(deletionRequest.getAipId());
                int statusCode = response.getStatusCode().value();
                String body = IOUtils.toString(response.getBody(), StandardCharsets.UTF_8);

                result = "HTTP status code on request for deletion of AIP " + deletionRequest.getAipId()
                        + " at archival storage: " + statusCode + ", result: " + body;
                log.info(result);
            } catch (ArchivalStorageException | IOException e) {
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
        aipDeletionRequestStore.save(deletionRequest);
        log.debug("Deletion request " + deletionRequestId + " has been set to DELETED.");

        String message = "User " + userDetails.getId() + " has disacknowledged deletion of AIP " + deletionRequest.getAipId();
        log.info(message);
        arclibMailCenter.sendAipDeletionDisacknowledgedNotification(requester.getEmail(), deletionRequest.getAipId(), message, Instant.now());
    }

    /**
     * Verifies the hash of the ArclibXMLto the computed value and throws and exception if the hash does not match
     *
     * @param xml          ArclibXml
     * @param expectedHash hash of the ArclibXml
     * @return true if the verification succeeded, false otherwise
     */
    private boolean verifyHash(String xml, Hash expectedHash) throws IOException {
        FixityCounter fixityCounter;
        switch (expectedHash.getHashType()) {
            case MD5:
                fixityCounter = md5Counter;
                break;
            case Crc32:
                fixityCounter = crc32Counter;
                break;
            case Sha512:
                fixityCounter = sha512Counter;
                break;
            default:
                throw new GeneralException("unexpected type of expectedHash");
        }
        return fixityCounter.verifyFixity(new ByteArrayInputStream(xml.getBytes()), expectedHash.getHashValue());
    }

    @Inject
    public void setAuthorialPackageUpdateLockStore(AuthorialPackageUpdateLockStore authorialPackageUpdateLockStore) {
        this.authorialPackageUpdateLockStore = authorialPackageUpdateLockStore;
    }

    @Inject
    public void setJobService(JobService jobService) {
        this.jobService = jobService;
    }

    @Inject
    public void setArclibXmlGenerator(ArclibXmlGenerator arclibXmlGenerator) {
        this.arclibXmlGenerator = arclibXmlGenerator;
    }

    @Inject
    public void setArchivalStorageService(ArchivalStorageService archivalStorageService) {
        this.archivalStorageService = archivalStorageService;
    }

    @Inject
    public void setArclibXmlValidator(ArclibXmlValidator arclibXmlValidator) {
        this.arclibXmlValidator = arclibXmlValidator;
    }

    @Inject
    public void setCrc32Counter(Crc32Counter crc32Counter) {
        this.crc32Counter = crc32Counter;
    }

    @Inject
    public void setSha512Counter(Sha512Counter sha512Counter) {
        this.sha512Counter = sha512Counter;
    }

    @Inject
    public void setMd5Counter(Md5Counter md5Counter) {
        this.md5Counter = md5Counter;
    }

    @Inject
    public void setindexedArclibXmlStore(IndexedArclibXmlStore indexedArclibXmlStore) {
        this.indexedArclibXmlStore = indexedArclibXmlStore;
    }

    @Inject
    public void setIndexArclibXmlStore(IndexArclibXmlStore indexArclibXmlStore) {
        this.indexArclibXmlStore = indexArclibXmlStore;
    }

    @Inject
    public void setIngestWorkflowService(IngestWorkflowService ingestWorkflowService) {
        this.ingestWorkflowService = ingestWorkflowService;
    }

    @Inject
    public void setAipDeletionRequestStore(AipDeletionRequestStore aipDeletionRequestStore) {
        this.aipDeletionRequestStore = aipDeletionRequestStore;
    }

    @Inject
    public void setAuthorialPackageStore(AuthorialPackageStore authorialPackageStore) {
        this.authorialPackageStore = authorialPackageStore;
    }

    @Inject
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }

    @Inject
    public void setArclibMailCenter(ArclibMailCenter arclibMailCenter) {
        this.arclibMailCenter = arclibMailCenter;
    }

    @Inject
    public void setKeepAliveUpdateScript(@Value("${arclib.script.keepAliveUpdate}") Resource keepAliveUpdateScript) {
        this.keepAliveUpdateScript = keepAliveUpdateScript;
    }

    @Inject
    public void setKeepAliveUpdateTimeout(@Value("${arclib.keepAliveUpdateTimeout}") int keepAliveUpdateTimeout) {
        this.keepAliveUpdateTimeout = keepAliveUpdateTimeout;
    }

    @Inject
    public void setKeepAliveNetworkDelay(@Value("${arclib.keepAliveNetworkDelay}") int keepAliveNetworkDelay) {
        this.keepAliveNetworkDelay = keepAliveNetworkDelay;
    }

    @Inject
    public void setProducerStore(ProducerStore producerStore) {
        this.producerStore = producerStore;
    }

    @Inject
    public void setBeanMappingService(BeanMappingService beanMappingService) {
        this.beanMappingService = beanMappingService;
    }
}
