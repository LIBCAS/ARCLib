package cz.cas.lib.arclib.index.solr;

import cz.cas.lib.arclib.store.BatchStore;
import cz.cas.lib.arclib.store.ProducerProfileStore;
import cz.cas.lib.arclib.store.UserStore;
import cz.cas.lib.core.scheduling.job.JobStore;
import cz.cas.lib.core.scheduling.run.JobRunStore;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service
public class SolrReindexService {
    private BatchStore batchStore;
    private JobStore jobStore;
    private ProducerProfileStore producerProfileStore;
    private UserStore userStore;
    private JobRunStore jobRunStore;

    public void reindexAll() {
        jobStore.reindex();
        batchStore.reindex();
        producerProfileStore.reindex();
        userStore.reindex();
        jobRunStore.reindex();
    }

    public void refreshAll() {
        jobStore.refresh();
        batchStore.refresh();
        producerProfileStore.refresh();
        userStore.refresh();
        jobRunStore.refresh();
    }

    @Inject
    public void setUserStore(UserStore userStore) {
        this.userStore = userStore;
    }

    @Inject
    public void setBatchStore(BatchStore batchStore) {
        this.batchStore = batchStore;
    }

    @Inject
    public void setJobStore(JobStore jobStore) {
        this.jobStore = jobStore;
    }

    @Inject
    public void setProducerProfileStore(ProducerProfileStore producerProfileStore) {
        this.producerProfileStore = producerProfileStore;
    }

    @Inject
    public void setJobRunStore(JobRunStore jobRunStore) {
        this.jobRunStore = jobRunStore;
    }
}
