package cz.cas.lib.arclib.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.config.ReingestConfig;
import cz.cas.lib.arclib.domain.Hash;
import cz.cas.lib.arclib.domain.IngestRoutine;
import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.ingestWorkflow.WorkflowDefinition;
import cz.cas.lib.arclib.domain.packages.Sip;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.domain.reingest.Reingest;
import cz.cas.lib.arclib.domain.reingest.ReingestItem;
import cz.cas.lib.arclib.domain.reingest.ReingestState;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.exception.ForbiddenException;
import cz.cas.lib.arclib.exception.ReingestCantContinueException;
import cz.cas.lib.arclib.exception.ReingestInProgressException;
import cz.cas.lib.arclib.exception.ReingestStateException;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedAipState;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlDocument;
import cz.cas.lib.arclib.index.solr.arclibxml.SolrArclibXmlStore;
import cz.cas.lib.arclib.mail.ArclibMailCenter;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageException;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageService;
import cz.cas.lib.arclib.service.fixity.Md5Counter;
import cz.cas.lib.arclib.store.AipBulkDeletionStore;
import cz.cas.lib.arclib.store.IngestWorkflowStore;
import cz.cas.lib.arclib.store.ReingestItemStore;
import cz.cas.lib.arclib.store.ReingestStore;
import cz.cas.lib.core.index.dto.*;
import cz.cas.lib.core.scheduling.job.Job;
import cz.cas.lib.core.scheduling.job.JobService;
import cz.cas.lib.core.script.ScriptType;
import cz.cas.lib.core.store.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Stream;

import static cz.cas.lib.arclib.service.ExportInfoFileService.EXPORT_INFO_FILE_NAME;
import static cz.cas.lib.arclib.utils.ArclibUtils.SUMS_EXTENSION;
import static cz.cas.lib.core.util.Utils.notNull;

@Component
@Slf4j
public class ReingestService {

    private IngestWorkflowStore ingestWorkflowStore;
    private ReingestConfig reingestConfig;
    private ReingestStore reingestStore;
    private ReingestItemStore reingestItemStore;
    private FileLocationResolver fileLocationResolver;
    private Md5Counter md5Counter;
    private ProducerProfileService producerProfileService;
    private TransactionTemplate transactionTemplate;
    private UserDetails userDetails;
    private IngestRoutineService ingestRoutineService;
    private ArchivalStorageService archivalStorageService;
    private ExportInfoFileService exportInfoFileService;
    private Resource reingestExportScript;
    private JobService jobService;
    private IngestWorkflowService ingestWorkflowService;
    private CoordinatorService coordinatorService;
    private AipBulkDeletionStore aipBulkDeletionStore;
    private SolrArclibXmlStore arclibXmlIndexStore;
    private ArclibMailCenter mailCenter;
    private ObjectMapper objectMapper;

    public Reingest getCurrent() {
        return reingestStore.getCurrent();
    }

