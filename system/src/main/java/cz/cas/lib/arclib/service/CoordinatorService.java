package cz.cas.lib.arclib.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import cz.cas.lib.arclib.bpm.ArclibXmlExtractorDelegate;
import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.bpm.ValidatorDelegate;
import cz.cas.lib.arclib.domain.*;
import cz.cas.lib.arclib.domain.ingestWorkflow.*;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.ForbiddenOperation;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.dto.JmsDto;
import cz.cas.lib.arclib.mail.ArclibMailCenter;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.store.HashStore;
import cz.cas.lib.arclib.store.IngestRoutineStore;
import cz.cas.lib.arclib.utils.ArclibUtils;
import cz.cas.lib.arclib.utils.JsonUtils;
import cz.cas.lib.core.store.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static cz.cas.lib.arclib.domain.AutoIngestFilePrefix.fileNameIsNotPrefixed;
import static cz.cas.lib.arclib.utils.ArclibUtils.*;
import static cz.cas.lib.core.util.Utils.*;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Slf4j
@Service
public class CoordinatorService {

    private IngestWorkflowService ingestWorkflowService;
    private ProducerProfileService producerProfileService;
    private HashStore hashStore;
    private BatchService batchService;
    private JmsTemplate template;
    private RepositoryService repositoryService;
    private UserDetails userDetails;
    private String fileStorage;
    private ArclibMailCenter arclibMailCenter;
    private UserService userService;
    private IngestErrorHandler ingestErrorHandler;
    private RuntimeService runtimeService;
    private TransactionTemplate transactionTemplate;
    private IngestRoutineStore ingestRoutineStore;

    /**
     * Processes one SIP package:
     * <p>
     * 1. copies the SIP content from <code>sipContent</code> to the temporary area
     * 2. creates new ingest workflow package from the provided SIP content and hash
     * 2. triggers processing of ingest workflow
     *
     * @param sipContent          content of the SIP package
     * @param hash                hash of the SIP package
     * @param externalId          external id of producer profile to use
     * @param batchWorkflowConfig JSON config configuring the BPMN ingest workflow process
     * @param originalFileName    original file name of the SIP
     * @param transferAreaPath    custom path to the transfer area to which the SIP content is copied from <code>sipContent</code>
     * @return id of the created batch
     */
    public String processSip(InputStream sipContent, Hash hash, String externalId, String batchWorkflowConfig,
                             String originalFileName, String transferAreaPath) throws IOException {
        log.info("Processing of one SIP package from the provided SIP content has been triggered.");

        if (batchWorkflowConfig.isEmpty()) throw new BadArgument("Workflow config is empty.");
        if (hash.getHashType() == null) throw new BadArgument("Hash type empty.");
        if (hash.getHashValue() == null) throw new BadArgument("Hash value is empty.");

        ProducerProfile producerProfile = producerProfileService.findByExternalId(externalId);
        notNull(producerProfile, () -> new MissingObject(ProducerProfile.class, externalId));

        Producer producer = producerProfile.getProducer();
        notNull(producer, () -> new IllegalArgumentException("null producer in the producer profile with external id " + producerProfile.getExternalId()));

        verifyProducer(producer, "User cannot process SIP when producer external ID does not match user's producer.");

        Path fullTransferAreaPath = computeTransferAreaPath(transferAreaPath, producer);
        if (!new File(fullTransferAreaPath.toString()).exists()) {
            throw new GeneralException("There is no folder on the path " + fullTransferAreaPath.toString() +
                    ". Please specify a valid path.");
        }
        Path absoluteTransferAreaPath = fullTransferAreaPath.toAbsolutePath();
        String fileNameWithoutFileExtension = originalFileName.substring(0, originalFileName.lastIndexOf("."));

        if (!(fileNameWithoutFileExtension + ArclibUtils.ZIP_EXTENSION).equals(originalFileName))
            throw new GeneralException("Cannot process SIP without a .zip extension");

        String fileName = originalFileName;
        Path sipPath = absoluteTransferAreaPath.resolve(fileName);

        for (int num = 1; Files.exists(sipPath); num++) {
            fileName = fileNameWithoutFileExtension + " (" + num + ")" + ArclibUtils.ZIP_EXTENSION;
            sipPath = absoluteTransferAreaPath.resolve(fileName);
        }

        Files.copy(sipContent, sipPath, REPLACE_EXISTING);
        log.debug("SIP content of file name " + fileName + " has been copied to transfer area at the path " +
                sipPath.toString());

        IngestWorkflow ingestWorkflow = new IngestWorkflow();
        ingestWorkflow.setProcessingState(IngestWorkflowState.NEW);
        ingestWorkflow.setHash(hash);
        ingestWorkflow.setFileName(fileName);
        String computedWorkflowConfig = computeIngestWorkflowConfig(batchWorkflowConfig, producerProfile);
        ingestWorkflow.setInitialConfig(computedWorkflowConfig);
        String batchId = transactionTemplate.execute(s -> {
            hashStore.save(hash);
            ingestWorkflowService.save(ingestWorkflow);
            return prepareBatch(asList(ingestWorkflow), producerProfile, batchWorkflowConfig, computedWorkflowConfig, fullTransferAreaPath, userDetails.getId(), null).getId();
        });
        log.debug("Sending a message to Worker to process ingest workflow with external id " + ingestWorkflow.getExternalId() + ".");
        template.convertAndSend("worker", new JmsDto(ingestWorkflow.getExternalId(), userDetails.getId()));
        return batchId;
    }

