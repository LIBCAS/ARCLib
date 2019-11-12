package cz.cas.lib.arclib.service.antivirus;

import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.bpm.IngestTool;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestIssue;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.preservationPlanning.IngestIssueDefinitionCode;
import cz.cas.lib.arclib.domain.preservationPlanning.Tool;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import cz.cas.lib.arclib.formatlibrary.domain.FormatDefinition;
import cz.cas.lib.arclib.formatlibrary.service.FormatDefinitionService;
import cz.cas.lib.arclib.service.IngestIssueService;
import cz.cas.lib.arclib.store.IngestIssueDefinitionStore;
import cz.cas.lib.arclib.utils.ArclibUtils;
import cz.cas.lib.core.store.Transactional;
import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.camunda.bpm.engine.delegate.BpmnError;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Antivirus interface.
 */
public abstract class Antivirus implements IngestTool {

    private IngestIssueService ingestIssueService;
    private Path quarantinePath;
    private FormatDefinitionService formatDefinitionService;
    @Getter
    private Tool toolEntity;
    private IngestIssueDefinitionStore ingestIssueDefinitionStore;
    private InfectedSipAction infectedSipAction;
    private Map<String, Pair<String, String>> formatIdentificationResult;

    /**
     * Scans SIP package for viruses.
     *
     * @param sipPath absolute path to SIP
     * @param iw      ingest workflow
     * @throws FileNotFoundException if sip is not found
     */
    public abstract void scan(Path sipPath, IngestWorkflow iw) throws FileNotFoundException, IncidentException;

    @Transactional
    public void invokeInfectedFilesIssue(List<Path> infectedFiles, IngestWorkflow ingestWorkflow, Path pathToSip) {
        String failureMessage = failureMessage(ingestWorkflow.getSip().getId(), infectedFiles, infectedSipAction);
        List<IngestIssue> issues = new ArrayList<>();

        for (Path infectedFile : infectedFiles) {
            Pair<String, FormatDefinition> formatPair = ArclibUtils.findFormat(pathToSip, infectedFile, formatIdentificationResult, formatDefinitionService);
            issues.add(new IngestIssue(
                    ingestWorkflow,
                    toolEntity,
                    ingestIssueDefinitionStore.findByCode(IngestIssueDefinitionCode.FILE_VIRUS_FOUND),
                    formatPair.getRight(),
                    "infected file: " + formatPair.getLeft() + " of SIP: " + ingestWorkflow.getSip().getId(),
                    true
            ));
        }
        switch (infectedSipAction) {
            case IGNORE:
                ingestIssueService.save(issues);
                break;
            case QUARANTINE:
                for (IngestIssue issue : issues) {
                    issue.setSuccess(false);
                }
                ingestIssueService.save(issues);
                Path ingestWorkflowQuarantinePath = quarantinePath.resolve(ingestWorkflow.getExternalId());
                try {
                    if (!Files.exists(ingestWorkflowQuarantinePath)) {
                        Files.createDirectories(ingestWorkflowQuarantinePath);
                    }
                    FileUtils.moveToDirectory(pathToSip.toFile(), ingestWorkflowQuarantinePath.toFile(), false);
                } catch (IOException e) {
                    throw new GeneralException("can't move sip " + pathToSip + " to quarantine " + ingestWorkflowQuarantinePath, e);
                }
                throw new BpmnError(BpmConstants.ErrorCodes.ProcessFailure, failureMessage);
            case CANCEL:
                ingestIssueService.save(issues);
                throw new BpmnError(BpmConstants.ErrorCodes.ProcessFailure, failureMessage);
        }
    }

    private String failureMessage(String sipId, List<Path> infectedFiles, InfectedSipAction action) {
        return "Antivirus scan on SIP with id: " + sipId + " has found infected files: " +
                Arrays.toString(infectedFiles.toArray()) + " solving with action: " + action;
    }

    public void inject(FormatDefinitionService formatDefinitionService,
                       IngestIssueService ingestIssueService,
                       Tool tool,
                       IngestIssueDefinitionStore ingestIssueDefinitionStore,
                       Path quarantinePath,
                       InfectedSipAction infectedSipAction,
                       Map<String, Pair<String, String>> formatIdentificationResult) {
        this.quarantinePath = quarantinePath;
        this.ingestIssueService = ingestIssueService;
        this.formatDefinitionService = formatDefinitionService;
        this.toolEntity = tool;
        this.ingestIssueDefinitionStore = ingestIssueDefinitionStore;
        this.infectedSipAction = infectedSipAction;
        this.formatIdentificationResult = formatIdentificationResult;
    }
}
