package cz.cas.lib.core.rest.data;

import cz.cas.lib.core.domain.DomainObject;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;

public interface DataAdapter<T extends DomainObject> {
    Class<T> getType();

    T find(String id);


    Result<T> findAll(Params params);

    T save(T entity);


    void delete(T entity);
}
