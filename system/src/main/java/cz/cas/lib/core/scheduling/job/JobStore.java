package cz.cas.lib.core.scheduling.job;

import cz.cas.lib.arclib.domainbase.store.DatedStore;
import cz.cas.lib.core.store.Transactional;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Implementation for storing {@link Job}.
 */
@Repository
public class JobStore extends DatedStore<Job, QJob> {

    public JobStore() {
        super(Job.class, QJob.class);
    }

    @Override
    public void delete(Job entity) {
        if (!entityManager.contains(entity) && entity != null) {
            entity = entityManager.find(type, entity.getId());
        }

        if (entity != null) {
            Instant now = Instant.now();
            entity.setDeleted(now);
            entity.setActive(false);

            entityManager.merge(entity);

            entityManager.flush();
            detachAll();

            logDeleteEvent(entity);
        }
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
