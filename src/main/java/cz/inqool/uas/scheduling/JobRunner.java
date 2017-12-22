package cz.inqool.uas.scheduling;

import cz.inqool.uas.exception.BadArgument;
import cz.inqool.uas.notification.NotificationService;
import cz.inqool.uas.scheduling.job.Job;
import cz.inqool.uas.scheduling.run.JobRun;
import cz.inqool.uas.script.ScriptExecutor;
import cz.inqool.uas.security.Permissions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import java.io.StringWriter;

import static cz.inqool.uas.util.Utils.notNull;

@Service
public class JobRunner {
    private ScriptExecutor executor;

    private JobLogger logger;

    private NotificationService notificationService;

    private Boolean notification;

    private Boolean emailingNotification;

    private Boolean flashNotification;

    public void run(Job job) {
        notNull(job, () -> new BadArgument("job"));

        StringWriter console = new StringWriter();

        Object result = null;
        boolean success = true;
        try {
            result = executor.executeScriptWithConsole(job.getScriptType(), job.getScript(), console);
        } catch (Exception ignored) {
            success = false;
        }

        JobRun run = new JobRun();
        run.setJob(job);
        run.setResult(result != null ? result.toString() : null);
        run.setConsole(console.toString());
        run.setSuccess(success);

        if (!success && notification) {
            notificationService.createMultiNotificationWithPermission(
                    "Failed job " + job.getName(),
                    "Specified job has failed to run.",
                    Permissions.JOB,
                    emailingNotification,
                    flashNotification
            );
        }

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

    @Inject
    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Inject
    public void setEmailingNotification(@Value("${scheduling.notification.email:false}") Boolean emailingNotification) {
        this.emailingNotification = emailingNotification;
    }

    @Inject
    public void setFlashNotification(@Value("${scheduling.notification.flash:false}") Boolean flashNotification) {
        this.flashNotification = flashNotification;
    }

    @Inject
    public void setNotification(@Value("${scheduling.notification.enabled:true}") Boolean notification) {
        this.notification = notification;
    }
}
