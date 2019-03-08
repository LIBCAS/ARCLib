package cz.cas.lib.core.rest.data;

import cz.cas.lib.core.domain.DomainObject;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;

import java.util.Collection;
import java.util.List;

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
    default Collection<T> findAll() {
        return getDelegate().findAll();
    }

    @Override
    default T save(T entity) {
        return getDelegate().save(entity);
    }

    @Override
    default List<T> findAllInList(List<String> ids) {
        return getDelegate().findAllInList(ids);
    }

    @Override
    default void delete(T entity) {
        getDelegate().delete(entity);
    }

    @Override
    default void hardDelete(T entity) {
        getDelegate().hardDelete(entity);
    }
}
