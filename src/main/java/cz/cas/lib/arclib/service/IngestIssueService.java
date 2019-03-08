package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.ingestWorkflow.IngestIssue;
import cz.cas.lib.arclib.store.IngestIssueStore;
import cz.cas.lib.core.rest.data.DelegateAdapter;
import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.Collection;

@Service
public class IngestIssueService implements DelegateAdapter<IngestIssue> {
    @Getter
    private IngestIssueStore delegate;


    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IngestIssue save(IngestIssue entity) {
        return delegate.save(entity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Collection<IngestIssue> save(Collection<IngestIssue> entities) {
        return (Collection<IngestIssue>) delegate.save(entities);
    }

    @Inject
    public void setIngestIssueStore(IngestIssueStore ingestIssueStore) {
        this.delegate = ingestIssueStore;
    }
}
