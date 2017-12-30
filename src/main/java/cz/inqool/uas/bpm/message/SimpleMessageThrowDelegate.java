package cz.inqool.uas.bpm.message;

import cz.inqool.uas.exception.BadArgument;
import lombok.Getter;
import lombok.Setter;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.runtime.MessageCorrelationBuilder;

import static cz.inqool.uas.util.Utils.notNull;

/**
 * Message throw event implementation with correlation by one variable or none
 * and sending one variable or none.
 */
@Getter
@Setter
public class SimpleMessageThrowDelegate extends MessageThrowDelegate {
    private String correlateName;
    private String correlateValue;

    private String variableName;
    private String variableValue;

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        String message = getMessageName(execution);
        notNull(message, () -> new BadArgument("message"));

        RuntimeService runtimeService = execution.getProcessEngineServices().getRuntimeService();

        MessageCorrelationBuilder correlation = runtimeService.createMessageCorrelation(message);

        if (variableName != null) {
            correlation.setVariable(variableName, variableValue);
        }

        if (correlateName != null) {
            correlation.processInstanceVariableEquals(correlateName, correlateValue);
        }

        correlation.correlateWithResult();
    }
}
