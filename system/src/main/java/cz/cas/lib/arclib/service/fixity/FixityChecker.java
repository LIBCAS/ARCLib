package cz.cas.lib.arclib.service.fixity;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.bpm.FixityCheckerDelegate;
import cz.cas.lib.arclib.bpm.IngestTool;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestIssue;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.preservationPlanning.IngestIssueDefinitionCode;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import cz.cas.lib.arclib.formatlibrary.domain.FormatDefinition;
import cz.cas.lib.arclib.formatlibrary.service.FormatDefinitionService;
import cz.cas.lib.arclib.service.IngestIssueService;
import cz.cas.lib.arclib.service.preservationPlanning.ToolService;
import cz.cas.lib.arclib.store.IngestIssueDefinitionStore;
import cz.cas.lib.arclib.store.IngestWorkflowStore;
import cz.cas.lib.arclib.utils.ArclibUtils;
import cz.cas.lib.core.store.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

@Slf4j
@Service
public abstract class FixityChecker {

    FixityCounterFacade fixityCounterFacade;
    private IngestIssueService ingestIssueService;
    private IngestWorkflowStore ingestWorkflowStore;
    private ToolService toolService;
    private IngestIssueDefinitionStore ingestIssueDefinitionStore;
    private FormatDefinitionService formatDefinitionService;

    /**
     * Verifies fixity of every file specified in metadata file(s) of the package.
     * The logic of implementing class is tied with {@link FixityCheckMethod}
     * Currently supports MD5, SHA-1, SHA-256 and SHA-512.
     * May invoke three types of issue in following order:
     * <ol>
     * <li>unsupported checksum type: description contains {@link Map<String,List<Path>} with checksum type and
     * corresponding files (even if the file does not exist)</li>
     * <li>files not found: description contains {@link List<Path>} of files which does not exist</li>
     * <li>invalid checksums: description contains {@link List<Path>} of files with invalid fixites</li>
     * </ol>
     * The first issue which is not automatically solved by config stops process and invokes new {@link IncidentException}
     *
     * @param sipWsPath        path to SIP in workspace
     * @param pathToFixityFile Path to a file which contains fixity information of files of the package.
     * @param externalId       external id of the ingest workflow, used in case of issue
     * @param configRoot       root node of the ingest workflow JSON config containing configuration of the behaviour
     *                         for a case of a fixity error
     */
    public abstract void verifySIP(Path sipWsPath, Path pathToFixityFile,
                                   String externalId, JsonNode configRoot,
                                   Map<String, Pair<String, String>> formatIdentificationResult,
                                   int fixityToolCheckCounter,
                                   IngestTool fixityCheckerTool)
            throws IOException, IncidentException;

    @Transactional
    public void invokeUnsupportedChecksumTypeIssue(Path pathToSip, Map<Path, Map<String, List<Path>>> files, String externalId,
                                                   JsonNode configRoot, Map<String, Pair<String, String>> formatIdentificationResult,
                                                   int fixityCheckToolCounter, IngestTool fixityCheckerTool)
            throws IncidentException {
        log.warn("Invoked unsupported checksum type issue for ingest workflow " + externalId + " .");
        IngestWorkflow ingestWorkflow = ingestWorkflowStore.findByExternalId(externalId);
        Pair<Boolean, String> parsedConfigValue = ArclibUtils.parseBooleanConfig(configRoot,
                FixityCheckerDelegate.FIXITY_CHECK_TOOL + "/" + fixityCheckToolCounter + FixityCheckerDelegate.CONFIG_UNSUPPORTED_CHECKSUM_TYPE);
        List<IngestIssue> issues = new ArrayList<>();

        List<Path> filesWithUnsupportedChecksums = new ArrayList<>();
        for (Path checksumSource : files.keySet()) {
            for (String algorithm : files.get(checksumSource).keySet()) {
                for (Path filePath : files.get(checksumSource).get(algorithm)) {
                    log.info("unsupported checksum algorithm: " + algorithm + " used for file: " + filePath);
                    Pair<String, FormatDefinition> fileFormat = ArclibUtils.findFormat(pathToSip, filePath,
                            formatIdentificationResult, formatDefinitionService);
                    issues.add(new IngestIssue(
                            ingestWorkflow,
                            toolService.getByNameAndVersion(fixityCheckerTool.getToolName(), fixityCheckerTool.getToolVersion()),
                            ingestIssueDefinitionStore.findByCode(IngestIssueDefinitionCode.FILE_UNSUPPORTED_CHECKSUM_TYPE),
                            fileFormat.getRight(),
                            "File: " + pathToSip.toAbsolutePath().relativize(checksumSource.toAbsolutePath()) + " contains unsupported checksum algorithm: " + algorithm + " used for file: " + fileFormat.getLeft() +
                                    ". " + parsedConfigValue.getRight(),
                            parsedConfigValue.getLeft() != null)
                    );
                    filesWithUnsupportedChecksums.add(filePath);
                }
            }
        }

        String errorMsg = IngestIssueDefinitionCode.FILE_UNSUPPORTED_CHECKSUM_TYPE +
                " issue occurred, unsupported checksum types: " + Arrays.toString(files.keySet().toArray()) +
                " files: " + Arrays.toString(filesWithUnsupportedChecksums.toArray());

        if (parsedConfigValue.getLeft() == null)
            throw new IncidentException(issues);
        ingestIssueService.save(issues);
        if (!parsedConfigValue.getLeft())
            throw new BpmnError(BpmConstants.ErrorCodes.ProcessFailure, ArclibUtils.trimBpmnErrorMsg(errorMsg));
    }

