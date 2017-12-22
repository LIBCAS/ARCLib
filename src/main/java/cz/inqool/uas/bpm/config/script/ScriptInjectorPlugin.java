package cz.inqool.uas.bpm.config.script;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.camunda.bpm.engine.spring.SpringExpressionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConditionalOnProperty(prefix = "bpm", name = "enabled", havingValue = "true")
@Component
public class ScriptInjectorPlugin implements ProcessEnginePlugin {
    private List<ScriptInjector> injectors;

    private ApplicationContext applicationContext;

    @Override
    public void preInit(ProcessEngineConfigurationImpl configuration) {
        Map<Object, Object> beans = configuration.getBeans();
        if (beans == null) {
            beans = new HashMap<>();
        }

        if (injectors != null) {
            for (ScriptInjector injector : injectors) {
                beans.put(injector.getName(), injector.getObject());
            }
        }

        configuration.setBeans(beans);

        configuration.setExpressionManager(new SpringExpressionManager(applicationContext, beans));
    }

    @Override
    public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {

    }

    @Override
    public void postProcessEngineBuild(ProcessEngine processEngine) {

    }

    @Autowired(required = false)
    public void setInjectors(List<ScriptInjector> injectors) {
        this.injectors = injectors;
    }

    @Inject
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}
