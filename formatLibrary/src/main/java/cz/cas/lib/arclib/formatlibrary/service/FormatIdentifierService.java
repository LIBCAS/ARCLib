package cz.cas.lib.arclib.formatlibrary.service;

import cz.cas.lib.arclib.formatlibrary.domain.FormatIdentifier;
import cz.cas.lib.arclib.formatlibrary.domain.FormatIdentifierType;
import cz.cas.lib.arclib.formatlibrary.store.FormatIdentifierStore;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FormatIdentifierService {

    private FormatIdentifierStore store;

    @Transactional
    public FormatIdentifier findByIdentifierTypeAndIdentifier(FormatIdentifierType identifierType, String identifier) {
        return store.findByIdentifierTypeAndIdentifier(identifierType, identifier);
    }

    @Transactional
    public FormatIdentifier save(FormatIdentifier entity) {
        return store.save(entity);
    }

    @Inject
    public void setStore(FormatIdentifierStore store) {
        this.store = store;
    }
}
