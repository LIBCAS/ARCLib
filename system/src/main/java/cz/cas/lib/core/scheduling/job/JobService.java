package cz.cas.lib.core.scheduling.job;

import cz.cas.lib.core.scheduling.JobRunner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;


import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;

@Service
@Slf4j
public class JobService {
    private JobStore store;

    private TaskScheduler scheduler;

    private JobRunner runner;

    private ConcurrentMap<String, ScheduledFuture<?>> runningJobs = new ConcurrentHashMap<>();

    public Job save(Job entity) {
        entity = store.save(entity);

        updateJobSchedule(entity);

        return entity;
    }

    public Job find(String id) {
        return store.find(id);
    }

    public void delete(Job entity) {
        entity.setActive(false);
        updateJobSchedule(entity);

        store.delete(entity);
    }

    public Collection<Job> findAll() {
        return store.findAll();
    }

    public void updateJobSchedule(Job job) {
        try {
            ScheduledFuture<?> future = runningJobs.remove(job.getId());
            if (future != null) {
                log.debug("Disabling {}", job);
                future.cancel(false);
            }

            if (job.getActive() == Boolean.TRUE) {
                log.debug("Enabling {}", job);

                future = scheduler.schedule(() -> runner.run(job), new CronTrigger(job.getTiming()));
                runningJobs.put(job.getId(), future);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("Failed to schedule '{}'. Reason: '{}'", job, ex.getMessage());
        }
    }

    @Autowired
    public void setStore(JobStore store) {
        this.store = store;
    }

    @Autowired
    public void setScheduler(TaskScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Autowired
    public void setRunner(JobRunner runner) {
        this.runner = runner;
    }
}