    /**
     * Creates and runs a batch of ingest workflow processes. A single instance of ingest workflow is created
     * for each SIP located in the respective transfer area.
     *
     * @param externalId          external id of producer profile
     * @param batchWorkflowConfig JSON configuration of ingest workflow on the batch level
     * @param transferAreaPath    custom transfer area path for the given batch (overrides the transfer area path of producer)
     * @param userId              id of the user
     * @param routineId           id of the ingest routine
     * @return id of the created batch or 'null' if no batch has been created
     */
    public String processBatchOfSips(String externalId, String batchWorkflowConfig, String transferAreaPath, String userId, String routineId) throws IOException {
        IngestRoutine ingestRoutine = ingestRoutineStore.findWithBatchesFilled(routineId);

        if (!ingestRoutine.isAuto() && !ingestRoutine.getCurrentlyProcessingBatches().isEmpty()) {
            log.debug("Skipping job of ingest routine: {} because there is still batch with id: {} processing", routineId, ingestRoutine.getCurrentlyProcessingBatches().get(0));
            return null;
        }

        log.trace("Processing of a batch of SIP packages has been triggered, routine id: {}", routineId);

        if (batchWorkflowConfig.isEmpty()) throw new BadArgument("Workflow config is empty. Try to use '{}'");

        ProducerProfile producerProfile = producerProfileService.findByExternalId(externalId);
        notNull(producerProfile, () -> new MissingObject(ProducerProfile.class, externalId));
        Producer producer = producerProfile.getProducer();
        notNull(producer, () -> new IllegalArgumentException("null producer in the producer profile with external id " + producerProfile.getExternalId()));

        String mergedWorkflowConfig = computeIngestWorkflowConfig(batchWorkflowConfig, producerProfile);

        Path fullTransferAreaPath = computeTransferAreaPath(transferAreaPath, producer);
        File transferArea = new File(fullTransferAreaPath.toString());
        if (!transferArea.exists()) {
            log.error("There is no folder on the path: {} ", fullTransferAreaPath.toString());
            return null;
        }

        String assignedUserId = userId == null ? userDetails.getId() : userId;
        List<IngestWorkflow> ingestWorkflows = new ArrayList<>();
        String batchId = transactionTemplate.execute(s -> {
            ingestWorkflows.addAll(scanTransferArea(transferArea, mergedWorkflowConfig, ingestRoutine.isAuto()));
            if (ingestWorkflows.isEmpty()) {
                return null;
            }

            log.debug("Merged workflow config to be used: {}", mergedWorkflowConfig);
            Batch batch = prepareBatch(ingestWorkflows, producerProfile, batchWorkflowConfig, mergedWorkflowConfig, fullTransferAreaPath, assignedUserId, ingestRoutine);
            ingestRoutine.getCurrentlyProcessingBatches().add(batch);

            // prefixing with PROCESSING_<file_name>
            if (ingestRoutine.isAuto()) {
                ingestWorkflows.forEach(w -> {
                    log.debug("Prefixing file:'" + w.getFileName() + "' with:'" + AutoIngestFilePrefix.PROCESSING.getPrefix() + "'.");
                    changeFilePrefix(AutoIngestFilePrefix.NONE, AutoIngestFilePrefix.PROCESSING, w);
                });
            }
            return batch.getId();
        });

        ingestWorkflows.forEach(ingestWorkflow -> {
            log.debug("Sending a message to Worker to process ingest workflow with external id " + ingestWorkflow.getExternalId() + ".");
            template.convertAndSend("worker", new JmsDto(ingestWorkflow.getExternalId(), assignedUserId));
        });

        return batchId;
    }

