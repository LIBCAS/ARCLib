package cz.cas.lib.arclib.bpm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.arclib.service.IngestIssueService;
import cz.cas.lib.arclib.service.preservationPlanning.FormatDefinitionService;
import cz.cas.lib.arclib.service.preservationPlanning.ToolService;
import cz.cas.lib.arclib.store.IngestEventStore;
import cz.cas.lib.arclib.store.IngestIssueDefinitionStore;
import cz.cas.lib.arclib.store.IngestWorkflowStore;
import cz.cas.lib.core.exception.GeneralException;
import cz.cas.lib.core.util.Utils;
import lombok.Getter;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.TreeMap;

import static cz.cas.lib.arclib.utils.ArclibUtils.getSipZipWorkspacePath;

@Service
public abstract class ArclibDelegate implements VariableMapper, IngestTool {

    protected ObjectMapper objectMapper;
    protected IngestIssueService ingestIssueService;
    protected ToolService toolService;
    protected IngestIssueDefinitionStore ingestIssueDefinitionStore;
    protected IngestWorkflowStore ingestWorkflowStore;
    protected String workspace;
    protected FormatDefinitionService formatDefinitionService;
    protected IngestEventStore ingestEventStore;
    @Getter
    private String toolVersion = null;

    /**
     * Gets {@link JsonNode} representation of the JSON config
     *
     * @param execution delegate execution containing BPM variables
     * @return {@link JsonNode} representation of the JSON config
     */
    public JsonNode getConfigRoot(DelegateExecution execution) {
        try {
            return objectMapper.readTree(getLatestConfig(execution));
        } catch (IOException ex) {
            throw new GeneralException("error reading ingest workflow config ", ex);
        }
    }

    /**
     * Gets external id of the ingest workflow.
     *
     * @param execution delegate execution containing BPM variables
     * @return external id of the ingest workflow
     */
    public String getIngestWorkflowExternalId(DelegateExecution execution) {
        return getStringVariable(execution, BpmConstants.ProcessVariables.ingestWorkflowExternalId);
    }
    public String getProducerProfileExternalId(DelegateExecution execution) {
        return getStringVariable(execution, BpmConstants.ProcessVariables.producerProfileExternalId);
    }

    public boolean isInDebugMode(DelegateExecution execution) {
        return getBooleanVariable(execution, BpmConstants.ProcessVariables.debuggingModeActive);
    }

    /**
     * Gets result map of format identifier.
     *
     * @param execution delegate execution containing BPM variables
     * @return map of path to file and its format
     */
    public TreeMap<String, Utils.Pair<String, String>> getFormatIdentificationResult(DelegateExecution execution) {
        return (TreeMap<String, Utils.Pair<String, String>>) execution.getVariable(BpmConstants.FormatIdentification.mapOfFilesToFormats);
    }

    /**
     * Gets id of the batch.
     *
     * @param execution delegate execution containing BPM variables
     * @return id of the batch
     */
    public String getBatchId(DelegateExecution execution) {
        return getStringVariable(execution, BpmConstants.ProcessVariables.batchId);
    }

    /**
     * Gets responsiblePerson.
     *
     * @param execution delegate execution containing BPM variables
     * @return responsiblePerson
     */
    public String getResponsiblePerson(DelegateExecution execution) {
        return getStringVariable(execution, BpmConstants.ProcessVariables.responsiblePerson);
    }

    /**
     * Gets path to the zip file with the SIP content stored at the workspace.
     *
     * @param execution delegate execution containing BPM variables
     * @return path to the zip with SIP content
     */
    public Path getSipZipPath(DelegateExecution execution) {
        String sipFileName = getStringVariable(execution, BpmConstants.Ingestion.sipFileName);
        return getSipZipWorkspacePath(getIngestWorkflowExternalId(execution), workspace, sipFileName);
    }

    /**
     * Gets the latest JSON config.
     * If there were changes of the config performed during ingest workflow execution, this is the most recent version.
     *
     * @param execution delegate execution containing BPM variables
     * @return latest JSON config
     */
    public String getLatestConfig(DelegateExecution execution) {
        return getStringVariable(execution, BpmConstants.ProcessVariables.latestConfig);
    }

    @Inject
    public void setIngestIssueService(IngestIssueService ingestIssueService) {
        this.ingestIssueService = ingestIssueService;
    }

    @Inject
    public void setToolService(ToolService toolService) {
        this.toolService = toolService;
    }

    @Inject
    public void setIngestIssueDefinitionStore(IngestIssueDefinitionStore ingestIssueDefinitionStore) {
        this.ingestIssueDefinitionStore = ingestIssueDefinitionStore;
    }

    @Inject
    public void setWorkspace(@Value("${arclib.path.workspace}") String workspace) {
        this.workspace = workspace;
    }

    @Inject
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Inject
    public void setIngestWorkflowStore(IngestWorkflowStore ingestWorkflowStore) {
        this.ingestWorkflowStore = ingestWorkflowStore;
    }

    @Inject
    public void setFormatDefinitionService(FormatDefinitionService formatDefinitionService) {
        this.formatDefinitionService = formatDefinitionService;
    }

    @Inject
    public void setIngestEventStore(IngestEventStore ingestEventStore) {
        this.ingestEventStore = ingestEventStore;
    }
}
