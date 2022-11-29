package cz.cas.lib.arclib.formatlibrary.service;

import cz.cas.lib.arclib.formatlibrary.domain.FormatIdentifier;
import cz.cas.lib.arclib.formatlibrary.store.FormatIdentifierStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;

@Service
public class FormatIdentifierService {

    private FormatIdentifierStore store;

    @Transactional
    public FormatIdentifier findByIdentifierTypeAndIdentifier(String identifierType, String identifier) {
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
