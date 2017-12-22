package cz.inqool.uas.error;

import cz.inqool.uas.index.IndexedDomainStore;
import cz.inqool.uas.index.IndexedStore;
import org.springframework.stereotype.Repository;

/**
 * Implementation of {@link IndexedStore} for storing {@link Error} and indexing {@link IndexedError}.
 */
@Repository
public class ErrorStore extends IndexedDomainStore<Error, QError, IndexedError> {

    public ErrorStore() {
        super(Error.class, QError.class, IndexedError.class);
    }

    @Override
    public IndexedError toIndexObject(Error o) {
        IndexedError indexedError = super.toIndexObject(o);

        indexedError.setClientSide(o.getClientSide());
        indexedError.setUserId(o.getUserId());
        indexedError.setIp(o.getIp());
        indexedError.setUrl(o.getUrl());

        return indexedError;
    }
}
