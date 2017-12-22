package cz.inqool.uas.bpm.config.notify;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ConditionalOnProperty(prefix = "bpm", name = "enabled", havingValue = "true")
@Component
public class NotifyPlugin implements ProcessEnginePlugin {
    private NotifyTaskListener notifyTaskListener;
    private NotifyPoolTaskListener notifyPoolTaskListener;

    @Override
    public void preInit(ProcessEngineConfigurationImpl configuration) {
        List<BpmnParseListener> preParseListeners = configuration.getCustomPreBPMNParseListeners();
        if(preParseListeners == null) {
            preParseListeners = new ArrayList<>();
            configuration.setCustomPreBPMNParseListeners(preParseListeners);
        }
        preParseListeners.add(new NotifyEventParseListener(notifyTaskListener, notifyPoolTaskListener));
    }

    @Override
    public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {

    }

    @Override
    public void postProcessEngineBuild(ProcessEngine processEngine) {

    }

    @Inject
    public void setNotifyTaskListener(NotifyTaskListener notifyTaskListener) {
        this.notifyTaskListener = notifyTaskListener;
    }

    @Inject
    public void setNotifyPoolTaskListener(NotifyPoolTaskListener notifyPoolTaskListener) {
        this.notifyPoolTaskListener = notifyPoolTaskListener;
    }
}
