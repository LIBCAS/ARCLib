package cz.inqool.uas.report.provider;

import cz.inqool.uas.report.exception.GeneratingException;
import cz.inqool.uas.script.ScriptExecutor;
import cz.inqool.uas.script.ScriptType;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Map;

import static cz.inqool.uas.util.Utils.asMap;

/**
 * Report provider working with Groovy/Javascript and params supplied from user.
 *
 * <p>
 *     This {@link ReportProvider} is used when generating complex report.
 * </p>
 */
@Slf4j
@Service
public class ScriptProvider extends BaseProvider<ScriptProvider.Input> {
    private ScriptExecutor executor;

    public ScriptProvider() {
        super(Input.class);
    }

    @Override
    public String getName() {
        return "Script provider";
    }

    @Override
    public Map<String, Object> provide(Input input) {

        if (input.getScript() != null && input.getType() != null && input.getParams() != null) {
            Object result = executor.executeScript(input.getType(), input.getScript(), input.getParams());
            return asMap("result", result);
        } else {
            log.error("Script/type/params query not provided.");
            throw new GeneratingException("Bad script/type/params specified.");
        }
    }

    @Inject
    public void setExecutor(ScriptExecutor executor) {
        this.executor = executor;
    }

    @Getter
    @Setter
    public static class Input {
        private String script;
        private ScriptType type;
        private Map<String, Object> params;
    }
}