    /**
     * Mark the batch as PROCESSED if all the belongings ingest workflows correspond one of the states PROCESSED or FAILED
     * and batch is in the state PROCESSING.
     *
     * @param dto dto containing the id of the batch to finish and id of the user that should be notified about the batch result
     */
    @Transactional
    @JmsListener(destination = "finish")
    public void finishBatch(JmsDto dto) {
        String batchId = dto.getId();
        User user = userService.find(dto.getUserId());
        notNull(user, () -> new MissingObject(User.class, dto.getUserId()));

        Batch batch = batchService.get(batchId);
        notNull(batch, () -> new MissingObject(Batch.class, batchId));

        boolean allIngestWorkflowsInFinalState = batch.getIngestWorkflows().stream()
                .allMatch(s -> s.getProcessingState() == IngestWorkflowState.PERSISTED || s.getProcessingState() == IngestWorkflowState.FAILED);

        if (allIngestWorkflowsInFinalState && batch.getState() == BatchState.PROCESSING) {
            batch.setPendingIncidents(false);
            batch.setIngestRoutine(null);

            if (batch.getIngestWorkflows().stream().anyMatch(iw -> iw.getProcessingState() == IngestWorkflowState.FAILED))
                batch.setState(BatchState.PROCESSED_WITH_FAILURES);
            else
                batch.setState(BatchState.PROCESSED);

            batchService.save(batch);
            log.info("Batch " + batchId + " has been processed. The batch state changed to " + batch.getState());
            if (user.getEmail() != null) {
                arclibMailCenter.sendIngestResultNotification(user.getEmail(), batchId, batch.getState().toString(), Instant.now());
            }
        }
    }

