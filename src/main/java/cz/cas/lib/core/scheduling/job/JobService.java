package cz.cas.lib.core.scheduling.job;

import cz.cas.lib.core.rest.data.DelegateAdapter;
import cz.cas.lib.core.scheduling.JobRunner;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;

@Service
@Slf4j
public class JobService implements DelegateAdapter<Job> {
    @Getter
    private JobStore delegate;

    private TaskScheduler scheduler;

    private JobRunner runner;

    private ConcurrentMap<String, ScheduledFuture<?>> runningJobs = new ConcurrentHashMap<>();

    @Override
    public Job save(Job entity) {
        entity = delegate.save(entity);

        updateJobSchedule(entity);

        return entity;
    }

    @Override
    public void delete(Job entity) {
        entity.setActive(false);
        updateJobSchedule(entity);

        delegate.delete(entity);
    }

    public Collection<Job> findAll() {
        return delegate.findAll();
    }

    public void updateJobSchedule(Job job) {
        try {
            ScheduledFuture<?> future = runningJobs.remove(job.getId());
            if (future != null) {
                log.info("Disabling {}", job);
                future.cancel(false);
            }

            if (job.getActive() == Boolean.TRUE) {
                log.info("Enabling {}", job);

                future = scheduler.schedule(() -> runner.run(job), new CronTrigger(job.getTiming()));
                runningJobs.put(job.getId(), future);
            }
        } catch (Exception ex) {
            log.error("Failed to schedule '{}'", job);
        }
    }

    @Inject
    public void setDelegate(JobStore delegate) {
        this.delegate = delegate;
    }

    @Inject
    public void setScheduler(TaskScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Inject
    public void setRunner(JobRunner runner) {
        this.runner = runner;
    }
}
