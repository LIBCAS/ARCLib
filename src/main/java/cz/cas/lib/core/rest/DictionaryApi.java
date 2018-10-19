package cz.cas.lib.core.rest;

import cz.cas.lib.core.domain.DictionaryObject;
import cz.cas.lib.core.rest.data.DictionaryDataAdapter;

/**
 * Generic RESTful CRUD API for accessing {@link cz.cas.lib.core.index.solr.SolrStore}.
 *
 * @param <T> type of JPA entity
 */
public interface DictionaryApi<T extends DictionaryObject> extends NamedApi<T> {
    DictionaryDataAdapter<T> getAdapter();

    default String getNameAttribute() {
        return "name";
    }
}