    /**
     * Cancels processing of the batch by updating its state to CANCELED.
     *
     * @param dto dto with id of the batch to finish and id of the user that should be notified about the batch result
     */
    @JmsListener(destination = "cancel")
    public void cancelBatch(JmsDto dto) {
        String batchId = dto.getId();
        User user = userService.find(dto.getUserId());
        notNull(user, () -> new MissingObject(User.class, dto.getUserId()));

        Batch batch = batchService.get(batchId);
        notNull(batch, () -> new MissingObject(Batch.class, batchId));
        //should be called before the transaction which sets batch to CANCELLED, otherwise ingest error handler hangs
        batch.getIngestWorkflows().stream()
                //not cancelling those in PROCESSED state as BPM engine itself will move those to final state, i.e.
                //PERSISTED or FAILED depending on the Archival Storage success or fail
                .filter(iw -> iw.getProcessingState().equals(IngestWorkflowState.PROCESSING))
                .forEach(iw -> {
                    ProcessInstance processInstance =
                            runtimeService.createProcessInstanceQuery()
                                    .variableValueEquals(BpmConstants.ProcessVariables.ingestWorkflowExternalId, iw.getExternalId())
                                    .singleResult();

                    IngestWorkflowFailureInfo failureInfo = new IngestWorkflowFailureInfo();
                    failureInfo.setMsg("Batch has been canceled.");
                    failureInfo.setStackTrace("");
                    failureInfo.setIngestWorkflowFailureType(IngestWorkflowFailureType.BATCH_CANCELLATION);

                    String processInstanceId = processInstance != null ? processInstance.getProcessInstanceId() : null;
                    ingestErrorHandler.handleError(iw.getExternalId(), failureInfo, processInstanceId, user.getId());
                });
        transactionTemplate.execute(s -> {
            batch.setState(BatchState.CANCELED);
            batch.setPendingIncidents(false);
            batch.setIngestRoutine(null);
            batchService.save(batch);
            return null;
        });
        log.info("Batch " + batch.getId() + " has been canceled. The batch state changed to CANCELED.");
        if (user.getEmail() != null) {
            arclibMailCenter.sendIngestResultNotification(user.getEmail(), batchId, BatchState.CANCELED.toString(), Instant.now());
        }
    }

    /**
     * Suspends processing of the batch by updating its state to SUSPENDED.
     *
     * @param batchId id of the batch
     */
    @Transactional
    public void suspendBatch(String batchId) {
        Batch batch = batchService.find(batchId);
        notNull(batch, () -> new MissingObject(Batch.class, batchId));

        notNull(batch.getProducerProfile(), () -> new IllegalArgumentException("producer profile of batch " + batchId + " is null"));
        notNull(batch.getProducerProfile().getProducer(), () -> new IllegalArgumentException("producer of batch " + batchId + " is null"));
        verifyProducer(batch.getProducerProfile().getProducer(), "User cannot suspend batch that does not belong to his producer.");

        batch.setState(BatchState.SUSPENDED);
        batchService.save(batch);

        log.info("Batch " + batch.getId() + " has been suspended. The batch state changed to SUSPENDED.");
    }

    /**
     * Resumes processing of the batch.
     * <p>
     * a) If the batch contains any ingest workflow package with the state PROCESSING, stops the resume process and returns <code>false</code>.
     * b) Otherwise, updates state of the batch to PROCESSING
     * and for each ingest workflow of the batch with the state NEW sends a JMS message to Worker.
     * If there are only ingest workflows with the state PROCESSED or FAILED, the batch state changes to PROCESSED.
     * At the end of the resume process the method returns <code>true</code>.
     *
     * @param batchId id of the batch
     * @return <code>false</code> if the batch contains at least one ingest workflow package with the state PROCESSING,
     * <code>true</code> otherwise
     */
    public Boolean resumeBatch(String batchId) {
        Batch batch = batchService.get(batchId);
        notNull(batch, () -> new MissingObject(Batch.class, batchId));
        notNull(batch.getProducerProfile(), () -> new IllegalArgumentException("producer profile of batch " + batchId + " is null"));
        notNull(batch.getProducerProfile().getProducer(), () -> new IllegalArgumentException("producer of batch " + batchId + " is null"));
        verifyProducer(batch.getProducerProfile().getProducer(), "User cannot resume batch that does not belong to his producer.");

        boolean hasProcessingIngestWorkflow = batch.getIngestWorkflows().stream()
                .anyMatch(ingestWorkflow -> ingestWorkflow.getProcessingState() == IngestWorkflowState.PROCESSING);
        if (hasProcessingIngestWorkflow) {
            log.warn("Batch " + batch.getId() + " has still some ingest workflow in the state PROCESSING. Processing of " +
                    "batch cannot be resumed.");
            return false;
        }

        batch.setState(BatchState.PROCESSING);
        List<IngestWorkflow> unprocessedIngestWorkflows = new ArrayList<>();
        transactionTemplate.execute(s -> {
            batchService.save(batch);
            log.info("Processing of batch " + batch.getId() + " has successfully resumed. The batch state changed to PROCESSING.");
            unprocessedIngestWorkflows.addAll(
                    batch.getIngestWorkflows().stream()
                            .filter(ingestWorkflow -> ingestWorkflow.getProcessingState() == IngestWorkflowState.NEW)
                            .collect(Collectors.toList()));
            if (unprocessedIngestWorkflows.isEmpty()) {
                if (batch.getIngestWorkflows().stream().anyMatch(iw -> iw.getProcessingState() == IngestWorkflowState.FAILED))
                    batch.setState(BatchState.PROCESSED_WITH_FAILURES);
                else
                    batch.setState(BatchState.PROCESSED);
                batchService.save(batch);
                log.info("Batch " + batchId + " has been processed. The batch state changed to PROCESSED.");
            }
            return batch;
        });
        unprocessedIngestWorkflows.forEach(ingestWorkflow -> template.convertAndSend(
                "worker", new JmsDto(ingestWorkflow.getExternalId(), userDetails.getId())));
        return true;
    }

