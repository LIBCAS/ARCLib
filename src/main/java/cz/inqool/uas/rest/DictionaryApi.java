package cz.inqool.uas.rest;

import cz.inqool.uas.domain.DictionaryObject;
import cz.inqool.uas.index.IndexedStore;
import cz.inqool.uas.rest.data.DictionaryDataAdapter;

/**
 * Generic RESTful CRUD API for accessing {@link IndexedStore}.
 *
 * @param <T> type of JPA entity
 */
public interface DictionaryApi<T extends DictionaryObject> extends NamedApi<T> {
    DictionaryDataAdapter<T> getAdapter();

    default String getNameAttribute() {
        return "name";
    }
}
