package cz.cas.lib.arclib.index.solr;

import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflowState;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.index.CreateIndexRecordDto;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedAipState;
import cz.cas.lib.arclib.index.solr.arclibxml.SolrArclibXmlStore;
import cz.cas.lib.arclib.report.ReportStore;
import cz.cas.lib.arclib.service.IngestWorkflowService;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageException;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageService;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageServiceDebug;
import cz.cas.lib.arclib.service.archivalStorage.ObjectState;
import cz.cas.lib.arclib.store.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static cz.cas.lib.core.util.Utils.notNull;

@Service
@Slf4j
public class ReindexService {

    private BatchStore batchStore;
    private ReportStore reportStore;
    private ProducerProfileStore producerProfileStore;
    private UserStore userStore;
    private IndexedFormatStore formatStore;
    private IngestIssueStore ingestIssueStore;
    private IngestWorkflowService ingestWorkflowService;
    private SolrArclibXmlStore indexedArclibXmlStore;
    private ArchivalStorageService archivalStorageService;
    private ArchivalStorageServiceDebug archivalStorageServiceDebug;

    /**
     * formats are omitted as there are too many records
     */
    @Async
    public void dropReindexAll() {
        batchStore.dropReindex();
        producerProfileStore.dropReindex();
        userStore.dropReindex();
        ingestIssueStore.dropReindex();
        reportStore.dropReindex();
    }

    @Async
    public void dropReindexFormat() {
        formatStore.dropReindex();
    }

    public void dropReindexManagedSync() {
        dropReindexAll();
        dropReindexFormat();
    }

    @Async
    public void reindexArclibXml() {
        log.info("recovering ARCLib XML index from DB, retrieving XMLs from Archival Storage");
        AtomicLong successCount = new AtomicLong(0);
        List<String> failures = new ArrayList<>();
        ingestWorkflowService.findAll().stream()
                .filter(iw -> iw.getProcessingState() == IngestWorkflowState.PERSISTED)
                .forEach(iw -> {
                    try {
                        Producer p = iw.getProducerProfile().getProducer();
                        String xml;
                        String username;
                        Object userInCamunda = ingestWorkflowService.getVariable(iw.getExternalId(), BpmConstants.ProcessVariables.responsiblePerson);
                        notNull(userInCamunda, () -> new IllegalStateException("no responsible person found for IW: " + iw.getExternalId() + " in camunda db"));
                        User user = userStore.findEvenDeleted((String) userInCamunda);
                        notNull(user, () -> new IllegalStateException("no user responsible for IW: " + iw.getExternalId() + " found in ARCLib db"));
                        username = user.getUsername();
                        IndexedAipState stateAtArchivalStorage;
                        if (iw.wasIngestedInDebugMode()) {
                            xml = IOUtils.toString(archivalStorageServiceDebug.exportSingleXml(iw.getSip().getId(), iw.getXmlVersionNumber()), Charset.defaultCharset());
                            stateAtArchivalStorage = IndexedAipState.ARCHIVED;
                        } else {
                            InputStream arcstorageResponse;
                            try {
                                arcstorageResponse = archivalStorageService.exportSingleXml(iw.getSip().getId(), iw.getXmlVersionNumber());
                            } catch (ArchivalStorageException e) {
                                log.error("could not retrieve XML " + iw.getExternalId() + " of AIP " + iw.getSip().getId());
                                throw e;
                            }
                            xml = IOUtils.toString(arcstorageResponse, Charset.defaultCharset());
                            ObjectState aipState = archivalStorageService.getAipState(iw.getSip().getId());
                            stateAtArchivalStorage = objectStateToIndexedAipState(aipState);
                        }
                        indexedArclibXmlStore.createIndex(new CreateIndexRecordDto(xml.getBytes(), p.getId(), p.getName(), username, stateAtArchivalStorage, iw.wasIngestedInDebugMode(), iw.isLatestVersion(), iw.getSip().isLatestVersion()));
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        log.error("error while reindexing " + iw.getId(), e);
                        failures.add(iw.getId());
                    }
                });
        log.info("recovered ARCLib XML index, {} succeeded, {} failures: {}", successCount, failures.size(), failures);
    }

    private IndexedAipState objectStateToIndexedAipState(ObjectState aipState) {
        IndexedAipState stateAtArchivalStorage;
        switch (aipState) {
            case PROCESSING:
            case PRE_PROCESSING:
            case ROLLED_BACK:
            case ARCHIVAL_FAILURE:
            case ROLLBACK_FAILURE: {
                stateAtArchivalStorage = null;
                break;
            }
            case ARCHIVED: {
                stateAtArchivalStorage = IndexedAipState.ARCHIVED;
                break;
            }
            case REMOVED: {
                stateAtArchivalStorage = IndexedAipState.REMOVED;
                break;
            }
            case DELETION_FAILURE:
            case DELETED: {
                stateAtArchivalStorage = IndexedAipState.DELETED;
                break;
            }
            default: {
                log.error("AIP saved in the archival storage is in an unexpected state.");
                throw new GeneralException("AIP saved in the archival storage is in an unexpected state.");
            }
        }
        return stateAtArchivalStorage;
    }

    @Autowired
    public void setUserStore(UserStore userStore) {
        this.userStore = userStore;
    }

    @Autowired
    public void setBatchStore(BatchStore batchStore) {
        this.batchStore = batchStore;
    }

    @Autowired
    public void setProducerProfileStore(ProducerProfileStore producerProfileStore) {
        this.producerProfileStore = producerProfileStore;
    }

    @Autowired
    public void setFormatStore(IndexedFormatStore formatStore) {
        this.formatStore = formatStore;
    }

    @Autowired
    public void setIngestIssueStore(IngestIssueStore ingestIssueStore) {
        this.ingestIssueStore = ingestIssueStore;
    }

    @Autowired
    public void setIngestWorkflowService(IngestWorkflowService ingestWorkflowService) {
        this.ingestWorkflowService = ingestWorkflowService;
    }

    @Autowired
    public void setIndexedArclibXmlStore(SolrArclibXmlStore indexedArclibXmlStore) {
        this.indexedArclibXmlStore = indexedArclibXmlStore;
    }

    @Autowired
    public void setArchivalStorageService(ArchivalStorageService archivalStorageService) {
        this.archivalStorageService = archivalStorageService;
    }

    @Autowired
    public void setArchivalStorageServiceDebug(ArchivalStorageServiceDebug archivalStorageServiceDebug) {
        this.archivalStorageServiceDebug = archivalStorageServiceDebug;
    }

    @Autowired
    public void setReportStore(ReportStore reportStore) {
        this.reportStore = reportStore;
    }
}