    /**
     * For each file with a name ending with '.zip' located in the folder:
     * 1. Creates an instance of ingest workflow.
     * 2. Sets hash of the ingest workflow with the value defined in the file ending with '.sums'.
     * 3. Sets original file name of the ingest workflow with the name of the file without the file extension
     * 4. Sets state of the ingest workflow to NEW.
     *
     * @param folder folder containing files to be processed
     * @return list of created ingest workflows
     */
    private List<IngestWorkflow> scanTransferArea(File folder, String initialIngestWorkflowConfig, boolean automaticProcessing) {
        log.trace("Scanning transfer area at path " + folder.toPath().toString() + " for SIP packages.");
        List<IngestWorkflow> ingestWorkflows = Arrays
                .stream(folder.listFiles())
                .filter(f -> {
                    String fileName = f.toPath().getFileName().toString().toLowerCase();
                    return automaticProcessing
                            ? fileNameIsNotPrefixed(fileName) && fileName.endsWith(ZIP_EXTENSION)
                            : fileName.endsWith(ZIP_EXTENSION);
                })
                .map(f -> {
                    IngestWorkflow ingestWorkflow = new IngestWorkflow();
                    ingestWorkflow.setProcessingState(IngestWorkflowState.NEW);
                    ingestWorkflow.setFileName(f.getName());
                    ingestWorkflow.setInitialConfig(initialIngestWorkflowConfig);

                    Path file = f.toPath();
                    Path pathToChecksumFile = getSipSumsTransferAreaPath(file);
                    try {
                        Hash hash = extractHash(pathToChecksumFile);
                        hashStore.save(hash);
                        ingestWorkflow.setHash(hash);
                    } catch (IOException e) {
                        throw new GeneralException("File with checksum for file with name " + f.getName() + " is inaccessible.", e);
                    }

                    ingestWorkflowService.save(ingestWorkflow);
                    return ingestWorkflow;
                })
                .collect(Collectors.toList());
        if (ingestWorkflows.size() > 0) {
            log.info("Processing of a batch of {} SIP packages at path {} has been triggered.", ingestWorkflows.size(), folder.toPath().toString());
        }
        return ingestWorkflows;
    }

