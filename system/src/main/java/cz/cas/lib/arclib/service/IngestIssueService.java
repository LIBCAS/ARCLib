package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.ingestWorkflow.IngestIssue;
import cz.cas.lib.arclib.store.IngestIssueStore;
import cz.cas.lib.core.rest.data.DelegateAdapter;
import lombok.Getter;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service
public class IngestIssueService implements DelegateAdapter<IngestIssue> {
    @Getter
    private IngestIssueStore delegate;

    @Inject
    public void setIngestIssueStore(IngestIssueStore ingestIssueStore) {
        this.delegate = ingestIssueStore;
    }
}