    @Transactional
    public void invokeInvalidChecksumsIssue(Path pathToSip, Map<Path, List<Path>> files, String externalId, JsonNode configRoot,
                                            Map<String, Pair<String, String>> formatIdentificationResult,
                                            int fixityCheckToolCounter, IngestTool fixityCheckerTool)
            throws IncidentException {
        log.warn("Invoked invalid checksums issue for ingest workflow " + externalId + " .");
        IngestWorkflow ingestWorkflow = ingestWorkflowStore.findByExternalId(externalId);
        Pair<Boolean, String> parsedConfigValue = ArclibUtils.parseBooleanConfig(configRoot,
                FixityCheckerDelegate.FIXITY_CHECK_TOOL + "/" + fixityCheckToolCounter + FixityCheckerDelegate.CONFIG_INVALID_CHECKSUMS);
        List<IngestIssue> issues = new ArrayList<>();

        for (Path checksumSource : files.keySet()) {
            for (Path filePath : files.get(checksumSource)) {
                log.info("invalid checksum of file: " + filePath);
                Pair<String, FormatDefinition> fileFormat = ArclibUtils.findFormat(pathToSip, filePath,
                        formatIdentificationResult, formatDefinitionService);
                issues.add(new IngestIssue(
                        ingestWorkflow,
                        toolService.getByNameAndVersion(fixityCheckerTool.getToolName(), fixityCheckerTool.getToolVersion()),
                        ingestIssueDefinitionStore.findByCode(IngestIssueDefinitionCode.FILE_INVALID_CHECKSUM),
                        fileFormat.getRight(),
                        "File: " + pathToSip.toAbsolutePath().relativize(checksumSource.toAbsolutePath()) + " contains invalid checksum of file: " + fileFormat.getLeft() + ". " + parsedConfigValue.getRight(),
                        parsedConfigValue.getLeft() != null)
                );
            }
        }

        String errorMsg = IngestIssueDefinitionCode.FILE_INVALID_CHECKSUM + " issue occurred, files: " + Arrays.toString(files.values().stream().flatMap(Collection::stream).toArray());

        if (parsedConfigValue.getLeft() == null)
            throw new IncidentException(issues);
        ingestIssueService.save(issues);
        if (!parsedConfigValue.getLeft())
            throw new BpmnError(BpmConstants.ErrorCodes.ProcessFailure, ArclibUtils.trimBpmnErrorMsg(errorMsg));
    }

    @Transactional
    public void invokeMissingFilesIssue(Path pathToSip, Map<Path, List<Path>> files, String externalId, JsonNode configRoot,
                                        int fixityCheckToolCounter, IngestTool fixityCheckerTool)
            throws IncidentException {
        log.warn("Invoked missing files issue for ingest workflow " + externalId + " .");
        IngestWorkflow ingestWorkflow = ingestWorkflowStore.findByExternalId(externalId);
        Pair<Boolean, String> parsedConfigValue = ArclibUtils.parseBooleanConfig(configRoot, FixityCheckerDelegate.FIXITY_CHECK_TOOL + "/" + fixityCheckToolCounter + FixityCheckerDelegate.CONFIG_MISSING_FILES);
        List<IngestIssue> issues = new ArrayList<>();

        for (Path checksumSource : files.keySet()) {
            for (Path filePath : files.get(checksumSource)) {
                log.info("missing file: " + filePath);
                String pathToSipStr = pathToSip.toAbsolutePath().toString().replace("\\", "/");
                String fileRelativePath = filePath.toAbsolutePath().toString().replace("\\", "/").replace(pathToSipStr + "/", "");
                issues.add(new IngestIssue(
                        ingestWorkflow,
                        toolService.getByNameAndVersion(fixityCheckerTool.getToolName(), fixityCheckerTool.getToolVersion()),
                        ingestIssueDefinitionStore.findByCode(IngestIssueDefinitionCode.FILE_MISSING),
                        null,
                        "File: " + pathToSip.toAbsolutePath().relativize(checksumSource.toAbsolutePath()) + " references missing file: " + fileRelativePath + ". " + parsedConfigValue.getRight(),
                        parsedConfigValue.getLeft() != null)
                );
            }
        }
        String errorMsg = IngestIssueDefinitionCode.FILE_MISSING + " issue occurred, files: " + Arrays.toString(files.values().stream().flatMap(Collection::stream).toArray());

        if (parsedConfigValue.getLeft() == null)
            throw new IncidentException(issues);
        ingestIssueService.save(issues);
        if (!parsedConfigValue.getLeft())
            throw new BpmnError(BpmConstants.ErrorCodes.ProcessFailure, ArclibUtils.trimBpmnErrorMsg(errorMsg));
    }

    @Autowired
    public void setIngestWorkflowStore(IngestWorkflowStore ingestWorkflowStore) {
        this.ingestWorkflowStore = ingestWorkflowStore;
    }

    @Autowired
    public void setIngestIssueService(IngestIssueService ingestIssueService) {
        this.ingestIssueService = ingestIssueService;
    }

    @Autowired
    public void setFormatDefinitionService(FormatDefinitionService formatDefinitionService) {
        this.formatDefinitionService = formatDefinitionService;
    }

    @Autowired
    public void setToolService(ToolService toolService) {
        this.toolService = toolService;
    }

    @Autowired
    public void setIngestIssueDefinitionStore(IngestIssueDefinitionStore ingestIssueDefinitionStore) {
        this.ingestIssueDefinitionStore = ingestIssueDefinitionStore;
    }

    @Autowired
    public void setFixityCounterFacade(FixityCounterFacade fixityCounterFacade) {
        this.fixityCounterFacade = fixityCounterFacade;
    }
}