    /**
     * Configure batch and ingest workflows
     * 1. creates a batch from the ingest workflows
     * 2. assigns it with the <code>producerProfile</code>, <code>batchWorkflowConfig</code> and sets its state to PROCESSING
     * 3. deploys the <code>workflowDefinition</code> from <code>producerProfile</code> to the repository of process definitions
     * and sets its id with the id of the batch
     *
     * @param ingestWorkflows        ingest workflows to be processed
     * @param producerProfile        profile of the producer
     * @param batchWorkflowConfig    init JSON configuration of the batch
     * @param computedWorkflowConfig result of batch config with producer profile config merge
     * @param transferAreaPath       path to the transfer area
     * @param userId                 id of the user
     * @return created batch
     */
    private Batch prepareBatch(List<IngestWorkflow> ingestWorkflows, ProducerProfile producerProfile,
                               String batchWorkflowConfig, String computedWorkflowConfig, Path transferAreaPath, String userId, IngestRoutine ingestRoutine) {
        WorkflowDefinition workflowDefinition = producerProfile.getWorkflowDefinition();
        notNull(workflowDefinition, () -> new IllegalArgumentException("null workflowDefinition of producer profile with id " + producerProfile.getId()));

        String bpmnDefinition = workflowDefinition.getBpmnDefinition();
        notNull(bpmnDefinition, () -> new IllegalArgumentException("null bpmnDefinition of producer profile with id " + producerProfile.getId()));

        Batch batch = new Batch(ingestWorkflows, BatchState.PROCESSING, producerProfile, batchWorkflowConfig,
                computedWorkflowConfig, transferAreaPath.toString(), producerProfile.isDebuggingModeActive(),
                true, false, producerProfile.getValidationProfile(),
                producerProfile.getSipProfile(), producerProfile.getWorkflowDefinition(), ingestRoutine);

        try {
            String bpmnString = ArclibUtils.prepareBpmnDefinitionForDeployment(bpmnDefinition, batch.getId());
            repositoryService.createDeployment().addInputStream(batch.getId() + ".bpmn",
                    new ByteArrayInputStream(bpmnString.getBytes())).name(batch.getId()).deploy();
        } catch (Exception e) {
            log.error("problem during parsing bpmn");
            throw new GeneralException(e);
        }
        batchService.create(batch, userId);
        log.info("New Batch with id " + batch.getId() + " created. The batch state is set to PROCESSING.");

        ingestWorkflows.forEach(ingestWorkflow -> {
            ingestWorkflow.setBatch(batch);
            ingestWorkflowService.save(ingestWorkflow);
            log.debug("Ingest workflow with external id " + ingestWorkflow.getExternalId() + " was assigned batch " + batch.getId() + ".");
        });
        return batch;
    }

    /**
     * Extract hash from a file.
     * <p>
     * The file must correspond the following format:
     * 1. first the type of the hash is specified {@link HashType}
     * 2. after a space delimiter follows the value of the hash
     *
     * @param pathToChecksumFile path to the file storing the hash
     * @return extracted hash together with its type
     */
    private Hash extractHash(Path pathToChecksumFile) throws IOException {
        byte[] bytes = Files.readAllBytes(pathToChecksumFile);
        String fileContent = new String(bytes, StandardCharsets.UTF_8);

        if (fileContent.isEmpty()) {
            throw new GeneralException("File at path " + pathToChecksumFile + " with the hash is empty.");
        }
        Hash hash = new Hash();

        String hashType = fileContent.substring(0, fileContent.indexOf(" "));
        hash.setHashType(HashType.valueOf(hashType));

        String hashValue = fileContent.substring(fileContent.indexOf(" ") + 1, fileContent.length());
        //strip spaces, tabulations and new lines
        hashValue = hashValue.replaceAll("\\s+$", "");
        hash.setHashValue(hashValue);
        log.debug("Extracted hash of type " + hashType + " and value " + hashValue + ".");
        return hash;
    }

