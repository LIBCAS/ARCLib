package cz.inqool.uas.index;

import cz.inqool.uas.domain.DatedObject;
import cz.inqool.uas.domain.DomainObject;
import cz.inqool.uas.exception.BadArgument;
import cz.inqool.uas.exception.GeneralException;
import cz.inqool.uas.exception.MissingObject;
import cz.inqool.uas.index.dto.*;
import cz.inqool.uas.store.DomainStore;
import cz.inqool.uas.util.Utils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldIndex;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static cz.inqool.uas.util.Utils.*;

/**
 * {@link cz.inqool.uas.store.DatedStore} with automatic Elasticsearch indexing and filtering.
 *
 * <p>
 *     First purpose of this extension is to hook into {@link DomainStore#save(DomainObject)} method and using defined
 *     {@link IndexedStore#toIndexObject(DomainObject)} method automatically construct Elasticsearch entity from
 *     JPA entity and sending it into Elasticsearch.
 * </p>
 * <p>
 *     Second purpose is retrieval of instances based on complex {@link Params} which encompass filtering, sorting and
 *     paging.
 * </p>
 * <p>
 *     {@link IndexedStore} works only on entities extending {@link DatedObject}.
 * </p>
 *
 * @param <T> type of JPA entity
 * @param <U> type of corresponding Elasticsearch entity
 */
@SuppressWarnings("WeakerAccess")
public interface IndexedStore<T extends DomainObject, U extends IndexedDomainObject> {
    T find(String id);

    Collection<T> findAll();

    Collection<T> findAll(long offset, long limit);

    List<T> findAllInList(List<String> ids);

    ElasticsearchTemplate getTemplate();

    Class<U> getUType();

    Class<T> getType();

    U toIndexObject(T obj);

    default T save(T entity) {
        return index(entity);
    }

    default Collection<? extends T> save(Collection<? extends T> entities) {
        index(entities);
        return entities;
    }

    default void delete(T entity) {
        removeIndex(entity);
    }

    /**
     * Reindexes all entities from JPA to Elasticsearch.
     *
     * <p>
     *     Also creates the mapping for type.
     * </p>
     * <p>
     *     This method should be used only if the index was previously deleted and recreated. Does not remove old
     *     mapping and instances from Elasticsearch.
     * </p>
     */
    default void reindex() {
        getTemplate().putMapping(getUType());
        Collection<T> instances;
        long batchSize = getReindexBatchSize();
        long offset = 0;
        do {
            instances = findAll(offset, batchSize);
            this.index(instances);
            offset += batchSize;
        } while (!instances.isEmpty());
    }

    /**
     * Reindexes only selected item without putting mapping
     * @param id id of entity to reindex
     */
    default void reindex(String id) {
        T entity = find(id);
        notNull(entity, () -> new MissingObject(getType(), id));
        index(entity);
    }

    /**
     * Finds all instances that respect the selected {@link Params}.
     *
     * <p>
     *     Though {@link Params} one could specify filtering, sorting and paging. For further explanation
     *     see {@link Params}.
     * </p>
     * <p>
     *     Returning also the total number of instances passed through the filtering phase.
     * </p>
     *
     * @param params Parameters to comply with
     * @return Sorted {@link List} of instances with total number
     */
    default Result<T> findAll(Params params) {
        return findAll(params, defaultContext());
    }

    default Result<T> findAll(Params params, SearchContext context) {
        notNull(params.getSort(), () -> new BadArgument("sort"));
        notNull(params.getOrder(), () -> new BadArgument("order"));
        notNull(params.getPage(), () -> new BadArgument("page"));

        Utils.gte(params.getPage(), 0, () -> new BadArgument("page"));

        if (params.getPageSize() != null) {
            Utils.gte(params.getPageSize(), 1, () -> new BadArgument("pageSize"));
        }

        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder()
                .withIndices(context.getIndexNames())
                .withTypes(context.getType())
                .withFilter(buildFilters(params, context))
                .withFields("id");

        if (params.getPageSize() != null) {
            nativeSearchQueryBuilder.withPageable(new PageRequest(params.getPage(), params.getPageSize()));
        } else {
            nativeSearchQueryBuilder.withPageable(new PageRequest(0, 10000));
        }

        if (params.getSorting() != null && !params.getSorting().isEmpty()) { //Add new sorting allowing multiple fields
            for (SortSpecification sortSpecification : params.getSorting()) {
                nativeSearchQueryBuilder.withSort(SortBuilders.fieldSort(sortSpecification.getSort())
                        .order(SortOrder.valueOf(sortSpecification.getOrder().name()))
                );
            }
        } else {  //Legacy sort
            nativeSearchQueryBuilder.withSort(SortBuilders.fieldSort(params.getSort())
                    .order(SortOrder.valueOf(params.getOrder().toString())));
        }

        NativeSearchQuery query = nativeSearchQueryBuilder.build();

        return getTemplate().query(query, response -> {
            Result<T> result = new Result<>();

            List<String> ids = StreamSupport.stream(response.getHits().spliterator(), true)
                    .map(hit -> hit.field("id").<String>getValue())
                    .collect(Collectors.toList());

            List<T> sorted = findAllInList(ids);

            result.setItems(sorted);
            result.setCount(response.getHits().totalHits());

            return result;
        });
    }

