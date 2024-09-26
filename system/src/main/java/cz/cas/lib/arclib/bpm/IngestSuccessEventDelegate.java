package cz.cas.lib.arclib.bpm;

import cz.cas.lib.arclib.dto.JmsDto;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IngestSuccessEventDelegate implements JavaDelegate {
    private JmsTemplate template;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String responsiblePersonId = (String) execution.getVariable(BpmConstants.ProcessVariables.responsiblePerson);
        String batchId = (String) execution.getVariable(BpmConstants.ProcessVariables.batchId);
        template.convertAndSend("finish", new JmsDto(batchId, responsiblePersonId));
    }

    @Autowired
    public void setTemplate(JmsTemplate template) {
        this.template = template;
    }
}
