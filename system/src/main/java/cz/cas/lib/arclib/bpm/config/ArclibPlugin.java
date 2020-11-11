package cz.cas.lib.arclib.bpm.config;

import cz.cas.lib.arclib.service.incident.CustomIncidentHandler;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.camunda.bpm.engine.impl.history.HistoryLevel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static cz.cas.lib.core.util.Utils.asList;

@ConditionalOnProperty(prefix = "bpm", name = "enabled", havingValue = "true")
@Component
public class ArclibPlugin implements ProcessEnginePlugin {

    @Inject
    private CustomIncidentHandler customIncidentHandler;

    @Override
    public void preInit(ProcessEngineConfigurationImpl configuration) {
        configuration.setDefaultNumberOfRetries(1);
        configuration.setHistoryLevel(HistoryLevel.HISTORY_LEVEL_FULL);
        configuration.setJobExecutorAcquireByPriority(true);
        customIncidentHandler.setRuntimeService(configuration.getRuntimeService());
        customIncidentHandler.setManagementService(configuration.getManagementService());
        configuration.setCustomIncidentHandlers(asList(customIncidentHandler));
    }

    @Override
    public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
    }

    @Override
    public void postProcessEngineBuild(ProcessEngine processEngine) {
    }
}