    /**
     * Counts all instances that respect the selected {@link Params}.
     *
     * <p>
     *     Though {@link Params} one could specify filtering. For further explanation
     *     see {@link Params}.
     * </p>
     *
     * @param params Parameters to comply with
     * @return Total number of instances
     */
    default Long count(Params params) {
        return count(params, defaultContext());
    }

    default Long count(Params params, SearchContext context) {
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withIndices(context.getIndexNames())
                .withTypes(context.getType())
                .withFilter(buildFilters(params, context))
                .withSort(SortBuilders.fieldSort(params.getSort())
                                      .order(SortOrder.valueOf(params.getOrder().toString()))
                )
                .build();

        return getTemplate().query(query, response -> response.getHits().totalHits());
    }

    /**
     * Builds an IN query.
     *
     * <p>
     *     Tests if the attribute of an instance is found in provided {@link Set} of values. If the {@link Set}
     *     is empty, then this query is silently ignored.
     * </p>
     * <p>
     *     Used internally in {@link IndexedStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name Name of the attribute to check
     * @param values {@link Set} of valid values
     * @return Elasticsearch query builder
     */
    default QueryBuilder inQuery(String name, Set<?> values) {
        if (!values.isEmpty()) {
            return QueryBuilders.termsQuery(name, values.toArray());
        } else {
            return nopQuery();
        }
    }

    /**
     * Builds a NOT IN query.
     *
     * <p>
     *     Tests if the attribute of an instance is not found in provided {@link Set} of values. If the {@link Set}
     *     is empty, then this query is silently ignored.
     * </p>
     * <p>
     *     Used internally in {@link IndexedStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name Name of the attribute to check
     * @param values {@link Set} of invalid values
     * @return Elasticsearch query builder
     */
    default QueryBuilder notInQuery(String name, Set<?> values) {
        if (!values.isEmpty()) {
            return QueryBuilders.boolQuery().mustNot(QueryBuilders.boolQuery().should(inQuery(name, values)));
        } else {
            return nopQuery();
        }
    }

    /**
     * Builds a string prefix query.
     *
     * <p>
     *     Tests if the attribute of an instance starts with the specified value.
     * </p>
     * <p>
     *     Used internally in {@link IndexedStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name Name of the attribute to check
     * @param value Value to test against
     * @return Elasticsearch query builder
     */
    default QueryBuilder prefixQuery(String name, String value) {
        return QueryBuilders.prefixQuery(name, value);
    }

    /**
     * Builds a string suffix query.
     *
     * <p>
     *     Tests if the attribute of an instance ends with the specified value.
     * </p>
     * <p>
     *     Used internally in {@link IndexedStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     * <p>
     *     This query is considerably slower than prefix query due to the nature of indexing. Should be avoided if
     *     possible.
     * </p>
     *
     * @param name Name of the attribute to check
     * @param value Value to test against
     * @return Elasticsearch query builder
     */
    default QueryBuilder suffixQuery(String name, String value) {
        return QueryBuilders.regexpQuery(name, ".*" + value);
    }

    /**
     * Builds a string contains query.
     *
     * <p>
     *     Tests if the attribute of an instance contains the specified value.
     * </p>
     * <p>
     *     Used internally in {@link IndexedStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     * <p>
     *     This query is considerably slower than prefix query due to the nature of indexing. Should be avoided if
     *     possible.
     * </p>
     *
     * @param name Name of the attribute to check
     * @param value Value to test against
     * @return Elasticsearch query builder
     */
    default QueryBuilder containsQuery(String name, String value) {
        return QueryBuilders.regexpQuery(name, ".*" + value + ".*");
    }

    /**
     * Builds a greater than query.
     *
     * <p>
     *     Tests if the attribute of an instance is greater than the specified value. Applicable to number and date
     *     attributes.
     * </p>
     * <p>
     *     Used internally in {@link IndexedStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name Name of the attribute to check
     * @param value Value to test against
     * @return Elasticsearch query builder
     */
    default QueryBuilder gtQuery(String name, String value) {
        return QueryBuilders.rangeQuery(name).gt(value);
    }

