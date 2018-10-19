package cz.cas.lib.core.store;

import com.querydsl.core.types.dsl.*;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.cas.lib.core.domain.DomainObject;
import cz.cas.lib.core.exception.GeneralException;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import cz.cas.lib.core.rest.data.DataAdapter;
import lombok.Getter;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cz.cas.lib.core.util.Utils.notNull;
import static cz.cas.lib.core.util.Utils.sortByIdList;
import static java.util.Collections.emptyList;

/**
 * Facade around JPA {@link EntityManager} and QueryDSL providing CRUD operations.
 * <p>
 * <p>
 * All the entity instances should have externally set {@link DomainObject#id} to an {@link java.util.UUID},
 * therefore we do not now if the instance is already saved in database or is completely new. Because of that, there
 * is no create/update method, only the {@link DomainStore#save(DomainObject)}, which handles both cases.
 * </p>
 * <p>
 * <p>
 * JPA concept of managed/detached instances is prone to development errors. Therefore every instance should be
 * detached upon retrieving. All methods in {@link DomainStore} adhere to this rule.
 * </p>
 * <p>
 * All find methods in child classes should retrieve only ids on their own and then use
 * {@link DomainStore#findAllInList(List)} which detach the instances or use
 * {@link DomainStore#detachAll()} explicitly.
 * </p>
 * <p>
 * <p>
 * After every saving of instance, the {@link EntityManager}'s context is flushed. This is a rather expensive
 * operation and therefore if more than a few instances should be saved in a row, one should use
 * {@link DomainStore#save(Collection)} which provides batching and only after saving all instances the context is
 * flushed.
 * </p>
 *
 * @param <T> Type of entity to hold
 * @param <Q> Type of query object
 */
public abstract class DomainStore<T extends DomainObject, Q extends EntityPathBase<T>> implements DataAdapter<T> {
    /**
     * Entity manager used for JPA
     */
    protected EntityManager entityManager;

    /**
     * QueryDSL query factory
     */
    protected JPAQueryFactory queryFactory;

    /**
     * Entity class object
     */
    @Getter
    protected Class<T> type;

    /**
     * QueryDSL meta class object
     */
    protected Class<Q> qType;

    private Q qObject;

    public DomainStore(Class<T> type, Class<Q> qType) {
        this.type = type;
        this.qType = qType;

        this.qObject = constructQObject(type, qType);
    }

    /**
     * Finds all instances.
     * <p>
     * <p>
     * Possibly very cost operation. Should be used only if we know there is not many instances or for
     * debugging purposes.
     * </p>
     *
     * @return {@link Collection} of instances
     */
    public Collection<T> findAll() {
        return findAll(0, 0);
    }

    /**
     * Returns a portion of all instances.
     * Limit 0 for limitless.
     * <p>
     * <p>
     * Possibly less costly operation. Should be used especially if we know there is many instances or for
     * debugging purposes.
     * </p>
     *
     * @return {@link Collection} of instances
     */
    public Collection<T> findAll(long offset, long limit) {
        JPAQuery<T> query = query().select(qObject());
        applyWhereExpression(query);

        if (offset != 0) {
            query.offset(offset);
        }

        if (limit != 0) {
            query.limit(limit);
        }

        List<T> list = query.fetch();

        detachAll();

        return list;
    }

    /**
     * Count entities in db.
     *
     * @return count
     */
    public long countAll() {
        JPAQuery<T> query = query().select(qObject());
        applyWhereExpression(query);
        return query.fetchCount();
    }

    /**
     * Finds the first instance.
     * <p>
     * <p>
     * Because there is no ordering it is not defined which instance will be returned. Should be used if there
     * is only one instance or in unit tests.
     * </p>
     *
     * @return Single instance or null if no instance exists
     */
    public T findAny() {
        JPAQuery<T> query = query().select(qObject);
        applyWhereExpression(query);

        T entity = query.fetchFirst();

        detachAll();

        return entity;
    }

    /**
     * Finds all the instances corresponding to the specified {@link List} of ids.
     * <p>
     * <p>
     * The returned {@link List} of instances is ordered according to the order of provided ids. If the instance
     * with provided id is not found, it is skipped, therefore the size of returned {@link List} might be of
     * different size that of the provided ids {@link List}.
     * </p>
     *
     * @param ids Ordered {@link List} of ids
     * @return ordered {@link List} of instances
     */
    public List<T> findAllInList(List<String> ids) {
        if (ids.isEmpty()) {
            return emptyList();
        }

        StringPath idPath = propertyPath("id");

        JPAQuery<T> query = query().select(qObject).where(idPath.in(ids));
        applyWhereExpression(query);

        List<T> list = query.fetch();

        detachAll();

        return sortByIdList(ids, list);
    }

