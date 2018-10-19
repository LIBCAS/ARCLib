package cz.cas.lib.core.store;

import com.querydsl.core.types.dsl.EntityPathBase;
import cz.cas.lib.core.domain.NamedObject;

public abstract class NamedStore<T extends NamedObject, Q extends EntityPathBase<T>> extends DatedStore<T, Q> {
    public NamedStore(Class<T> type, Class<Q> qType) {
        super(type, qType);
    }
}
