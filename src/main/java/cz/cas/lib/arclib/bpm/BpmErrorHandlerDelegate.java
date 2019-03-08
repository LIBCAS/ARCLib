package cz.cas.lib.arclib.bpm;

import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflowFailureInfo;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflowFailureType;
import cz.cas.lib.arclib.service.IngestErrorHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Slf4j
@Service
public class BpmErrorHandlerDelegate extends ArclibDelegate implements JavaDelegate {

    private IngestErrorHandler ingestErrorHandler;
    @Getter
    private String toolName = "ARCLib_error_handler";

    /**
     * Handles BPM errors using {@link IngestErrorHandler}
     *
     * @param execution delegate execution containing the BPM variables
     */
    @Override
    public void execute(DelegateExecution execution) {
        String externalId = getIngestWorkflowExternalId(execution);
        log.debug("Execution of Bpm error handler delegate started for ingest workflow with external id " + externalId + ".");

        String errorCode = getStringVariable(execution, BpmConstants.ProcessVariables.errorCode);
        String errorMessage = getStringVariable(execution, BpmConstants.ProcessVariables.errorMessage);
        IngestWorkflowFailureInfo failureInfo = new IngestWorkflowFailureInfo(
                errorCode + " : " + errorMessage, null, IngestWorkflowFailureType.BPM_ERROR
        );
        String responsiblePerson = getStringVariable(execution, BpmConstants.ProcessVariables.responsiblePerson);
        ingestErrorHandler.handleError(externalId, failureInfo, execution.getProcessInstanceId(), responsiblePerson);
    }

    @Inject
    public void setIngestErrorHandler(IngestErrorHandler ingestErrorHandler) {
        this.ingestErrorHandler = ingestErrorHandler;
    }
}

