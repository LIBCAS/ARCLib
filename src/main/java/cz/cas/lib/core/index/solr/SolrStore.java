package cz.cas.lib.core.index.solr;

import cz.cas.lib.core.domain.DatedObject;
import cz.cas.lib.core.domain.DomainObject;
import cz.cas.lib.core.exception.BadArgument;
import cz.cas.lib.core.exception.GeneralException;
import cz.cas.lib.core.index.dto.*;
import cz.cas.lib.core.index.solr.util.NestedCriteria;
import cz.cas.lib.core.store.DatedStore;
import cz.cas.lib.core.store.DomainStore;
import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.mapping.SolrDocument;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleFilterQuery;
import org.springframework.data.solr.core.query.SimpleQuery;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static cz.cas.lib.arclib.index.solr.SolrQueryUtils.*;
import static cz.cas.lib.core.util.Utils.asSet;

/**
 * {@link DatedStore} with automatic Solr indexing and filtering.
 * <p>
 * <p>
 * First purpose of this extension is to hook into {@link DomainStore#save(DomainObject)} method and using defined
 * {@link SolrStore#toIndexObject(DomainObject)} method automatically construct Solr entity from
 * JPA entity and sending it into Solr.
 * </p>
 * <p>
 * Second purpose is retrieval of instances based on complex {@link Params} which encompass filtering, sorting and
 * paging.
 * </p>
 * <p>
 * {@link SolrStore} works only on entities extending {@link DatedObject}.
 * </p>
 *
 * @param <T> type of JPA entity
 * @param <U> type of corresponding Solr entity
 */
@SuppressWarnings("WeakerAccess")
public interface SolrStore<T extends DomainObject, U extends SolrDomainObject> {
    Collection<T> findAll();

    List<T> findAllInList(List<String> ids);

    SolrTemplate getTemplate();

    Class<U> getUType();

    U toIndexObject(T obj);

    default T save(T entity) {
        return index(entity);
    }

    default Collection<? extends T> save(Collection<? extends T> entities) {
        return index(entities);
    }

    default void delete(T entity) {
        removeIndex(entity);
    }

    /**
     * Reindexes all entities from JPA to Solr.
     * <p>
     * <p>
     * Also creates the mapping for type.
     * </p>
     * <p>
     * This method should be used only if the index was previously deleted and recreated. Does not remove old
     * mapping and instances from Solr.
     * </p>
     */
    default void reindex() {
        Collection<T> instances = findAll();
        instances.forEach(this::index);
    }

    /**
     * Deletes all documents from SOLR and reindexes all records from DB
     */
    default void refresh() {
        removeAllIndexes();
        reindex();
    }

    /**
     * Finds all instances that respect the selected {@link Params}.
     * <p>
     * <p>
     * Though {@link Params} one could specify filtering, sorting and paging. For further explanation
     * see {@link Params}.
     * </p>
     * <p>
     * Returning also the total number of instances passed through the filtering phase.
     * </p>
     *
     * @param params Parameters to comply with
     * @return Sorted {@link List} of instances with total number
     */
    default Result<T> findAll(Params params) {
        SimpleQuery query = initializeQuery(params);
        query.addProjectionOnField("id");
        query.addCriteria(Criteria.where("id"));
        query.addFilterQuery(new SimpleFilterQuery(buildFilters(params)));
        Result<T> result = new Result<>();
        Page<U> page = getTemplate().query(getIndexCore(), query, getUType());
        List<String> ids = page.getContent().stream().map(SolrDomainObject::getId).collect(Collectors.toList());
        List<T> sorted = findAllInList(ids);
        result.setItems(sorted);
        result.setCount(page.getTotalElements());
        return result;
    }

    /**
     * Counts all instances that respect the selected {@link Params}.
     * <p>
     * <p>
     * Though {@link Params} one could specify filtering. For further explanation
     * see {@link Params}.
     * </p>
     *
     * @param params Parameters to comply with
     * @return Total number of instances
     */
    default Long count(Params params) {
        SimpleQuery query = new SimpleQuery();
        query.addSort(new Sort(new Sort.Order(Sort.Direction.valueOf(params.getOrder().toString()), params.getSort())));
        query.addProjectionOnField("id");
        query.setPageRequest(new PageRequest(params.getPage(), params.getPageSize()));
        query.addFilterQuery(new SimpleFilterQuery(buildFilters(params)));
        Page<U> page = getTemplate().query(query, getUType());
        return page.getTotalElements();
    }

    /**
     * Gets Solr type
     *
     * @return Name of Solr type
     */
    default String getIndexType() {
        return getUType().getName();
    }

    /**
     * Gets Solr core
     *
     * @return Name of Solr type
     */
    default String getIndexCore() {
        SolrDocument document = getUType().getAnnotation(SolrDocument.class);

        if (document != null) {
            return document.solrCoreName();
        } else {
            throw new GeneralException("Missing Solr @SolrDocument.coreName for " + getUType().getSimpleName());
        }
    }

    default void removeIndex(T obj) {
        getTemplate().deleteById(getIndexCore(), obj.getId());
        getTemplate().commit(getIndexCore());
    }

