package cz.cas.lib.core.rest.data;

import cz.cas.lib.core.domain.DomainObject;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;

public interface DelegateAdapter<T extends DomainObject> extends DataAdapter<T> {
    DataAdapter<T> getDelegate();

    @Override
    default Class<T> getType() {
        return getDelegate().getType();
    }

    @Override
    default T find(String id) {
        return getDelegate().find(id);
    }

    @Override
    default Result<T> findAll(Params params) {
        return getDelegate().findAll(params);
    }

    @Override
    default T save(T entity) {
        return getDelegate().save(entity);
    }

    @Override
    default void delete(T entity) {
        getDelegate().delete(entity);
    }
}
