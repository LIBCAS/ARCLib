package cz.cas.lib.core.scheduling;

import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.core.scheduling.job.Job;
import cz.cas.lib.core.scheduling.run.JobRun;
import cz.cas.lib.core.script.ScriptExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.StringWriter;
import java.util.HashMap;

import static cz.cas.lib.core.util.Utils.notNull;

@Service
@Slf4j
public class JobRunner {
    private ScriptExecutor executor;

    private JobLogger logger;

    public void run(Job job) {
        notNull(job, () -> new BadArgument("job"));

        StringWriter console = new StringWriter();

        Object result = null;
        boolean success = true;
        try {
            result = executor.executeScriptWithConsole(job.getScriptType(), job.getScript(),
                    new HashMap<>(job.getParams()), console);
        } catch (Exception ignored) {
            success = false;
        }

        JobRun run = new JobRun();
        run.setJob(job);
        run.setResult(result != null ? result.toString() : null);
        run.setConsole(console.toString());
        run.setSuccess(success);

        logger.log(run);
    }

    @Inject
    public void setExecutor(ScriptExecutor executor) {
        this.executor = executor;
    }

    @Inject
    public void setLogger(JobLogger logger) {
        this.logger = logger;
    }
}
