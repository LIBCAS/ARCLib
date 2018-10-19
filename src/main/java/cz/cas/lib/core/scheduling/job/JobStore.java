package cz.cas.lib.core.scheduling.job;

import cz.cas.lib.core.index.solr.SolrDatedStore;
import cz.cas.lib.core.store.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Implementation of {@link cz.cas.lib.core.index.solr.SolrStore} for storing {@link Job} and indexing {@link SolrJob}.
 */
@Repository
public class JobStore extends SolrDatedStore<Job, QJob, SolrJob> {

    public JobStore() {
        super(Job.class, QJob.class, SolrJob.class);
    }

    @Override
    public SolrJob toIndexObject(Job obj) {
        SolrJob indexed = super.toIndexObject(obj);

        indexed.setName(obj.getName());
        indexed.setTiming(obj.getTiming());
        indexed.setScriptTypeName(obj.getScriptType().getLabel());
        indexed.setActive(obj.getActive());

        return indexed;
    }

    @Transactional
    public List<Job> findAllActive() {
        QJob qJob = qObject();

        List<Job> jobs = query().select(qJob)
                .from(qJob)
                .where(findWhereExpression())
                .where(qJob.active.eq(true))
                .fetch();

        detachAll();

        return jobs;
    }
}
