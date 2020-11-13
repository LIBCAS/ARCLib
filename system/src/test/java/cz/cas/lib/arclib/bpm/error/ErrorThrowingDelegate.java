package cz.cas.lib.arclib.bpm.error;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cas.lib.arclib.bpm.ArclibDelegate;
import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import lombok.Getter;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Service;

/**
 * delegate which throws an exception of specified type
 * <br>
 * throws exception if:
 * <ol>
 * <li>
 * config contains field <i>throw</i> with one of these values: incident/runtime/bpm
 * </li>
 * <li>
 * process variable with the same name as activityId is set to one of these values: incident/runtime/bpm
 * </li>
 * </ol>
 * </p>
 */
@Service
public class ErrorThrowingDelegate extends ArclibDelegate {
    @Getter
    private String toolName;
    @Override
    public void executeArclibDelegate(DelegateExecution execution, String ingestWorkflowExternalId) throws Exception {
        String exceptionTypeString = getStringVariable(execution, execution.getCurrentActivityId());
        if (exceptionTypeString == null) {
            JsonNode configRoot = getConfigRoot(execution);
            JsonNode exceptionType = configRoot.get("throw");
            if (exceptionType == null)
                return;
            exceptionTypeString = exceptionType.textValue();
        }
        if (exceptionTypeString == null)
            return;
        switch (exceptionTypeString) {
            case "runtime":
                throw new RuntimeException("internalError");
            case "bpm":
                throw new BpmnError(BpmConstants.ErrorCodes.ProcessFailure, BpmConstants.ErrorCodes.ProcessFailure);
            case "incident":
                throw new IncidentException("incident exception");
        }
    }
}
