package cz.cas.lib.arclib.service.fixity;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.bpm.FixityCheckerDelegate;
import cz.cas.lib.arclib.bpm.IngestTool;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestIssue;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.packages.PackageType;
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
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

@Slf4j
@Service
public abstract class FixityChecker implements IngestTool {

    Md5Counter md5Counter;
    Sha512Counter sha512Counter;
    Sha256Counter sha256Counter;
    Sha1Counter sha1Counter;
    private IngestIssueService ingestIssueService;
    private IngestWorkflowStore ingestWorkflowStore;
    private ToolService toolService;
    private IngestIssueDefinitionStore ingestIssueDefinitionStore;
    private FormatDefinitionService formatDefinitionService;
    private int fixityCheckToolCounter;

    /**
     * Verifies fixity of every file specified in metadata file(s) of the package.
     * The logic of implementing class is tied with {@link PackageType}
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
     * @param externalId      external id of the ingest workflow, used in case of issue
     * @param configRoot      root node of the ingest workflow JSON config containing configuration of the behaviour
     *                        for a case of a fixity error
     */
    public abstract void verifySIP(Path sipWsPath, Path pathToFixityFile,
                                   String externalId, JsonNode configRoot,
                                   Map<String, Pair<String, String>> formatIdentificationResult)
            throws IOException, IncidentException;

    @Transactional
    public void invokeUnsupportedChecksumTypeIssue(Path pathToSip, Map<String, List<Path>> files, String externalId,
                                                   JsonNode configRoot, Map<String, Pair<String, String>> formatIdentificationResult)
            throws IncidentException {
        log.warn("Invoked unsupported checksum type issue for ingest workflow " + externalId + " .");
        IngestWorkflow ingestWorkflow = ingestWorkflowStore.findByExternalId(externalId);
        Pair<Boolean, String> parsedConfigValue = ArclibUtils.parseBooleanConfig(configRoot,
                FixityCheckerDelegate.FIXITY_CHECK_TOOL + "/" + fixityCheckToolCounter + FixityCheckerDelegate.CONFIG_UNSUPPORTED_CHECKSUM_TYPE);
        List<IngestIssue> issues = new ArrayList<>();

        for (String algorithm : files.keySet()) {
            for (Path filePath : files.get(algorithm)) {
                log.info("unsupported checksum algorithm: " + algorithm + " used for file: " + filePath);
                Pair<String, FormatDefinition> fileFormat = ArclibUtils.findFormat(pathToSip, filePath,
                        formatIdentificationResult, formatDefinitionService);
                issues.add(new IngestIssue(
                        ingestWorkflow,
                        toolService.getByNameAndVersion(getToolName(), getToolVersion()),
                        ingestIssueDefinitionStore.findByCode(IngestIssueDefinitionCode.FILE_UNSUPPORTED_CHECKSUM_TYPE),
                        fileFormat.getRight(),
                        "unsupported checksum algorithm: " + algorithm + " used for file: " + fileFormat.getLeft() +
                                ". " + parsedConfigValue.getRight(),
                        parsedConfigValue.getLeft() != null)
                );
            }
        }

        Path[] problemFiles = files.values().stream().flatMap(Collection::stream).map(p->p.relativize(pathToSip)).toArray(Path[]::new);
        String errorMsg = IngestIssueDefinitionCode.FILE_UNSUPPORTED_CHECKSUM_TYPE +
                " issue occurred, unsupported checksum types: " + Arrays.toString(files.keySet().toArray()) +
                " files: " + Arrays.toString(problemFiles);

        if (parsedConfigValue.getLeft() == null)
            throw new IncidentException(issues);
        ingestIssueService.save(issues);
        if (!parsedConfigValue.getLeft())
            throw new BpmnError(BpmConstants.ErrorCodes.ProcessFailure, ArclibUtils.trimBpmnErrorMsg(errorMsg));
    }