    /**
     * Builds a less than query.
     *
     * <p>
     *     Tests if the attribute of an instance is less than the specified value. Applicable to number and date
     *     attributes.
     * </p>
     * <p>
     *     Used internally in {@link IndexedStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name Name of the attribute to check
     * @param value Value to test against
     * @return Elasticsearch query builder
     */
    default QueryBuilder ltQuery(String name, String value) {
        return QueryBuilders.rangeQuery(name).lt(value);
    }

    /**
     * Builds a greater than or equal query.
     *
     * <p>
     *     Tests if the attribute of an instance is greater than or equal to the specified value. Applicable to number
     *     and date attributes.
     * </p>
     * <p>
     *     Used internally in {@link IndexedStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name Name of the attribute to check
     * @param value Value to test against
     * @return Elasticsearch query builder
     */
    default QueryBuilder gteQuery(String name, String value) {
        return QueryBuilders.rangeQuery(name).gte(value);
    }

    /**
     * Builds a less than or equal query.
     *
     * <p>
     *     Tests if the attribute of an instance is less than or equal to the specified value. Applicable to number
     *     and date attributes.
     * </p>
     * <p>
     *     Used internally in {@link IndexedStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name Name of the attribute to check
     * @param value Value to test against
     * @return Elasticsearch query builder
     */
    default QueryBuilder lteQuery(String name, String value) {
        return QueryBuilders.rangeQuery(name).lte(value);
    }

    /**
     * Builds a dummy query, which does nothing.
     *
     * <p>
     *     Should be used instead of conditionally skipping query building.
     * </p>
     * <p>
     *     Used internally in {@link IndexedStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @return Elasticsearch query builder
     */
    default QueryBuilder nopQuery() {
        return QueryBuilders.boolQuery();
    }

    /**
     * Builds set query.
     *
     * <p>
     *     Tests if the attribute of an instance is set.
     * </p>
     * <p>
     *     Used internally in {@link IndexedStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name Name of the attribute to check
     * @return Elasticsearch query builder
     */
    default QueryBuilder notNullQuery(String name) {
        return QueryBuilders.existsQuery(name);
    }


    default QueryBuilder nestedQuery(String name, List<Filter> filters, SearchContext context) {
        return QueryBuilders.nestedQuery(name, andQuery(filters, context));
    }

    default QueryBuilder negateQuery(List<Filter> filters, SearchContext context) {
        return QueryBuilders.boolQuery().mustNot(andQuery(filters, context));
    }
    /**
     * Builds not set query.
     *
     * <p>
     *     Tests if the attribute of an instance is not set.
     * </p>
     * <p>
     *     Used internally in {@link IndexedStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name Name of the attribute to check
     * @return Elasticsearch query builder
     */
    default QueryBuilder isNullQuery(String name) {
        return QueryBuilders.boolQuery().mustNot(notNullQuery(name));
    }

    /**
     * Builds an OR query between sub-filters.
     *
     * <p>
     *     Used internally in {@link IndexedStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param filters {@link List} of {@link Filter}
     * @return Elasticsearch query builder
     */
    default QueryBuilder orQuery(List<Filter> filters, SearchContext context) {
        List<QueryBuilder> builders = filters.stream()
                                             .map(filter -> buildFilter(filter, context))
                                             .collect(Collectors.toList());

        return orQueryInternal(builders);
    }

    /**
     * Builds an AND query between sub-filters.
     *
     * <p>
     *     Used internally in {@link IndexedStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param filters {@link List} of {@link Filter}
     * @return Elasticsearch query builder
     */
    default QueryBuilder andQuery(List<Filter> filters, SearchContext context) {
        List<QueryBuilder> builders = filters.stream()
                                             .map(filter -> buildFilter(filter, context))
                                             .collect(Collectors.toList());

        return andQueryInternal(builders);
    }

    /**
     * Gets Elasticsearch type
     *
     * @return Name of Elasticsearch type
     */
    default String getIndexType() {
        Document document = getUType().getAnnotation(Document.class);

        if (document != null) {
            return document.type();
        } else {
            throw new GeneralException("Missing Elasticsearch @Document.type for " + getUType().getSimpleName());
        }
    }

    /**
     * Gets Elasticsearch index
     *
     * @return Name of Elasticsearch index
     */
    default String getIndexName() {
        Document document = getUType().getAnnotation(Document.class);

        if (document != null) {
            return document.indexName();
        } else {
            throw new GeneralException("Missing Elasticsearch @Document.indexName for " + getUType().getSimpleName());
        }
    }

    default void removeIndex(T obj) {
        getTemplate().delete(getUType(), obj.getId());
        getTemplate().refresh(getUType());
    }

    default T index(T obj) {
        IndexQuery query = new IndexQuery();
        query.setId(obj.getId());
        query.setObject(this.toIndexObject(obj));

        getTemplate().index(query);
        getTemplate().refresh(getUType());

        return obj;
    }

