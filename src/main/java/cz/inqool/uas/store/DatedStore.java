package cz.inqool.uas.store;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.StringPath;
import cz.inqool.uas.domain.DatedObject;

import java.time.Instant;

/**
 * {@link DomainStore} specialized implementation automatically setting {@link DatedObject#created},
 * {@link DatedObject#updated} and {@link DatedObject#deleted}.
 *
 * @param <T> Type of entity to hold
 * @param <Q> Type of query object
 */
public abstract class DatedStore<T extends DatedObject, Q extends EntityPathBase<T>> extends DomainStore<T, Q> {
    public DatedStore(Class<T> type, Class<Q> qType) {
        super(type, qType);
    }

    @Override
    protected BooleanExpression findWhereExpression() {
        StringPath deletedPath = propertyPath("deleted");

        return deletedPath.isNull();
    }

    @Override
    public void delete(T entity) {
        if (!entityManager.contains(entity) && entity != null) {
            entity = entityManager.find(type, entity.getId());
        }

        if (entity != null) {
            Instant now = Instant.now();
            entity.setDeleted(now);

            entityManager.merge(entity);

            entityManager.flush();
            detachAll();
        }
    }
}