    public Reingest initiateReingest() throws ReingestStateException {
        log.info("initiating new reingest");
        Reingest current = reingestStore.getCurrent();
        if (current != null) {
            throw new ReingestStateException("can't initiate new reingest since other reingest " +
                    current.getId() + " is still present");
        }
        if (aipBulkDeletionStore.isAnyRunning()) {
            throw new ForbiddenException("can't initiate reingest since some bulk deletion request is running");
        }

        Reingest reingestEntity = new Reingest();
        reingestEntity.setState(ReingestState.INITIATED);
        Job exportJob = new Job();
        exportJob.setTiming(reingestConfig.getExportCron());
        exportJob.setActive(false);
        exportJob.setName("Reingest " + reingestEntity.getId() + " export job");
        exportJob.setScriptType(ScriptType.GROOVY);
        try {
            String script = StreamUtils.copyToString(reingestExportScript.getInputStream(), Charset.defaultCharset());
            exportJob.setScript(script);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        reingestEntity.setExporterJob(exportJob);

        transactionTemplate.executeWithoutResult(t -> {
            jobService.save(reingestEntity.getExporterJob());
            reingestStore.save(reingestEntity);
        });

        try {
            Map<Pair<Producer, WorkflowDefinition>, ProducerProfile> newProducerProfiles = new HashMap<>();
            Map<ProducerProfile, Map<String, String>> configHashes = new HashMap<>();
            List<ReingestItem> reingestItems = new ArrayList<>();

            log.info("preparing DB entities");

            Params p = new Params();
            p.getFilter().add(new Filter(IndexedArclibXmlDocument.AIP_STATE, FilterOperation.EQ, IndexedAipState.ARCHIVED.toString(), List.of()));
            p.getFilter().add(new Filter(IndexedArclibXmlDocument.LATEST, FilterOperation.EQ, "true", List.of()));
            p.getFilter().add(new Filter(IndexedArclibXmlDocument.DEBUG_MODE, FilterOperation.EQ, "false", List.of()));
            p.addSorting(new SortSpecification(IndexedArclibXmlDocument.CREATED, Order.ASC));
            List<String> ids = arclibXmlIndexStore.findAllIgnorePagination(p).getItems().stream().map(IndexedArclibXmlDocument::getId).toList();
            List<IngestWorkflow> iws = ingestWorkflowStore.findAllInListByExternalIds(ids);

            String timestamp = LocalDateTime.now().toString();
            for (IngestWorkflow iw : iws) {
                iw = getIngestedWorkflow(iw);
                LocalDateTime ingestTime = LocalDateTime.ofInstant(iw.getCreated(), ZoneId.systemDefault());
                if (ingestTime.getYear() < 2021) {
                    log.debug("skipping reingest of {} created before 2021 ({})", iw.getExternalId(), ingestTime);
                    continue;
                }
                Pair<Producer, WorkflowDefinition> key = Pair.of(iw.getProducerProfile().getProducer(), iw.getProducerProfile().getWorkflowDefinition());
                ProducerProfile producerProfile = newProducerProfiles.computeIfAbsent(key, k -> {
                    ProducerProfile pp = new ProducerProfile(getProducerProfileExternalId(key, timestamp), key.getLeft(), null, null, null, key.getRight(), false, true);
                    pp.setName(pp.getExternalId());
                    return pp;
                });
                producerProfile.setName(producerProfile.getExternalId());
                String jsonConfig = iw.getFinalConfig();
                String configMd5 = md5Counter.getDigestInHex(new ByteArrayInputStream(jsonConfig.getBytes()));
                configHashes.computeIfAbsent(producerProfile, k -> new HashMap<>()).put(configMd5, jsonConfig);
                reingestItems.add(new ReingestItem(reingestEntity, iw, producerProfile, configMd5));
            }

            log.info("creating folders and config files");
            for (ProducerProfile producerProfile : configHashes.keySet()) {
                Map<String, String> configsOfProducerProfile = configHashes.get(producerProfile);
                for (String configHash : configsOfProducerProfile.keySet()) {
                    String config = configsOfProducerProfile.get(configHash);
                    Path reingestWorkflowConfigPath = fileLocationResolver.getReingestWorkflowConfigFile(reingestEntity, producerProfile, configHash);
                    Files.createDirectories(reingestWorkflowConfigPath.getParent());
                    Files.write(reingestWorkflowConfigPath, config.getBytes());
                }
            }

            log.info("storing reingest init data to DB, setting {} state", ReingestState.JOB_STOPPED);
            transactionTemplate.executeWithoutResult(t -> {
                reingestEntity.setState(ReingestState.JOB_STOPPED);
                reingestEntity.setSize(reingestItems.size());
                reingestStore.save(reingestEntity);
                producerProfileService.save(newProducerProfiles.values());
                reingestItemStore.save(reingestItems);
                for (ProducerProfile pp : newProducerProfiles.values()) {
                    Job job = new Job();
                    job.setTiming("0 0 9 1 12 ? 2099");
                    job.setActive(false);
                    //transfer area path is null - the routine will serve all subfolders of the producer profile folder
                    IngestRoutine routine = new IngestRoutine(
                            job, pp, null, null, userDetails.getUser(), List.of(), true, true
                    );
                    routine.setName(pp.getName());
                    ingestRoutineService.save(routine);
                }
            });
            return reingestEntity;
        } catch (Exception e) {
            log.error("REINGEST INIT FAILED", e);
            reingestEntity.setState(ReingestState.INIT_FAILED);
            transactionTemplate.executeWithoutResult(t -> reingestStore.save(reingestEntity));
            return reingestEntity;
        }
    }

    @Transactional
    public Reingest startReingestJob() throws ReingestStateException {
        log.info("resuming reingest job");
        Reingest reingest = reingestStore.getCurrent();
        notNull(reingest, () -> new MissingObject(Reingest.class, "current"));
        switch (reingest.getState()) {
            case JOB_FINISHED -> {
                log.info("skipping reingest resume request since reingest job has finished");
                return reingest;
            }
            case JOB_STOPPED -> {
            }
            default ->
                    throw new ReingestStateException("can't start reingest export since the reingest is not in " + ReingestState.JOB_STOPPED + " state");
        }
        Job job = reingest.getExporterJob();
        job.setActive(true);
        reingest.setState(ReingestState.JOB_RUNNING);
        reingestStore.save(reingest);
        jobService.save(job);
        return reingest;
    }

    @Transactional
    public Reingest stopReingestJob() throws ReingestStateException {
        log.info("stopping reingest job");
        Reingest reingest = reingestStore.getCurrent();
        notNull(reingest, () -> new MissingObject(Reingest.class, "current"));
        switch (reingest.getState()) {
            case JOB_FINISHED -> {
                log.info("skipping reingest stop request since reingest job has finished");
                return reingest;
            }
            case JOB_RUNNING -> {
            }
            default ->
                    throw new ReingestStateException("can't stop reingest export since the reingest is not in " + ReingestState.JOB_RUNNING + " state");
        }
        Job job = reingest.getExporterJob();
        job.setActive(false);
        reingest.setState(ReingestState.JOB_STOPPED);
        reingestStore.save(reingest);
        jobService.save(job);
        return reingest;
    }

    public Long countPackagesInTransferArea() throws ReingestStateException {
        log.info("stopping reingest job");
        Reingest reingestEntity = reingestStore.getCurrent();
        notNull(reingestEntity, () -> new MissingObject(Reingest.class, "current"));

        List<IngestRoutine> ingestRoutines = ingestRoutineService.getReingestRoutines();
        long count = 0;
        for (IngestRoutine ir : ingestRoutines) {
            File routineTransferAreaFolder = fileLocationResolver.getReingestProducerProfileFolder(reingestEntity, ir.getProducerProfile()).toFile();
            File[] uniqueConfigFolders = routineTransferAreaFolder.listFiles();
            count = count + Arrays.stream(uniqueConfigFolders).flatMap(f -> Arrays.stream(f.list())).filter(f -> f.endsWith(SUMS_EXTENSION)).count();
        }
        return count;
    }

    public void exportReingestBatch() throws ReingestStateException, ArchivalStorageException, IOException {
        log.info("reingest job instance started");
        Reingest reingestEntity = reingestStore.getCurrent();

        if (reingestEntity == null || reingestEntity.getState() != ReingestState.JOB_RUNNING) {
            throw new ReingestStateException("can't run export job of reingest " + reingestEntity.getId() + " since it is not in " + ReingestState.JOB_RUNNING + " state");
        }

        List<IngestRoutine> ingestRoutines = ingestRoutineService.getReingestRoutines();

        if (!checkFoldersAreEmpty(reingestEntity, ingestRoutines)) {
            log.debug("skipping export of new data for reingest since some ingest routines folders are still not empty");
            return;
        }

        try {
            //noone should set the job active anyway, but to be sure, set it active=false explicitly
            ingestRoutines.stream().map(IngestRoutine::getJob).filter(Job::getActive).forEach(activeJob -> {
                activeJob.setActive(false);
                jobService.save(activeJob);
            });

            Pair<List<ReingestItem>, Integer> batchingInput = prepareExportBatch(reingestEntity);
            if (batchingInput == null) {
                return;
            }

            try {
                exportAips(reingestEntity, batchingInput.getLeft());

                //if it fails before transaction with next offset is committed it will result in possible duplicate ingest workflows
                log.info("creating batches (if data present in transfer area) of {} ingest routines", ingestRoutines.size());
                int counter = 0;
                for (IngestRoutine ingestRoutine : ingestRoutines) {
                    coordinatorService.processBatchOfSips(ingestRoutine.getId());
                    log.info("{}/{} transfer area folders scanned", ++counter, ingestRoutines.size());
                }

                reingestEntity.setNextOffset(reingestEntity.getNextOffset() + batchingInput.getLeft().size() + batchingInput.getRight());
                transactionTemplate.executeWithoutResult(t -> {
                    reingestStore.save(reingestEntity);
                });
            } catch (Exception e) {
                handleReingestJobException(reingestEntity, ingestRoutines, e);
                throw e;
            }
        } catch (Exception e) {
            stopJobAndSendMail(e);
            throw e;
        }
    }

    private void handleReingestJobException(Reingest reingestEntity, List<IngestRoutine> ingestRoutines, Exception e) {
        log.warn("reingest job failed, clearing workspace of {} ingest routines", ingestRoutines.size());
        int counter = 0;
        for (IngestRoutine ir : ingestRoutines) {
            File routineTransferAreaFolder = fileLocationResolver.getReingestProducerProfileFolder(reingestEntity, ir.getProducerProfile()).toFile();
            File[] uniqueConfigFolders = routineTransferAreaFolder.listFiles();
            Stream<File> filesToDelete = Arrays.stream(uniqueConfigFolders).flatMap(f -> Arrays.stream(f.listFiles())).filter(f -> !f.getName().equals("config.json") && !f.getName().equals("config.json.skip"));
            filesToDelete.forEach(f -> FileUtils.deleteQuietly(f));
            log.info("{}/{} cleared", ++counter, ingestRoutines.size());
        }
    }

    private boolean checkFoldersAreEmpty(Reingest reingestEntity, List<IngestRoutine> ingestRoutines) {
        return ingestRoutines.stream().allMatch(ir -> {
            File routineTransferAreaFolder = fileLocationResolver.getReingestProducerProfileFolder(reingestEntity, ir.getProducerProfile()).toFile();
            File[] uniqueConfigFolders = routineTransferAreaFolder.listFiles();
            //all folders must be empty, with the exception of config.json files, that can be present
            return Arrays.stream(uniqueConfigFolders).flatMap(f -> Arrays.stream(f.list())).allMatch(f -> f.equals("config.json") || f.equals("config.json.skip") || f.endsWith(EXPORT_INFO_FILE_NAME));
        });
    }

    private void exportAips(Reingest reingestEntity, List<ReingestItem> itemsForReingest) throws ArchivalStorageException, IOException {
        log.info("exporting {} AIPs from archival storage", itemsForReingest.size());
        int counter = 0;
        for (ReingestItem reingestItem : itemsForReingest) {
            Sip sip = reingestItem.getIngestWorkflow().getSip();
            String sipId = sip.getId();
            Path transferAreaPath = fileLocationResolver.getReingestWorkflowConfigFolder(reingestEntity, reingestItem.getProducerProfile(), reingestItem.getConfigMd5());
            InputStream aipStream = archivalStorageService.exportAipData(sipId);
            Path path = transferAreaPath.resolve("TRANSFERRING_" + sipId + ".zip");
            try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
                IOUtils.copyLarge(aipStream, fos);
            }
            Hash hash = sip.getHashes().iterator().next();
            Files.write(transferAreaPath.resolve(sipId + ".sums"), List.of(hash.getHashType() + " " + hash.getHashValue()));
            Path exportInfoFilePath = transferAreaPath.resolve(sipId + ".zip." + EXPORT_INFO_FILE_NAME);
            exportInfoFileService.write(exportInfoFilePath, Map.of(ExportInfoFileService.KEY_AUTHORIAL_PACKAGE_UUID, sip.getAuthorialPackage().getId()));
            log.info("{}/{} exported", ++counter, itemsForReingest.size());
        }
        log.info("removing TRANSFERRING suffix");
        for (ReingestItem reingestItem : itemsForReingest) {
            String sipId = reingestItem.getIngestWorkflow().getSip().getId();
            Path transferAreaPath = fileLocationResolver.getReingestWorkflowConfigFolder(reingestEntity, reingestItem.getProducerProfile(), reingestItem.getConfigMd5());
            Files.move(transferAreaPath.resolve("TRANSFERRING_" + sipId + ".zip"), transferAreaPath.resolve(sipId + ".zip"));
        }
    }

