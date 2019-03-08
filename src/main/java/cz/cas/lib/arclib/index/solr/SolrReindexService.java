package cz.cas.lib.arclib.index.solr;

import cz.cas.lib.arclib.store.*;
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
    private FormatStore formatStore;
    private IngestIssueStore ingestIssueStore;
    private FormatDefinitionStore formatDefinitionStore;

    /**
     * formats are omitted as there are too many records
     */
    public void reindexAll() {
        jobStore.reindex();
        batchStore.reindex();
        producerProfileStore.reindex();
        userStore.reindex();
        jobRunStore.reindex();
        ingestIssueStore.reindex();
    }

    /**
     * formats are omitted as there are too many records
     */
    public void refreshAll() {
        jobStore.refresh();
        batchStore.refresh();
        producerProfileStore.refresh();
        userStore.refresh();
        jobRunStore.refresh();
        ingestIssueStore.refresh();
    }

    public void refreshFormat() {
        formatStore.refresh();
    }

    public void reindexFormat() {
        formatStore.reindex();
    }

    public void refreshFormatDefinition() {
        formatDefinitionStore.refresh();
    }

    public void reindexFormatDefinition() {
        formatDefinitionStore.reindex();
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

    @Inject
    public void setFormatStore(FormatStore formatStore) {
        this.formatStore = formatStore;
    }

    @Inject
    public void setIngestIssueStore(IngestIssueStore ingestIssueStore) {
        this.ingestIssueStore = ingestIssueStore;
    }

    @Inject
    public void setFormatDefinitionStore(FormatDefinitionStore formatDefinitionStore) {
        this.formatDefinitionStore = formatDefinitionStore;
    }
}