    /**
     * Computes transfer area path from the batchTransferAreaPath and the transferAreaPath of the producer.
     * a) If the provided <code>batchTransferAreaPath</code> is empty, it returns a path composed from the path to
     * file storage and transfer area path of the producer.
     * <p>
     * b) Otherwise it returns a path composed from the path path to file storage, transfer area path of the producer
     * and <code>batchTransferAreaPath</code>.
     *
     * @return computed transfer area path
     */
    private Path computeTransferAreaPath(String batchTransferAreaPath, Producer producer) {
        String producerTransferAreaPath = producer.getTransferAreaPath();
        notNull(producerTransferAreaPath, () -> new IllegalArgumentException("null fullTransferAreaPath to transfer area in producer with id " + producer.getId()));
        Path fullTransferAreaPath = Paths.get(fileStorage, producerTransferAreaPath);
        return batchTransferAreaPath != null ? fullTransferAreaPath.resolve(batchTransferAreaPath) : fullTransferAreaPath;
    }

    /**
     * Computes resulting ingest workflow config by merging producer and batch ingest configs.
     * If there are two equally named attributes the batch config has the priority over the producer config.
     *
     * @param batchIngestConfig ingest config provided with the batch
     * @param producerProfile   producer profile
     * @return computed ingest config
     */
    private String computeIngestWorkflowConfig(String batchIngestConfig, ProducerProfile producerProfile) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        String producerIngestConfig = producerProfile.getWorkflowConfig();
        if (producerIngestConfig == null || producerIngestConfig.isBlank())
            producerIngestConfig = "{}";
        JsonNode ingestConfig = mapper.readTree(producerIngestConfig);
        ((ObjectNode) ingestConfig).set(ArclibXmlExtractorDelegate.SIP_PROFILE_CONFIG_ENTRY, new TextNode(producerProfile.getSipProfile().getExternalId()));
        ((ObjectNode) ingestConfig).set(ValidatorDelegate.VALIDATION_PROFILE_CONFIG_ENTRY, new TextNode(producerProfile.getValidationProfile().getExternalId()));

        log.trace("Producer ingest workflow config: " + ingestConfig.toString());

        if (batchIngestConfig != null) {
            log.trace("Batch ingest workflow config: " + batchIngestConfig);
            JsonNode batchIngestConfigJson = mapper.readTree(batchIngestConfig);
            ingestConfig = JsonUtils.merge(ingestConfig, batchIngestConfigJson);
        } else {
            log.trace("Batch ingest workflow is empty");
        }
        return mapper.writeValueAsString(ingestConfig);
    }

    public void verifyProducer(Producer producer, String exceptionMessage) {
        if (!hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE)) {
            eq(producer.getId(), userDetails.getUser().getProducer().getId(), () -> new ForbiddenOperation(exceptionMessage));
        }
    }

    @Inject
    public void setProducerProfileService(ProducerProfileService producerProfileService) {
        this.producerProfileService = producerProfileService;
    }

    @Inject
    public void setTemplate(JmsTemplate template) {
        this.template = template;
    }

    @Inject
    public void setIngestWorkflowService(IngestWorkflowService ingestWorkflowService) {
        this.ingestWorkflowService = ingestWorkflowService;
    }

    @Inject
    public void setBatchService(BatchService batchService) {
        this.batchService = batchService;
    }

    @Inject
    public void setRepositoryService(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @Inject
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }

    @Inject
    public void setHashStore(HashStore hashStore) {
        this.hashStore = hashStore;
    }

    @Inject
    public void setArclibMailCenter(ArclibMailCenter arclibMailCenter) {
        this.arclibMailCenter = arclibMailCenter;
    }

    @Inject
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Inject
    public void setIngestErrorHandler(IngestErrorHandler ingestErrorHandler) {
        this.ingestErrorHandler = ingestErrorHandler;
    }

    @Inject
    public void setRuntimeService(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Inject
    public void setFileStorage(@Value("${arclib.path.fileStorage}") String fileStorage) {
        this.fileStorage = fileStorage;
    }

    @Inject
    public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    @Inject
    public void setIngestRoutineStore(IngestRoutineStore ingestRoutineStore) {
        this.ingestRoutineStore = ingestRoutineStore;
    }
}
