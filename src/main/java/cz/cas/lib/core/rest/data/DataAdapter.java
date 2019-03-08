package cz.cas.lib.core.rest.data;

import cz.cas.lib.core.domain.DomainObject;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;

import java.util.Collection;
import java.util.List;

public interface DataAdapter<T extends DomainObject> {
    Class<T> getType();

    T find(String id);


    Result<T> findAll(Params params);

    Collection<T> findAll();

    T save(T entity);


    void delete(T entity);

    List<T> findAllInList(List<String> ids);

    void hardDelete(T entity);
}