    default void index(Collection<? extends T> objects) {
        if (objects.isEmpty()) {
            return;
        }

        List<IndexQuery> queries = objects.stream()
                .map(obj -> {
                    IndexQuery query = new IndexQuery();
                    query.setId(obj.getId());

                    query.setObject(this.toIndexObject(obj));

                    return query;
                })
                .collect(Collectors.toList());

        getTemplate().bulkIndex(queries);
        getTemplate().refresh(getUType());
    }

    default String sanitizeFilterValue(String value) {
        if (value != null && value.trim().length() > 0) {
            return sanitizeElasticsearch(value.trim());
        } else {
            return null;
        }
    }

    /**
     * Returns populated @Field annotation object for attribute
     * going deeper to nested objects if needed and also to generics (Set, List, ...)
     *
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

    default SearchContext defaultContext() {
        SearchContext context = new SearchContext();
        context.setNormalizer(defaultNormalizer());
        context.setType(getIndexType());
        context.setIndexNames(new String[] {getIndexName()});
        return context;
    }

    /**
     * Gets normalizer for folding or identity function
     * Normalizer receives name of the field and value and return normalized value
     * @return normalizer
     */
    default BiFunction<String, String, String> defaultNormalizer() {
        return (fieldName, value) -> {
            if (fieldName != null) {
                Class<U> uType = getUType();
                Field field = getFieldAnnotation(uType, fieldName);

                if (field != null) {
                    String analyzer = field.analyzer();
                    FieldIndex index = field.index();

                    if (index == FieldIndex.analyzed && Objects.equals(analyzer, "folding")) {
                        return normalize(value);
                    }
                }
            }

            return value;
        };
    }

    default QueryBuilder buildFilter(Filter filter, SearchContext context) {
        String value = sanitizeFilterValue(filter.getValue());
        FilterOperation operation = filter.getOperation();
        String field = filter.getField();

        if (operation == null) {
            throw new BadArgument("operation");
        }

        if (value == null
                && operation != FilterOperation.AND
                && operation != FilterOperation.OR
                && operation != FilterOperation.NOT_NULL
                && operation != FilterOperation.IS_NULL
                && operation != FilterOperation.NESTED
                && operation != FilterOperation.NEGATE) {
            throw new BadArgument("value");
        }

        BiFunction<String, String, String> normalizer = context.getNormalizer();
        String normalizedValue = normalizer.apply(field, value);

        switch (operation) {
            case EQ:
            default:
                return inQuery(field, asSet(normalizedValue));
            case NEQ:
                return notInQuery(field, asSet(normalizedValue));
            case STARTWITH:
                return prefixQuery(field, normalizedValue);
            case ENDWITH:
                return suffixQuery(field, normalizedValue);
            case CONTAINS:
                return containsQuery(field, normalizedValue);
            case GT:
                return gtQuery(field, value);
            case LT:
                return ltQuery(field, value);
            case GTE:
                return gteQuery(field, value);
            case LTE:
                return lteQuery(field, value);
            case AND:
                return andQuery(filter.getFilter(), context);
            case OR:
                return orQuery(filter.getFilter(), context);
            case IS_NULL:
                return isNullQuery(field);
            case NOT_NULL:
                return notNullQuery(field);
            case NESTED:
                return nestedQuery(field, filter.getFilter(), context);
            case NEGATE:
                return negateQuery(filter.getFilter(), context);
        }
    }

    default QueryBuilder buildFilters(Params params, SearchContext context) {
        if (params.getFilter() == null) {
            return nopQuery();
        }

        List<QueryBuilder> queries = params.getFilter().stream()
                .map(filter -> buildFilter(filter, context))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        QueryBuilder internalFilter = params.getInternalFilter();
        List<QueryBuilder> filterList;

        if (internalFilter != null) {
            filterList = asList(queries, internalFilter);
        } else {
            filterList = queries;
        }

        if (params.getOperation() == RootFilterOperation.OR) {
            return orQueryInternal(filterList);
        } else {
            return andQueryInternal(filterList);
        }
    }

    default QueryBuilder andQueryInternal(List<QueryBuilder> filters) {
        BoolQueryBuilder query = QueryBuilders.boolQuery();

        for (QueryBuilder filter : filters) {
            query.must(filter);
        }

        return query;
    }

    default QueryBuilder orQueryInternal(List<QueryBuilder> filters) {
        if (filters.size() == 0) {
            return QueryBuilders.boolQuery().mustNot(QueryBuilders.matchAllQuery());
        }

        BoolQueryBuilder query = QueryBuilders.boolQuery();

        for (QueryBuilder filter : filters) {
            query.should(filter);
        }

        return query;
    }

    /**
     * Defines the batch size used during reindexing
     * @return batch size
     */
    default long getReindexBatchSize() {
        return 10000;
    }
}