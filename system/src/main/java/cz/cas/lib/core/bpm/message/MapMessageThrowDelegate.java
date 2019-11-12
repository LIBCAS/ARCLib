package cz.cas.lib.core.bpm.message;

import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.core.util.Utils;
import lombok.Getter;
import lombok.Setter;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.Expression;
import org.camunda.bpm.engine.runtime.MessageCorrelationBuilder;

import java.util.Map;

/**
 * Message throw event implementation with correlation by one variable or none
 * and sending Map of variables.
 */
@Getter
@Setter
public class MapMessageThrowDelegate extends MessageThrowDelegate {
    private Expression correlateName;
    private Expression correlateValue;

    private Expression variables;

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        String message = getMessageName(execution);
        Utils.notNull(message, () -> new BadArgument("message"));

        RuntimeService runtimeService = execution.getProcessEngineServices().getRuntimeService();

        MessageCorrelationBuilder correlation = runtimeService.createMessageCorrelation(message);

        if (variables != null) {
            correlation.setVariables((Map<String, Object>) variables.getValue(execution));
        }

        if (correlateName != null) {
            correlation.processInstanceVariableEquals((String) correlateName.getValue(execution), correlateValue.getValue(execution));
        }

        correlation.correlateWithResult();
    }
}
