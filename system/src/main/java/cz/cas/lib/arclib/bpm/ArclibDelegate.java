package cz.cas.lib.arclib.bpm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.arclib.service.IngestIssueService;
import cz.cas.lib.arclib.service.preservationPlanning.ToolService;
import cz.cas.lib.arclib.store.IngestEventStore;
import cz.cas.lib.arclib.store.IngestIssueDefinitionStore;
import cz.cas.lib.arclib.store.IngestWorkflowStore;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.core.store.TransactionalNew;
import cz.cas.lib.core.util.Utils;
import cz.cas.lib.arclib.formatlibrary.service.FormatDefinitionService;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.TreeMap;

import static cz.cas.lib.arclib.utils.ArclibUtils.getSipZipWorkspacePath;
import static cz.cas.lib.core.util.Utils.notNull;

@Service
public abstract class ArclibDelegate implements VariableMapper, IngestTool, JavaDelegate {

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

    public abstract void executeArclibDelegate(DelegateExecution execution) throws Exception;

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
     * wrapper around {@link JavaDelegate#execute(DelegateExecution)} to make sure that the code to be executed runs in own transactional context
     *
     * @param execution
     * @throws IOException
     */
    @TransactionalNew
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        executeArclibDelegate(execution);
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
     * @return map of paths to file and its format if the preferred format identification has been performed,
     * <code>null</code> otherwise
     */
    public TreeMap<String, Pair<String, String>> getFormatIdentificationResult(DelegateExecution execution) {
        HashMap<String, TreeMap<String, Pair<String, String>>> mapOfEventIdsToMapsOfFilesToFormats =
                (HashMap<String, TreeMap<String, Pair<String, String>>>)
                        execution.getVariable(BpmConstants.FormatIdentification.mapOfEventIdsToMapsOfFilesToFormats);

        String preferredFormatIdentificationEventId = (String) execution.getVariable(BpmConstants.FormatIdentification.preferredFormatIdentificationEventId);
        notNull(preferredFormatIdentificationEventId, () -> new GeneralException("Failed to retrieve map of files to formats because format identification has not been performed."));

        return mapOfEventIdsToMapsOfFilesToFormats.get(preferredFormatIdentificationEventId);
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
