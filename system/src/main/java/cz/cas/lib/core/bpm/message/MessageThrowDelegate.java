package cz.cas.lib.core.bpm.message;

import lombok.Getter;
import lombok.Setter;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.model.bpmn.instance.Event;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.Message;
import org.camunda.bpm.model.bpmn.instance.MessageEventDefinition;

/**
 * Abstract implementation of Message throw event providing access to defined message.
 */
@Getter
@Setter
public abstract class MessageThrowDelegate implements JavaDelegate {
    /**
     * Retrieves the name of message from BPMN diagram
     *
     * @param execution Execution
     * @return Message name
     * @throws UnsupportedOperationException If the element where this delegate is used is not a message event or the message is not specified
     */
    protected String getMessageName(DelegateExecution execution) {
        FlowElement element = execution.getBpmnModelElementInstance();
        if (element instanceof Event) {
            MessageEventDefinition definition = (MessageEventDefinition) element
                    .getUniqueChildElementByType(MessageEventDefinition.class);

            if (definition != null) {
                Message message = definition.getMessage();

                if (message != null) {
                    return message.getName();
                }
            }

            throw new UnsupportedOperationException("Message must by specified in BPMN diagram.");
        } else {
            throw new UnsupportedOperationException("Only message events are supported with this delegate.");
        }
    }
}
