package cz.cas.lib.arclib.domainbase.store;

import com.querydsl.core.types.dsl.EntityPathBase;
import cz.cas.lib.arclib.domainbase.domain.NamedObject;

public abstract class NamedStore<T extends NamedObject, Q extends EntityPathBase<T>> extends DatedStore<T, Q> {
    public NamedStore(Class<T> type, Class<Q> qType) {
        super(type, qType);
    }
}
