package cz.cas.lib.arclib.service.preservationPlanning;

import cz.cas.lib.arclib.domain.preservationPlanning.FormatIdentifier;
import cz.cas.lib.arclib.domain.preservationPlanning.FormatIdentifierType;
import cz.cas.lib.arclib.store.FormatIdentifierStore;
import cz.cas.lib.core.rest.data.DelegateAdapter;
import cz.cas.lib.core.store.Transactional;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service
public class FormatIdentifierService implements DelegateAdapter<FormatIdentifier> {
    private FormatIdentifierStore delegate;

    @Transactional
    public FormatIdentifier findByIdentifierTypeAndIdentifier(FormatIdentifierType identifierType, String identifier) {
        return delegate.findByIdentifierTypeAndIdentifier(identifierType, identifier);
    }

    @Override
    @Transactional
    public FormatIdentifier save(FormatIdentifier entity) {
        return delegate.save(entity);
    }

    @Inject
    public void setDelegate(FormatIdentifierStore delegate) {
        this.delegate = delegate;
    }

    @Override
    public FormatIdentifierStore getDelegate() {
        return delegate;
    }
}