    @Transactional
    public void invokeInvalidChecksumsIssue(Path pathToSip, List<Path> files, String externalId, JsonNode configRoot,
                                            Map<String, Pair<String, String>> formatIdentificationResult)
            throws IncidentException {
        log.warn("Invoked invalid checksums issue for ingest workflow " + externalId + " .");
        IngestWorkflow ingestWorkflow = ingestWorkflowStore.findByExternalId(externalId);
        Pair<Boolean, String> parsedConfigValue = ArclibUtils.parseBooleanConfig(configRoot,
                FixityCheckerDelegate.FIXITY_CHECK_TOOL + "/" + fixityCheckToolCounter + FixityCheckerDelegate.CONFIG_INVALID_CHECKSUMS);
        List<IngestIssue> issues = new ArrayList<>();

        List<String> fileRelativePaths = new ArrayList<>();
        for (Path filePath : files) {
            log.info("invalid checksum of file: " + filePath);
            Pair<String, FormatDefinition> fileFormat = ArclibUtils.findFormat(pathToSip, filePath,
                    formatIdentificationResult, formatDefinitionService);
            issues.add(new IngestIssue(
                    ingestWorkflow,
                    toolService.getByNameAndVersion(getToolName(), getToolVersion()),
                    ingestIssueDefinitionStore.findByCode(IngestIssueDefinitionCode.FILE_INVALID_CHECKSUM),
                    fileFormat.getRight(),
                    "invalid checksum of file: " + fileFormat.getLeft() + ". " + parsedConfigValue.getRight(),
                    parsedConfigValue.getLeft() != null)
            );
            fileRelativePaths.add(fileFormat.getLeft());
        }
        String errorMsg = IngestIssueDefinitionCode.FILE_INVALID_CHECKSUM + " issue occurred, files: " + Arrays.toString(fileRelativePaths.toArray());

        if (parsedConfigValue.getLeft() == null)
            throw new IncidentException(issues);
        ingestIssueService.save(issues);
        if (!parsedConfigValue.getLeft())
            throw new BpmnError(BpmConstants.ErrorCodes.ProcessFailure, ArclibUtils.trimBpmnErrorMsg(errorMsg));
    }

    @Transactional
    public void invokeMissingFilesIssue(Path pathToSip, List<Path> files, String externalId, JsonNode configRoot,
                                        Map<String, Pair<String, String>> formatIdentificationResult)
            throws IncidentException {
        log.warn("Invoked missing files issue for ingest workflow " + externalId + " .");
        IngestWorkflow ingestWorkflow = ingestWorkflowStore.findByExternalId(externalId);
        Pair<Boolean, String> parsedConfigValue = ArclibUtils.parseBooleanConfig(configRoot, FixityCheckerDelegate.FIXITY_CHECK_TOOL + "/" + fixityCheckToolCounter + FixityCheckerDelegate.CONFIG_MISSING_FILES);
        List<IngestIssue> issues = new ArrayList<>();

        List<String> fileRelativePaths = new ArrayList<>();
        for (Path filePath : files) {
            log.info("missing file: " + filePath);
            Pair<String, FormatDefinition> fileFormat = ArclibUtils.findFormat(pathToSip, filePath,
                    formatIdentificationResult, formatDefinitionService);
            issues.add(new IngestIssue(
                    ingestWorkflow,
                    toolService.getByNameAndVersion(getToolName(), getToolVersion()),
                    ingestIssueDefinitionStore.findByCode(IngestIssueDefinitionCode.FILE_MISSING),
                    fileFormat.getRight(),
                    "missing file: " + fileFormat.getLeft() + ". " + parsedConfigValue.getRight(),
                    parsedConfigValue.getLeft() != null)
            );
            fileRelativePaths.add(fileFormat.getLeft());
        }
        String errorMsg = IngestIssueDefinitionCode.FILE_MISSING + " issue occurred, files: " + Arrays.toString(fileRelativePaths.toArray());

        if (parsedConfigValue.getLeft() == null)
            throw new IncidentException(issues);
        ingestIssueService.save(issues);
        if (!parsedConfigValue.getLeft())
            throw new BpmnError(BpmConstants.ErrorCodes.ProcessFailure, ArclibUtils.trimBpmnErrorMsg(errorMsg));
    }

    public void setFixityCheckToolCounter(int fixityCheckToolCounter) {
        this.fixityCheckToolCounter = fixityCheckToolCounter;
    }

    @Inject
    public void setIngestWorkflowStore(IngestWorkflowStore ingestWorkflowStore) {
        this.ingestWorkflowStore = ingestWorkflowStore;
    }

    @Inject
    public void setMd5Counter(Md5Counter md5Counter) {
        this.md5Counter = md5Counter;
    }

    @Inject
    public void setSha512Counter(Sha512Counter sha512Counter) {
        this.sha512Counter = sha512Counter;
    }

    @Inject
    public void setSha256Counter(Sha256Counter sha256Counter) {
        this.sha256Counter = sha256Counter;
    }

    @Inject
    public void setSha1Counter(Sha1Counter sha1Counter) {
        this.sha1Counter = sha1Counter;
    }

    @Inject
    public void setIngestIssueService(IngestIssueService ingestIssueService) {
        this.ingestIssueService = ingestIssueService;
    }

    @Inject
    public void setFormatDefinitionService(FormatDefinitionService formatDefinitionService) {
        this.formatDefinitionService = formatDefinitionService;
    }

    @Inject
    public void setToolService(ToolService toolService) {
        this.toolService = toolService;
    }

    @Inject
    public void setIngestIssueDefinitionStore(IngestIssueDefinitionStore ingestIssueDefinitionStore) {
        this.ingestIssueDefinitionStore = ingestIssueDefinitionStore;
    }
}
