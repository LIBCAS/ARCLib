package cz.inqool.uas.rest.data;

import cz.inqool.uas.domain.DomainObject;
import cz.inqool.uas.index.dto.Params;
import cz.inqool.uas.index.dto.Result;

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
