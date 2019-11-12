package cz.cas.lib.arclib.domainbase.store;

import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.StringPath;
import cz.cas.lib.arclib.domainbase.domain.NamedObject;
import cz.cas.lib.arclib.domainbase.domain.NamedObject;

public abstract class NamedStore<T extends NamedObject, Q extends EntityPathBase<T>> extends DatedStore<T, Q> {
    public NamedStore(Class<T> type, Class<Q> qType) {
        super(type, qType);
    }

    /**
     * Finds the first instance with provided name.
     *
     * @param name Name of instance to find
     * @return Single instance or null if not found
     */
    public T findByName(String name) {
        StringPath idPath = propertyPath("name");
        T entity = query().select(qObject()).where(idPath.eq(name)).fetchFirst();
        detachAll();
        return entity;
    }
}