    @Transactional
    public void terminateReingest() throws ReingestStateException, IOException, ReingestInProgressException {
        Reingest current = reingestStore.getCurrent();
        if (current == null) {
            throw new ReingestStateException("can't terminate reingest since there is no active reingest");
        }
        ingestRoutineService.deleteReingestRoutines();
        producerProfileService.deleteReindexProfiles();
        reingestStore.delete(current);
        jobService.delete(current.getExporterJob());
        File reingestDir = fileLocationResolver.getReingestFolder(current).toFile();
        if (reingestDir.exists()) {
            FileUtils.deleteDirectory(reingestDir);
        }
    }

    /**
     * @param reingestEntity
     * @return null if the job execution should be skipped, list of items for reingest and count of items to skip otherwise
     */
    private Pair<List<ReingestItem>, Integer> prepareExportBatch(Reingest reingestEntity) throws IOException {
        MutableLong futureWorkspaceFreeSpace = new MutableLong(getWorkspaceFreeSpace());
        MutableLong futureTransferAreaFreeSpace;
        if (reingestConfig.isSharedStorage()) {
            futureTransferAreaFreeSpace = futureWorkspaceFreeSpace;
        } else {
            futureTransferAreaFreeSpace = new MutableLong(getTransferAreaFreeSpace(reingestEntity));
        }
        boolean workspaceSizeLimitReached = false;
        boolean transferAreaSizeLimitReached = false;

        int offset = reingestEntity.getNextOffset();
        List<ReingestItem> itemsForReingest = new ArrayList<>();
        ReingestItem next = reingestItemStore.findNext(reingestEntity, offset);

        if (next == null) {
            log.info("no more packages to export into transfer area, reingest job finishing");
            reingestEntity.setState(ReingestState.JOB_FINISHED);
            reingestEntity.getExporterJob().setActive(false);
            transactionTemplate.executeWithoutResult(t -> {
                jobService.save(reingestEntity.getExporterJob());
                reingestStore.save(reingestEntity);
            });
            return null;
        }

        int skipCount = 0;
        while (next != null && !transferAreaSizeLimitReached && !workspaceSizeLimitReached) {
            IngestWorkflow iw = next.getIngestWorkflow();
            iw = getIngestedWorkflow(iw);

            Path skipFile = fileLocationResolver.getReingestWorkflowConfigSkipFile(reingestEntity, next.getProducerProfile(), next.getConfigMd5());
            if (skipFile.toFile().exists()) {
                log.info("reingest of {} will be skipped as skipfile exists at {}", iw.getExternalId(), skipFile.toAbsolutePath());
                skipCount++;
                continue;
            }

            futureTransferAreaFreeSpace.subtract(iw.getSip().getSizeInBytes());
            transferAreaSizeLimitReached = reingestConfig.getTransferAreaKeepFreeMb() > futureTransferAreaFreeSpace.getValue() / 1000000;
            futureWorkspaceFreeSpace.subtract(iw.getSip().getSizeInBytes());
            futureWorkspaceFreeSpace.subtract(getSizeOfUnpackedSip(iw));
            workspaceSizeLimitReached = reingestConfig.getWorkspaceKeepFreeMb() > futureWorkspaceFreeSpace.getValue() / 1000000;

            if (!transferAreaSizeLimitReached && !workspaceSizeLimitReached) {
                itemsForReingest.add(next);
                offset++;
                next = reingestItemStore.findNext(reingestEntity, offset);
            }
        }
        if (itemsForReingest.isEmpty() && skipCount == 0 && (workspaceSizeLimitReached || transferAreaSizeLimitReached)) {
            ReingestCantContinueException ex = createSizeTooBigException(reingestEntity, next.getIngestWorkflow(), workspaceSizeLimitReached);
            stopJobAndSendMail(ex);
            return null;
        }

        return Pair.of(itemsForReingest, skipCount);
    }

