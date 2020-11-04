package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.*;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflowState;
import cz.cas.lib.arclib.domain.packages.AuthorialPackage;
import cz.cas.lib.arclib.domain.packages.AuthorialPackageUpdateLock;
import cz.cas.lib.arclib.domain.packages.Sip;
import cz.cas.lib.arclib.domainbase.exception.ForbiddenObject;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.dto.AipDetailDto;
import cz.cas.lib.arclib.exception.AipStateChangeException;
import cz.cas.lib.arclib.exception.AuthorialPackageLockedException;
import cz.cas.lib.arclib.exception.AuthorialPackageNotLockedException;
import cz.cas.lib.arclib.exception.ForbiddenException;
import cz.cas.lib.arclib.index.IndexArclibXmlStore;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedAipState;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlDocument;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlStore;
import cz.cas.lib.arclib.security.authorization.data.Permissions;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageException;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageService;
import cz.cas.lib.arclib.service.arclibxml.ArclibXmlGenerator;
import cz.cas.lib.arclib.service.arclibxml.ArclibXmlValidator;
import cz.cas.lib.arclib.service.fixity.Crc32Counter;
import cz.cas.lib.arclib.service.fixity.FixityCounter;
import cz.cas.lib.arclib.service.fixity.Md5Counter;
import cz.cas.lib.arclib.service.fixity.Sha512Counter;
import cz.cas.lib.arclib.store.AuthorialPackageStore;
import cz.cas.lib.arclib.store.AuthorialPackageUpdateLockStore;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StreamUtils;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

import static cz.cas.lib.arclib.utils.ArclibUtils.hasRole;
import static cz.cas.lib.core.util.Utils.bytesToHexString;
import static cz.cas.lib.core.util.Utils.notNull;

@Slf4j
@Service
public class AipService {
    private AuthorialPackageUpdateLockStore authorialPackageUpdateLockStore;
    private AuthorialPackageStore authorialPackageStore;
    private IndexArclibXmlStore indexedArclibXmlStore;
    private IngestWorkflowService ingestWorkflowService;
    private Path workspace;

    private JobService jobService;
    private ArclibXmlGenerator arclibXmlGenerator;
    private ArchivalStorageService archivalStorageService;
    private ArclibXmlValidator arclibXmlValidator;

    private Crc32Counter crc32Counter;
    private Sha512Counter sha512Counter;
    private Md5Counter md5Counter;

    private Resource keepAliveUpdateScript;
    private int keepAliveUpdateTimeout;
    private int keepAliveNetworkDelay;
    private UserDetails userDetails;
    private TransactionTemplate transactionTemplate;

