package cz.inqool.uas.store;

import com.querydsl.core.types.dsl.EntityPathBase;
import cz.inqool.uas.domain.DictionaryObject;

/**
 * {@link DatedStore} specialized implementation. Exists for orthogonal purpose.
 *
 * @param <T> Type of entity to hold
 * @param <Q> Type of query object
 */
public abstract class DictionaryStore<T extends DictionaryObject, Q extends EntityPathBase<T>> extends DatedStore<T, Q> {
    public DictionaryStore(Class<T> type, Class<Q> qType) {
        super(type, qType);
    }
}
