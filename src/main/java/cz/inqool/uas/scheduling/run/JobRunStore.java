package cz.inqool.uas.scheduling.run;

import cz.inqool.uas.index.IndexedDatedStore;
import cz.inqool.uas.index.IndexedStore;
import cz.inqool.uas.scheduling.job.Job;
import org.springframework.stereotype.Repository;

import static cz.inqool.uas.util.Utils.toLabeledReference;

/**
 * Implementation of {@link IndexedStore} for storing {@link JobRun} and indexing {@link IndexedJobRun}.
 */
@Repository
public class JobRunStore extends IndexedDatedStore<JobRun, QJobRun, IndexedJobRun> {

    public JobRunStore() {
        super(JobRun.class, QJobRun.class, IndexedJobRun.class);
    }

    @Override
    public IndexedJobRun toIndexObject(JobRun obj) {
        IndexedJobRun indexed = super.toIndexObject(obj);

        indexed.setResult(obj.getResult());
        indexed.setSuccess(obj.getSuccess());
        indexed.setJob(toLabeledReference(obj.getJob(), Job::getName));

        return indexed;
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
}
