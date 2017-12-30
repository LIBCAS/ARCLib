package cz.inqool.uas.bpm.config.tenant;

import cz.inqool.uas.bpm.security.UasTenantIdProvider;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@ConditionalOnProperty(prefix = "bpm", name = "enabled", havingValue = "true")
@Component
public class TenantPlugin implements ProcessEnginePlugin {
    private UasTenantIdProvider tenantIdProvider;

    @Override
    public void preInit(ProcessEngineConfigurationImpl configuration) {
        configuration.setTenantIdProvider(tenantIdProvider);
    }

    @Override
    public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {

    }

    @Override
    public void postProcessEngineBuild(ProcessEngine processEngine) {

    }

    @Inject
    public void setTenantIdProvider(UasTenantIdProvider tenantIdProvider) {
        this.tenantIdProvider = tenantIdProvider;
    }
}
