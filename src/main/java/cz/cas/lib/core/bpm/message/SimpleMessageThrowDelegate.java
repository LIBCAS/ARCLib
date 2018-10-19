package cz.cas.lib.core.bpm.message;

import cz.cas.lib.core.exception.BadArgument;
import cz.cas.lib.core.util.Utils;
import lombok.Getter;
import lombok.Setter;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.runtime.MessageCorrelationBuilder;

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
    public void execute(DelegateExecution execution) {

        String message = getMessageName(execution);
        Utils.notNull(message, () -> new BadArgument("message"));

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
