package cz.cas.lib.core.scheduling.run;

import cz.cas.lib.arclib.domainbase.store.DatedStore;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Implementation  for storing {@link JobRun}.
 */
@Repository
public class JobRunStore extends DatedStore<JobRun, QJobRun> {

    public JobRunStore() {
        super(JobRun.class, QJobRun.class);
    }

    public JobRun find(String jobId, String runId) {
        QJobRun qJobRun = qObject();

        JobRun run = query().select(qJobRun)
                .where(qJobRun.id.eq(runId))
                .where(qJobRun.job.id.eq(jobId))
                .where(qJobRun.deleted.isNull())
                .fetchFirst();

        detachAll();
        return run;
    }

    public List<JobRun> findByJob(String jobId) {
        QJobRun qJobRun = qObject();

        List<JobRun> fetch = query().select(qJobRun)
                .where(qJobRun.job.id.eq(jobId))
                .where(qJobRun.deleted.isNull())
                .fetch();

        detachAll();
        return fetch;
    }
}
