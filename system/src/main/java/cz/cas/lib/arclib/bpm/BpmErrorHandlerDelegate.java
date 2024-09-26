package cz.cas.lib.arclib.bpm;

import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflowFailureInfo;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflowFailureType;
import cz.cas.lib.arclib.service.IngestErrorHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;



@Slf4j
@Service
public class BpmErrorHandlerDelegate extends ArclibDelegate {

    private IngestErrorHandler ingestErrorHandler;
    @Getter
    private String toolName = "ARCLib_error_handler";

    /**
     * Handles BPM errors using {@link IngestErrorHandler}
     *
     * @param execution delegate execution containing the BPM variables
     */
    @Override
    public void executeArclibDelegate(DelegateExecution execution) {
        String errorCode = getStringVariable(execution, BpmConstants.ProcessVariables.errorCode);
        String errorMessage = getStringVariable(execution, BpmConstants.ProcessVariables.errorMessage);
        IngestWorkflowFailureInfo failureInfo = new IngestWorkflowFailureInfo(
                errorCode + " : " + errorMessage, null, IngestWorkflowFailureType.BPM_ERROR
        );
        String responsiblePerson = getStringVariable(execution, BpmConstants.ProcessVariables.responsiblePerson);
        ingestErrorHandler.handleError(getIngestWorkflowExternalId(execution), failureInfo, execution.getProcessInstanceId(), responsiblePerson);
    }

    @Autowired
    public void setIngestErrorHandler(IngestErrorHandler ingestErrorHandler) {
        this.ingestErrorHandler = ingestErrorHandler;
    }
}

