package cz.cas.lib.core.script;

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;


import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Executes provided script in Javascript or Groovy and return the result.
 * <p>
 * The provided script has access to all spring beans and therefore can do nearly anything.
 */
@Service
public class ScriptExecutor {
    private ApplicationContext context;

    public Object executeScript(ScriptType type, String script) {
        return executeScript(type, script, new HashMap<>());
    }

    public Object executeScriptWithConsole(ScriptType type, String script, StringWriter console) {
        return executeScriptWithConsole(type, script, new HashMap<>(), console);
    }

    public Object executeScript(ScriptType type, String script, Map<String, Object> params) {
        return executeScriptWithConsole(type, script, params, null);
    }

    public Object executeScriptWithConsole(ScriptType type, String script, Map<String, Object> params, StringWriter console) {
        ScriptEngine scriptEngine = produceScriptEngine(type);

        if (console != null) {
            scriptEngine.getContext().setWriter(console);
        }

        SimpleBindings bindings = new SimpleBindings();
        bindings.putAll(params);
        bindings.put("spring", context);

        try {
            return scriptEngine.eval(script, bindings);
        } catch (ScriptException ex) {
            throw new GeneralException(ex);
        }
    }

    private ScriptEngine produceScriptEngine(ScriptType type) {
        switch (type) {
            case GROOVY:
                return new ScriptEngineManager().getEngineByName("groovy");
            case JAVASCRIPT:
                return GraalJSScriptEngine.create(null, Context.newBuilder("js")
                        .allowHostAccess(HostAccess.ALL)
                        .allowHostClassLookup(s -> true)
                        .option("js.ecmascript-version", "2021"));
            default:
                throw new UnsupportedOperationException("invalid script engine '" + type + "'");
        }
    }

    @Autowired
    public void setContext(ApplicationContext context) {
        this.context = context;
    }
}