    /**
     * Gets all fields of ARCLib XML index record together with corresponding IW entity containing SIPs folder structure.
     *
     * @param xmlId od of the ARCLib XML
     * @return DTO with indexed fields and IW entity
     */
    @Transactional
    public AipDetailDto get(String xmlId, UserDetails userDetails) throws IOException {
        IngestWorkflow ingestWorkflow = ingestWorkflowService.findByExternalId(xmlId);
        notNull(ingestWorkflow, () -> new MissingObject(IngestWorkflow.class, xmlId));

        Map<String, Object> arclibXmlIndexDocument = indexedArclibXmlStore.findArclibXmlIndexDocument(xmlId);

        notNull(arclibXmlIndexDocument, () -> new MissingObject(IndexedArclibXmlDocument.class, xmlId));
        String producerId = (String) ((ArrayList) arclibXmlIndexDocument.get(IndexedArclibXmlDocument.PRODUCER_ID)).get(0);
        if (!hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE) && !userDetails.getProducerId().equals(producerId)) {
            throw new ForbiddenObject(IngestWorkflow.class, xmlId);
        }
        return new AipDetailDto(arclibXmlIndexDocument, ingestWorkflow);
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
            throws IOException {
        for (Map.Entry<String, List<Integer>> aipAndVersionId : aipIdsAndVersions.entrySet()) {
            String aipId = aipAndVersionId.getKey();
            List<Integer> versions = aipAndVersionId.getValue();
            for (Integer version : versions) {
                try {
                    InputStream response = archivalStorageService.exportSingleXml(aipId, version);
                    String fileName = ArclibUtils.getXmlExportName(aipId, version);
                    File file = new File(exportLocationPath + "/" + fileName);
                    FileUtils.copyInputStreamToFile(response, file);
                    log.info("XML of AIP with ID " + aipId + " has been exported from archival storage.");
                } catch (ArchivalStorageException e) {
                    log.error("error during export of XML of AIP with ID" + aipId, e);
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
     */
    @Transactional
    public void exportMultipleAips(List<String> aipIds, boolean all, String exportLocationPath)
            throws IOException {
        for (String aipId : aipIds) {
            try {
                InputStream response = archivalStorageService.exportSingleAip(aipId, all);
                File file = new File(exportLocationPath + "/" + ArclibUtils.getAipExportName(aipId));
                FileUtils.copyInputStreamToFile(response, file);
                log.info("AIP " + aipId + " has been exported from archival storage.");
            } catch (ArchivalStorageException e) {
                log.error("error during export of AIP: " + aipId, e);
            }
        }
    }

    /**
     * Changes state of the AIP in Archival Storage and changes state of all associated indexed XML documents in index
     *
     * @param aipId    id of the AIP
     * @param newState new state
     * @throws IOException             in the case of uncontrolled/unexpected IO error probably not related to the ArchivalStorage service
     * @throws AipStateChangeException in the case of any controlled error (e.g. some ingest workflows are unfinished or Archival Storage returns bad return code)
     */
    public void changeAipState(String aipId, IndexedAipState newState) throws AipStateChangeException, IOException {
        notNull(newState, () -> new IllegalArgumentException("state can't be null"));
        String logPrefix = "Change state request (new state: " + newState + ") of AIP: " + aipId + ": ";
        log.debug(logPrefix + "STARTED");
        List<IngestWorkflow> iws = ingestWorkflowService.findBySipId(aipId);
        List<IngestWorkflow> unfinishedIws = new ArrayList<>();
        List<IngestWorkflow> successfulIws = new ArrayList<>();
        for (IngestWorkflow iw : iws) {
            switch (iw.getProcessingState()) {
                case PERSISTED:
                    successfulIws.add(iw);
                case FAILED:
                    break;
                default:
                    unfinishedIws.add(iw);
            }
        }
        if (!unfinishedIws.isEmpty()) {
            throw new AipStateChangeException(logPrefix + "some ingest workflows not in final state: " + Arrays.toString(unfinishedIws.toArray()));
        }
        Path requestWsFolder = workspace.resolve(aipId + "_state_change_" + Instant.now().toEpochMilli());
        Files.createDirectory(requestWsFolder);
        Map<IngestWorkflow, Path> iwToTmpXmlFile = new HashMap<>();
        for (IngestWorkflow successfulIw : successfulIws) {
            InputStream aipXmlFromStorage;
            try {
                aipXmlFromStorage = archivalStorageService.exportSingleXml(aipId, successfulIw.getXmlVersionNumber());
            } catch (ArchivalStorageException e) {
                FileSystemUtils.deleteRecursively(requestWsFolder);
                throw new AipStateChangeException(logPrefix + "unable to retrieve AIP XML from Archival Storage", e);
            }
            Path xmlTmpFile = requestWsFolder.resolve(successfulIw.getExternalId() + ".xml");
            try (FileOutputStream fileOutputStream = new FileOutputStream(xmlTmpFile.toFile())) {
                IOUtils.copy(aipXmlFromStorage, fileOutputStream);
            }
            iwToTmpXmlFile.put(successfulIw, xmlTmpFile);
        }
        try {
            switch (newState) {
                case DELETED:
                    archivalStorageService.delete(aipId);
                    break;
                case REMOVED:
                    archivalStorageService.remove(aipId);
                    break;
                case ARCHIVED:
                    archivalStorageService.renew(aipId);
                    break;
            }
        } catch (ArchivalStorageException e) {
            FileSystemUtils.deleteRecursively(requestWsFolder);
            throw new AipStateChangeException(logPrefix + "unexpected Archival Storage response to state change request", e);
        }
        for (IngestWorkflow iw : iwToTmpXmlFile.keySet()) {
            final Path tmpXmlFile = iwToTmpXmlFile.get(iw);
            indexedArclibXmlStore.changeAipState(iw.getExternalId(), newState, Files.readAllBytes(tmpXmlFile));
            log.debug("State of XML of AIP " + aipId + " version " + iw.getXmlVersionNumber() + " has changed to: " + newState);
        }
        FileSystemUtils.deleteRecursively(requestWsFolder);
        log.info(logPrefix + "successfully ENDED");
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
     * <br>
     * <b>this method is not @Transactional, transactions are handled programmatically</b>
     *
     * @param aipId      id of the AIP being updated
     * @param xmlId      xml id of the latest version XML of the AIP being updated
     * @param xml        content of the XML document
     * @param reason     reason for updating specified by user
     * @param hash       hash of the XML document
     * @param xmlVersion xml version of the updated XML document
     * @return result code and message
     * @throws IllegalStateException if there is no update in progress
     */
    //not @Transactional, transactions are handled programmatically
    public void finishXmlUpdate(String aipId, String xmlId, String xml, Hash hash, Integer xmlVersion, String reason)
            throws ParserConfigurationException, SAXException, IOException, DocumentException, AuthorialPackageNotLockedException, ArchivalStorageException {
        String opLogId = "Upload of AIP XML version: " + xmlVersion + " of AIP: " + aipId + " ";
        log.info(opLogId + "started");

        IngestWorkflow originalIngestWorkflow = ingestWorkflowService.findByExternalId(xmlId);
        AuthorialPackage authorialPackage = originalIngestWorkflow.getSip().getAuthorialPackage();
        AuthorialPackageUpdateLock lock = authorialPackageUpdateLockStore.findByAuthorialPackageId(authorialPackage.getId());
        if (!lock.isLocked()) {
            throw new AuthorialPackageNotLockedException(authorialPackage.getId());
        }
        InputStream previousAipXmlStream = archivalStorageService.exportSingleXml(aipId, null);
        byte[] previousAipXml = IOUtils.toByteArray(previousAipXmlStream);
        transactionTemplate.execute(t -> {
            jobService.delete(lock.getTimeoutCheckJob());
            return null;
        });
        Producer producer = originalIngestWorkflow.getProducerProfile().getProducer();
        Integer sipVersionNumber = originalIngestWorkflow.getSip().getVersionNumber();
        Sip previousVersionSip = originalIngestWorkflow.getSip().getPreviousVersionSip();
        String sipVersionOf = previousVersionSip == null ? ArclibXmlGenerator.INITIAL_VERSION : previousVersionSip.getId();
        String authorialId = originalIngestWorkflow.getSip().getAuthorialPackage().getAuthorialId();
        IngestWorkflow newIngestWorkflow = new IngestWorkflow();
        newIngestWorkflow.setXmlVersionNumber(xmlVersion);
        newIngestWorkflow.setRelatedWorkflow(originalIngestWorkflow);
        newIngestWorkflow.setSip(originalIngestWorkflow.getSip());
        newIngestWorkflow.setFileName(originalIngestWorkflow.getFileName());
        newIngestWorkflow.setVersioningLevel(VersioningLevel.ARCLIB_XML_VERSIONING);
        newIngestWorkflow.setBatch(null);
        newIngestWorkflow.setFailureInfo(null);

        transactionTemplate.execute(t -> {
            boolean sentToArchivalStorage = false;
            try {
                ingestWorkflowService.save(newIngestWorkflow);

                log.debug(opLogId + "verifying hash");
                verifyHash(xml, hash);

                log.debug(opLogId + "validating AIP XML");
                arclibXmlValidator.validateFinalXml(xml, aipId, authorialId, sipVersionNumber, sipVersionOf);

                log.debug(opLogId + "generating metadata update event");
                String updatedXml = arclibXmlGenerator.addUpdateMetadata(xml, reason, userDetails.getUsername(), newIngestWorkflow);
                String hashValue = bytesToHexString(sha512Counter.computeDigest(new ByteArrayInputStream(updatedXml.getBytes())));
                Hash arclibXmlHash = new Hash(hashValue, HashType.Sha512);

                log.debug(opLogId + "sending to Archival Storage");
                sentToArchivalStorage = true;
                archivalStorageService.updateXml(aipId, new ByteArrayInputStream(updatedXml.getBytes()), arclibXmlHash, xmlVersion, true);
                newIngestWorkflow.setArclibXmlHash(arclibXmlHash);
                newIngestWorkflow.setLatestVersion(true);
                newIngestWorkflow.setProcessingState(IngestWorkflowState.PERSISTED);
                newIngestWorkflow.setEnded(Instant.now());
                originalIngestWorkflow.setLatestVersion(false);
                ingestWorkflowService.save(originalIngestWorkflow);
                ingestWorkflowService.save(newIngestWorkflow);
                log.debug(opLogId + "creating index record");
                indexedArclibXmlStore.createIndex(
                        updatedXml.getBytes(),
                        producer.getId(),
                        producer.getName(),
                        userDetails.getUsername(),
                        IndexedAipState.ARCHIVED,
                        false,
                        true);
                indexedArclibXmlStore.setLatestFlag(xmlId, false, previousAipXml);
                log.info(opLogId + "successfully finished");
            } catch (Exception e) {
                t.setRollbackOnly();
                log.error(opLogId + "Error occurred: " + e.toString() + ", system will roll back data, see the stacktrace bellow.");
                log.debug(opLogId + "deleting index");
                indexedArclibXmlStore.removeIndex(newIngestWorkflow.getExternalId());
                if (sentToArchivalStorage) {
                    log.debug(opLogId + "rolling back at Archival Storage");
                    try {
                        archivalStorageService.rollbackLatestXml(aipId, newIngestWorkflow.getXmlVersionNumber());
                    } catch (ArchivalStorageException rollbackException) {
                        log.error(opLogId + "rollback has failed at Archival Storage");
                    }
                }
                throw new RuntimeException(e);
            }
            return null;
        });
        transactionTemplate.execute(t -> {
            deactivateLock(newIngestWorkflow.getSip().getAuthorialPackage().getId());
            return null;
        });
    }

    /**
     * Refreshes keep alive update timeout
     *
     * @param authorialPackageId id of the authorial package being updated
     * @throws IllegalStateException no update process is in progress
     */
    @Transactional
    public void refreshKeepAliveUpdate(String authorialPackageId) throws AuthorialPackageNotLockedException {
        AuthorialPackage authorialPackage = authorialPackageStore.find(authorialPackageId);
        notNull(authorialPackage, () -> new MissingObject(AuthorialPackage.class, authorialPackageId));

        AuthorialPackageUpdateLock updateLock =
                authorialPackageUpdateLockStore.findByAuthorialPackageId(authorialPackageId);
        if (updateLock == null || !updateLock.isLocked()) {
            throw new AuthorialPackageNotLockedException(authorialPackageId);
        }
        updateLock.setLatestLockedInstant(Instant.now());
        authorialPackageUpdateLockStore.save(updateLock);
        log.debug("Refreshed keep alive of update for XML of authorial package with id " + authorialPackageId + ".");
    }

    /**
     * Cancels XML update if the update lock has expired
     *
     * @param authorialPackageId authorial package id
     */
    @Transactional
    public void testAndCancelXmlUpdate(String authorialPackageId) {
        AuthorialPackageUpdateLock authorialPackageUpdateLock =
                authorialPackageUpdateLockStore.findByAuthorialPackageId(authorialPackageId);
        Instant latestLockedInstant = authorialPackageUpdateLock.getLatestLockedInstant();

        if (latestLockedInstant != null && latestLockedInstant.plusSeconds(keepAliveUpdateTimeout)
                .isBefore(Instant.now().minusSeconds(keepAliveNetworkDelay))) {
            log.debug("Job is canceling XML update for authorial package " + authorialPackageId + ".");
            deactivateLock(authorialPackageId);
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
     * @throws IOException keep alive update script is unreadable
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

    public void removeAipXmlIndex(String externalId) {
        indexedArclibXmlStore.removeIndex(externalId);
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
    public void setIngestWorkflowService(IngestWorkflowService ingestWorkflowService) {
        this.ingestWorkflowService = ingestWorkflowService;
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
    public void setWorkspace(@Value("${arclib.path.workspace}") String workspace) {
        this.workspace = Paths.get(workspace);
    }

    @Inject
    public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }
}