    /**
     * Finds the single instance with provided id.
     *
     * @param id Id of instance to find
     * @return Single instance or null if not found
     */
    public T find(String id) {
        StringPath idPath = propertyPath("id");

        JPAQuery<T> query = query().select(qObject).where(idPath.eq(id));
        applyWhereExpression(query);

        T entity = query.fetchFirst();

        detachAll();

        return entity;
    }

    public T getReference(String id) {
        return entityManager.getReference(type, id);
    }

    /**
     * Creates or updates instance.
     * <p>
     * <p>
     * Corresponds to {@link EntityManager#merge(Object)} method.
     * </p>
     *
     * @param entity Instance to save
     * @return Saved detached instance
     * @throws IllegalArgumentException If entity is NULL
     */
    public T save(T entity) {
        notNull(entity, () -> new IllegalArgumentException("entity"));

        T obj = entityManager.merge(entity);

        entityManager.flush();
        detachAll();

        return obj;
    }

    /**
     * Provides batching for {@link DomainStore#save(DomainObject)} method.
     *
     * @param entities Instances to save
     * @throws IllegalArgumentException If entity is NULL
     */
    public Collection<? extends T> save(Collection<? extends T> entities) {
        notNull(entities, () -> new IllegalArgumentException("entities"));

        Set<? extends T> saved = entities.stream()
                .map(entityManager::merge)
                .collect(Collectors.toSet());

        entityManager.flush();
        detachAll();

        return saved;
    }

    /**
     * Deletes an instance.
     * <p>
     * <p>
     * Non existing instance is silently skipped.
     * </p>
     *
     * @param entity Instance to delete
     * @throws IllegalArgumentException If entity is NULL
     */
    public void delete(T entity) {
        if (!entityManager.contains(entity) && entity != null) {
            entity = entityManager.find(type, entity.getId());
        }

        if (entity != null) {
            entityManager.remove(entity);
        }
    }

    /**
     * Creates QueryDSL query object.
     *
     * @return Query object
     */
    protected JPAQuery<?> query() {
        return queryFactory.from(qObject);
    }

    /**
     * Creates QueryDSL query object for other entity than the storeAip one.
     *
     * @return Query object
     */
    protected <C> JPAQuery<?> query(EntityPathBase<C> base) {
        return queryFactory.from(base);
    }

    /**
     * Gets the used Query DSL object
     *
     * @return The QueryDSL Q instance
     */
    protected Q qObject() {
        return qObject;
    }

    protected void detachAll() {
        entityManager.clear();
    }

    /**
     * Creates meta object attribute.
     * <p>
     * <p>
     * Used for addressing QueryDSL attributes, which are not known at compile time. Should be used with caution,
     * because it circumvents type safety.
     * </p>
     *
     * @param name Name of the attribute
     * @return Meta object attribute
     */
    protected StringPath propertyPath(String name) {
        PathBuilder<T> builder = new PathBuilder<>(qObject.getType(), qObject.getMetadata().getName());
        return builder.getString(name);
    }

    /**
     * Creates meta object attribute for enum type
     * <p>
     * <p>
     * Used for addressing QueryDSL attributes, which are not known at compile time. Should be used with caution,
     * because it circumvents type safety.
     * </p>
     *
     * @param name Name of the attribute
     * @return Meta object attribute
     */
    protected <X extends Enum<X>> EnumPath<X> propertyPathEnum(String name, Class<X> type) {
        PathBuilder<T> builder = new PathBuilder<>(qObject.getType(), qObject.getMetadata().getName());
        return builder.getEnum(name, type);
    }

    /**
     * Provides extension point for inheriting classes to define a where clause for all find* methods
     * in {@link DomainStore}.
     *
     * @return A where clause or null
     */
    protected BooleanExpression findWhereExpression() {
        return null;
    }

    private void applyWhereExpression(JPAQuery<T> query) {
        BooleanExpression expression = findWhereExpression();

        if (expression != null) {
            query.where(expression);
        }
    }

    private Q constructQObject(Class<T> type, Class<Q> qType) {
        String name = type.getSimpleName();

        try {
            Constructor<Q> constructor = qType.getConstructor(String.class);
            return constructor.newInstance(name);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new GeneralException("Error creating Q object for + " + type.getName());
        }
    }

    @Override
    public Result<T> findAll(Params params) {
        throw new UnsupportedOperationException();
    }

    @Inject
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Inject
    public void setQueryFactory(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }
}