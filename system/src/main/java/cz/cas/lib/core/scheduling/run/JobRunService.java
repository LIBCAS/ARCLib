package cz.cas.lib.core.scheduling.run;

import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

@Service
public class JobRunService {
    private JobRunStore store;

    public List<JobRun> findAll(String jobId) {
        return store.findByJob(jobId);
    }

    public JobRun find(String jobId, String runId) {
        return store.find(jobId, runId);
    }

    @Inject
    public void setStore(JobRunStore store) {
        this.store = store;
    }
}