    default void removeAllIndexes() {
        SimpleQuery query = new SimpleQuery();
        query.addCriteria(Criteria.where("id"));
        getTemplate().delete(getIndexCore(), query);
        getTemplate().commit(getIndexCore());
    }

    default T index(T obj) {
        getTemplate().saveBean(this.toIndexObject(obj));
        getTemplate().commit(getIndexCore());

        return obj;
    }

    default Collection<? extends T> index(Collection<? extends T> objects) {
        if (objects.isEmpty()) {
            return objects;
        }

        List<U> indexObjects = objects.stream()
                .map(this::toIndexObject)
                .collect(Collectors.toList());
        getTemplate().saveBeans(indexObjects);

        getTemplate().commit(getIndexCore());

        return objects;
    }

    default Criteria buildFilters(Params params) {
        if (params.getFilter() == null || params.getFilter().isEmpty())
            return Criteria.where("id");
        List<Criteria> queries = params.getFilter().stream()
                .map(this::buildFilter)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (params.getOperation() == RootFilterOperation.OR) {
            return orQueryInternal(queries);
        } else {
            return andQueryInternal(queries);
        }
    }

    default Criteria buildFilter(Filter filter) {
        String value = sanitizeFilterValue(filter.getValue());
        FilterOperation operation = filter.getOperation();
        if (operation == null) {
            throw new BadArgument("operation not specified: " + filter);
        }
        if (value == null
                && operation != FilterOperation.AND
                && operation != FilterOperation.OR
                && operation != FilterOperation.NOT_NULL
                && operation != FilterOperation.IS_NULL
                && operation != FilterOperation.NESTED) {
            throw new BadArgument("value not specified: " + filter);
        }
        switch (operation) {
            case EQ:
            default:
                return inQuery(filter.getField(), asSet(value));
            case NEQ:
                return notInQuery(filter.getField(), asSet(value));
            case STARTWITH:
                return prefixQuery(filter.getField(), value);
            case ENDWITH:
                return suffixQuery(filter.getField(), value);
            case CONTAINS:
                return containsQuery(filter.getField(), value);
            case GT:
                return gtQuery(filter.getField(), value);
            case LT:
                return ltQuery(filter.getField(), value);
            case GTE:
                return gteQuery(filter.getField(), value);
            case LTE:
                return lteQuery(filter.getField(), value);
            case AND:
                return andQuery(filter.getFilter());
            case OR:
                return orQuery(filter.getFilter());
            case IS_NULL:
                return isNullQuery(filter.getField());
            case NOT_NULL:
                return notNullQuery(filter.getField());
            case NESTED:
                return nestedQuery(filter.getField(), filter.getFilter());
        }
    }

    /**
     * Builds an OR query between sub-filters.
     * <p>
     * Used internally in {@link SolrStore#findAll(Params)} )} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param filters {@link List} of {@link Filter}
     * @return Solr query builder
     */
    default Criteria orQuery(List<Filter> filters) {
        List<Criteria> builders = filters.stream()
                .map(this::buildFilter)
                .collect(Collectors.toList());
        return orQueryInternal(builders);
    }

    /**
     * Builds an AND query between sub-filters.
     * <p>
     * Used internally in {@link SolrStore#findAll(Params)} )} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param filters {@link List} of {@link Filter}
     * @return Solr query builder
     */
    default Criteria andQuery(List<Filter> filters) {
        List<Criteria> builders = filters.stream()
                .map(this::buildFilter)
                .collect(Collectors.toList());
        return andQueryInternal(builders);
    }

    default Criteria nestedQuery(String name, List<Filter> filters) {
        Criteria parentCriteria = Criteria.where("type").is(getIndexType());

        try {
            Class<?> childType = getUType().getDeclaredField(name).getType();
            String childTypeName = childType.getName();
            Criteria childTypeCriteria = Criteria.where("type").is(childTypeName);

            Criteria childrenCriteria = andQuery(filters);
            childrenCriteria.and(childTypeCriteria);

            return new NestedCriteria(parentCriteria, childrenCriteria);

        } catch (Exception e) {
            throw new GeneralException(e);
        }
    }

    /**
     * Returns populated @Field annotation object for attribute
     * going deeper to nested objects if needed and also to generics (Set, List, ...)
     * <p>
     * fixme: add support for arrays
     *
     * @param fieldName name of the field
     * @return populated annotation object
     */
    default Field getFieldAnnotation(Class clazz, String fieldName) {
        try {
            if (fieldName.contains(".")) {
                String[] data = fieldName.split("\\.", 2);
                Class<?> nestedType = clazz.getDeclaredField(data[0]).getType();

                Type genericType = clazz.getDeclaredField(data[0]).getGenericType();
                if (genericType instanceof ParameterizedType) {
                    Type[] typeArguments = ((ParameterizedType) genericType).getActualTypeArguments();
                    if (typeArguments.length == 1 && typeArguments[0] instanceof Class) {
                        nestedType = (Class<?>) typeArguments[0];
                    }
                }

                return getFieldAnnotation(nestedType, data[1]);
            } else {
                return clazz.getDeclaredField(fieldName).getAnnotation(Field.class);
            }
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) {
                return getFieldAnnotation(clazz.getSuperclass(), fieldName);
            } else {
                return null;
            }
        }
    }
}