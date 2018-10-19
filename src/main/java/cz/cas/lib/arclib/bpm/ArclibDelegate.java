package cz.cas.lib.arclib.bpm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.arclib.utils.ArclibUtils;
import cz.cas.lib.core.exception.GeneralException;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;

import static cz.cas.lib.arclib.utils.ArclibUtils.getSipZipWorkspacePath;

@Service
public abstract class ArclibDelegate implements VariableMapper {

    private ObjectMapper objectMapper;
    protected String workspace;

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
     * Gets assignee.
     *
     * @param execution delegate execution containing BPM variables
     * @return assignee
     */
    public String getAssignee(DelegateExecution execution) {
        return getStringVariable(execution, BpmConstants.ProcessVariables.assignee);
    }

    /**
     * Gets path to the zip file with the SIP content stored at the workspace.
     *
     * @param execution delegate execution containing BPM variables
     * @return path to the zip with SIP content
     */
    public Path getSipZipPath(DelegateExecution execution) {
        String originalSipFileName = getStringVariable(execution, BpmConstants.Ingestion.originalSipFileName);
        return getSipZipWorkspacePath(getIngestWorkflowExternalId(execution), workspace, originalSipFileName
                + ArclibUtils.ZIP_EXTENSION);
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
    public void setWorkspace(@Value("${arclib.path.workspace}") String workspace) {
        this.workspace = workspace;
    }

    @Inject
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
}