    private String getProducerProfileExternalId(Pair<Producer, WorkflowDefinition> pair, String timestamp) {
        return String.join("_", "reingest", timestamp, pair.getLeft().getId(), pair.getRight().getId());
    }

    private IngestWorkflow getIngestedWorkflow(IngestWorkflow ingestWorkflow) {
        if (ingestWorkflow == null) {
            return null;
        }
        if (ingestWorkflow.getBatch() != null) {
            return ingestWorkflow;
        }
        return getIngestedWorkflow(ingestWorkflow.getRelatedWorkflow());
    }

    private long getSizeOfUnpackedSip(IngestWorkflow iw) {
        Map<String, Map<String, Triple<Long, String, String>>> fixityData = (Map<String, Map<String, Triple<Long, String, String>>>)
                ingestWorkflowService.getVariable(getIngestedWorkflow(iw).getExternalId(), BpmConstants.FixityGeneration.mapOfEventIdsToSipContentFixityData);
        Map<String, Triple<Long, String, String>> anyFixityOutput = fixityData.values().iterator().next();
        return anyFixityOutput.values().stream().mapToLong(Triple::getLeft).sum();
    }

    private void stopJobAndSendMail(Exception e) {
        try {
            mailCenter.sendReingestJobFailedNotification(e);
        } catch (Exception ee) {
            log.error("failed while sending mail", ee);
        }
        transactionTemplate.executeWithoutResult(t -> {
            try {
                stopReingestJob();
            } catch (ReingestStateException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    private ReingestCantContinueException createSizeTooBigException(Reingest reingestEntity, IngestWorkflow iw, boolean workspaceLimitReached) {
        iw = getIngestedWorkflow(iw);
        long sipFolderMb = getSizeOfUnpackedSip(iw) / 1000000;
        long sipZipMb = iw.getSip().getSizeInBytes() / 1000000;

        String place;
        long keepFreeMbConfig;
        long freeSpaceMb;
        if (workspaceLimitReached) {
            place = "workspace";
            keepFreeMbConfig = reingestConfig.getWorkspaceKeepFreeMb();
            freeSpaceMb = getWorkspaceFreeSpace() / 1000000;
        } else {
            place = "transfer area";
            keepFreeMbConfig = reingestConfig.getTransferAreaKeepFreeMb();
            freeSpaceMb = getTransferAreaFreeSpace(reingestEntity) / 1000000;
        }

        String msg = String.format("can't reingest %s as its size (%s MB unpacked, %s MB packed) is greater than free space in %s " +
                        "(%s MB free space with %s keep free MB config) ... if workspace and transfer area storage is shared than even more " +
                        "free space is required",
                iw.getExternalId(), sipFolderMb, sipZipMb, place, freeSpaceMb, keepFreeMbConfig);
        return new ReingestCantContinueException(msg);
    }

    private long getWorkspaceFreeSpace() {
        return fileLocationResolver.getWorkspaceFolder().toFile().getFreeSpace();
    }

    private long getTransferAreaFreeSpace(Reingest reingestEntity) {
        return fileLocationResolver.getReingestFolder(reingestEntity).toFile().getFreeSpace();
    }

    @Autowired
    public void setReingestConfig(ReingestConfig reingestConfig) {
        this.reingestConfig = reingestConfig;
    }

    @Autowired
    public void setIngestWorkflowStore(IngestWorkflowStore ingestWorkflowStore) {
        this.ingestWorkflowStore = ingestWorkflowStore;
    }

    @Autowired
    public void setReingestStore(ReingestStore reingestStore) {
        this.reingestStore = reingestStore;
    }

    @Autowired
    public void setReingestItemStore(ReingestItemStore reingestItemStore) {
        this.reingestItemStore = reingestItemStore;
    }

    @Autowired
    public void setFileLocationResolver(FileLocationResolver fileLocationResolver) {
        this.fileLocationResolver = fileLocationResolver;
    }

    @Autowired
    public void setMd5Counter(Md5Counter md5Counter) {
        this.md5Counter = md5Counter;
    }

    @Autowired
    public void setProducerProfileService(ProducerProfileService producerProfileService) {
        this.producerProfileService = producerProfileService;
    }

    @Autowired
    public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    @Autowired
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }

    @Autowired
    public void setIngestRoutineService(IngestRoutineService ingestRoutineService) {
        this.ingestRoutineService = ingestRoutineService;
    }

    @Autowired
    public void setArchivalStorageService(ArchivalStorageService archivalStorageService) {
        this.archivalStorageService = archivalStorageService;
    }

    @Autowired
    public void setExportInfoFileService(ExportInfoFileService exportInfoFileService) {
        this.exportInfoFileService = exportInfoFileService;
    }

    @Autowired
    public void setReingestExportScript(@Value("${arclib.script.reingestExport}") Resource reingestExportScript) {
        this.reingestExportScript = reingestExportScript;
    }

    @Autowired
    public void setJobService(JobService jobService) {
        this.jobService = jobService;
    }

    @Autowired
    public void setIngestWorkflowService(IngestWorkflowService ingestWorkflowService) {
        this.ingestWorkflowService = ingestWorkflowService;
    }

    @Autowired
    public void setCoordinatorService(CoordinatorService coordinatorService) {
        this.coordinatorService = coordinatorService;
    }

    @Autowired
    public void setAipBulkDeletionStore(AipBulkDeletionStore aipBulkDeletionStore) {
        this.aipBulkDeletionStore = aipBulkDeletionStore;
    }

    @Autowired
    public void setArclibXmlIndexStore(SolrArclibXmlStore arclibXmlIndexStore) {
        this.arclibXmlIndexStore = arclibXmlIndexStore;
    }

    @Autowired
    public void setMailCenter(ArclibMailCenter mailCenter) {
        this.mailCenter = mailCenter;
    }

    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
}
