package cz.cas.lib.core.scheduling.run;

import cz.cas.lib.core.index.solr.SolrDatedStore;
import cz.cas.lib.core.scheduling.job.Job;
import org.springframework.stereotype.Repository;

import static cz.cas.lib.core.util.Utils.toSolrReference;

/**
 * Implementation of {@link cz.cas.lib.core.index.solr.SolrStore} for storing {@link JobRun} and indexing {@link SolrJobRun}.
 */
@Repository
public class JobRunStore extends SolrDatedStore<JobRun, QJobRun, SolrJobRun> {

    public JobRunStore() {
        super(JobRun.class, QJobRun.class, SolrJobRun.class);
    }

    @Override
    public SolrJobRun toIndexObject(JobRun obj) {
        SolrJobRun indexed = super.toIndexObject(obj);

        indexed.setResult(obj.getResult());
        indexed.setSuccess(obj.getSuccess());
        indexed.setJob(toSolrReference(obj.getJob(), Job::getName));

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
