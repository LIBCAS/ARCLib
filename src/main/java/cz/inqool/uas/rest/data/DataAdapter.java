package cz.inqool.uas.rest.data;

import cz.inqool.uas.domain.DomainObject;
import cz.inqool.uas.index.dto.Params;
import cz.inqool.uas.index.dto.Result;

public interface DataAdapter<T extends DomainObject> {
    Class<T> getType();

    T find(String id);


    Result<T> findAll(Params params);

    T save(T entity);


    void delete(T entity);
}
