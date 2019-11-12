package cz.cas.lib.arclib.bpm;

import org.camunda.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.camunda.bpm.engine.impl.history.HistoryLevel;
import org.camunda.bpm.engine.impl.incident.IncidentHandler;
import org.camunda.bpm.engine.test.mock.MockExpressionManager;

import static cz.cas.lib.core.util.Utils.asList;

public class BpmTestConfig extends StandaloneInMemProcessEngineConfiguration {
    public BpmTestConfig() {
        super();
        setExpressionManager(new MockExpressionManager());
        setDatabaseSchemaUpdate("true");
        setDefaultNumberOfRetries(1);
        setHistoryLevel(HistoryLevel.HISTORY_LEVEL_FULL);
    }

    public BpmTestConfig(IncidentHandler handler) {
        this();
        setCustomIncidentHandlers(asList(handler));
    }
}
