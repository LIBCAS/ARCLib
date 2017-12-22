package cz.inqool.uas.bpm.config.form;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static cz.inqool.uas.util.Utils.asList;

@ConditionalOnProperty(prefix = "bpm", name = "enabled", havingValue = "true")
@Component
public class FormTypePlugin implements ProcessEnginePlugin {
    @Override
    public void preInit(ProcessEngineConfigurationImpl configuration) {
        configuration.setCustomFormTypes(asList(new ObjectFormType(), new LocalDateTimeFormType()));
    }

    @Override
    public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {

    }

    @Override
    public void postProcessEngineBuild(ProcessEngine processEngine) {

    }
}
