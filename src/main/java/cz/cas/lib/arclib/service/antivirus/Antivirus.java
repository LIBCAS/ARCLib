package cz.cas.lib.arclib.service.antivirus;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cas.lib.arclib.bpm.AntivirusDelegate;
import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestIssue;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import cz.cas.lib.arclib.store.IngestIssueStore;
import cz.cas.lib.arclib.store.IngestWorkflowStore;
import cz.cas.lib.core.exception.GeneralException;
import cz.cas.lib.core.store.Transactional;
import org.apache.commons.io.FileUtils;
import org.camunda.bpm.engine.delegate.BpmnError;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static cz.cas.lib.arclib.utils.ArclibUtils.parseEnumFromConfig;

/**
 * Antivirus interface.
 */
public abstract class Antivirus {

    private IngestIssueStore ingestIssueStore;
    private IngestWorkflowStore ingestWorkflowStore;
    private Path quarantinePath;

    /**
     * Scans SIP package for viruses.
     *
     * @param sipPath    absolute path to SIP
     * @param externalId external id
     * @param configRoot config
     * @throws FileNotFoundException if sip is not found
     */
    public abstract void scan(Path sipPath, String externalId, JsonNode configRoot) throws FileNotFoundException, IncidentException;

    @Transactional
    public void invokeInfectedFilesIssue(List<Path> infectedFiles, String externalId, JsonNode configRoot,
                                         Path pathToSip) throws IncidentException {
        InfectedSipAction infectedSipAction = parseEnumFromConfig(configRoot,
                AntivirusDelegate.CONFIG_INFECTED_SIP_ACTION, InfectedSipAction.class);

        IngestWorkflow ingestWorkflow = ingestWorkflowStore.findByExternalId(externalId);

        String issueMessage = failureMessage(ingestWorkflow.getSip().getId(), infectedFiles, infectedSipAction);

        IngestIssue issue = new IngestIssue(
                ingestWorkflow,
                BpmConstants.VirusCheck.class, issueMessage
        );
        switch (infectedSipAction) {
            case IGNORE:
                issue.setSolvedByConfig(true);
                ingestIssueStore.save(issue);
                break;
            case QUARANTINE:
                issue.setSolvedByConfig(false);
                ingestIssueStore.save(issue);

                Path ingestWorkflowQuarantinePath = quarantinePath.resolve(externalId);
                try {
                    if (!Files.exists(ingestWorkflowQuarantinePath)) {
                        Files.createDirectories(ingestWorkflowQuarantinePath);
                    }
                    FileUtils.moveToDirectory(pathToSip.toFile(), ingestWorkflowQuarantinePath.toFile(), false);
                } catch (IOException e) {
                    throw new GeneralException("can't move sip " + pathToSip + " to quarantine " + ingestWorkflowQuarantinePath, e);
                }
                throw new BpmnError(BpmConstants.ErrorCodes.ProcessFailure, issue.toString());
            case CANCEL:
                issue.setSolvedByConfig(true);
                ingestIssueStore.save(issue);
                throw new BpmnError(BpmConstants.ErrorCodes.ProcessFailure, issue.toString());
        }
    }

    public abstract String getToolVersion();

    private String failureMessage(String sipId, List<Path> infectedFiles, InfectedSipAction action) {
        return "Antivirus scan on SIP with id: " + sipId + " has found infected files: " +
                Arrays.toString(infectedFiles.toArray()) + " solving with action: " + action;
    }

    public void setIngestWorkflowStore(IngestWorkflowStore ingestWorkflowStore) {
        this.ingestWorkflowStore = ingestWorkflowStore;
    }

    public void setIngestIssueStore(IngestIssueStore ingestIssueStore) {
        this.ingestIssueStore = ingestIssueStore;
    }

    public void setQuarantinePath(Path quarantinePath) {
        this.quarantinePath = quarantinePath;
    }
}
