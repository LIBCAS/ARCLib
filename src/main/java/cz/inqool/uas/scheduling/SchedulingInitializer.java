package cz.inqool.uas.scheduling;

import cz.inqool.uas.scheduling.job.Job;
import cz.inqool.uas.scheduling.job.JobService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Collection;

/**
 * Gathers timers from Database and add triggers for every timer to spring.
 */
@Slf4j
@Component
public class SchedulingInitializer implements ApplicationListener<ContextRefreshedEvent> {
    private JobService service;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        Collection<Job> jobs = service.findAll();

        log.info("Starting jobs...");

        jobs.forEach(service::updateJobSchedule);
    }

    @Inject
    public void setService(JobService service) {
        this.service = service;
    }
}
