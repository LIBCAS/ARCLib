package cz.inqool.uas.action;

import cz.inqool.uas.exception.MissingObject;
import cz.inqool.uas.script.ScriptExecutor;
import cz.inqool.uas.store.Transactional;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Map;

import static cz.inqool.uas.util.Utils.notNull;

/**
 * Wraps execution of actions
 */
@Service
public class ActionExecutor {
    private ScriptExecutor executor;

    private ActionStore store;

    /**
     * Loads and execute the action through the scripting engine
     * @param actionId id of Action
     * @param params params to use
     * @return return value from script
     */
    @Transactional
    public Object execute(String actionId, Map<String, Object> params) {
        Action action = store.find(actionId);
        notNull(action, () -> new MissingObject(Action.class, actionId));

        return executor.executeScript(action.getScriptType(), action.getScript(), params);
    }

    @Inject
    public void setExecutor(ScriptExecutor executor) {
        this.executor = executor;
    }

    @Inject
    public void setStore(ActionStore store) {
        this.store = store;
    }
}
