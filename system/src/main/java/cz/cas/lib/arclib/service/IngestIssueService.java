package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.ingestWorkflow.IngestIssue;
import cz.cas.lib.arclib.store.IngestIssueStore;
import cz.cas.lib.core.rest.data.DelegateAdapter;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;



@Service
public class IngestIssueService implements DelegateAdapter<IngestIssue> {
    @Getter
    private IngestIssueStore delegate;

    @Autowired
    public void setIngestIssueStore(IngestIssueStore ingestIssueStore) {
        this.delegate = ingestIssueStore;
    }
}
