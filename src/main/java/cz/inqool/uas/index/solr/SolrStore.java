package cz.inqool.uas.index.solr;

import cz.inqool.uas.domain.DatedObject;
import cz.inqool.uas.domain.DomainObject;
import cz.inqool.uas.exception.BadArgument;
import cz.inqool.uas.exception.GeneralException;
import cz.inqool.uas.index.dto.Filter;
import cz.inqool.uas.index.dto.FilterOperation;
import cz.inqool.uas.index.dto.Params;
import cz.inqool.uas.index.dto.Result;
import cz.inqool.uas.index.solr.util.NestedCriteria;
import cz.inqool.uas.store.DomainStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.mapping.SolrDocument;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleFilterQuery;
import org.springframework.data.solr.core.query.SimpleQuery;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cz.inqool.uas.util.Utils.*;

/**
 * {@link cz.inqool.uas.store.DatedStore} with automatic Solr indexing and filtering.
 *
 * <p>
 *     First purpose of this extension is to hook into {@link DomainStore#save(DomainObject)} method and using defined
 *     {@link SolrStore#toIndexObject(DomainObject)} method automatically construct Solr entity from
 *     JPA entity and sending it into Solr.
 * </p>
 * <p>
 *     Second purpose is retrieval of instances based on complex {@link Params} which encompass filtering, sorting and
 *     paging.
 * </p>
 * <p>
 *     {@link SolrStore} works only on entities extending {@link DatedObject}.
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
     *
     * <p>
     *     Also creates the mapping for type.
     * </p>
     * <p>
     *     This method should be used only if the index was previously deleted and recreated. Does not remove old
     *     mapping and instances from Solr.
     * </p>
     */
    default void reindex() {
        Collection<T> instances = findAll();
        instances.forEach(this::index);
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
        notNull(params.getSort(), () -> new BadArgument("sort"));
        notNull(params.getOrder(), () -> new BadArgument("order"));
        notNull(params.getPage(), () -> new BadArgument("page"));
        notNull(params.getPageSize(), () -> new BadArgument("pageSize"));

        SimpleQuery query = new SimpleQuery();
        query.addCriteria(typeCriteria());
        query.addSort(new Sort(new Sort.Order(Sort.Direction.valueOf(params.getOrder().toString()), params.getSort())));
        query.addProjectionOnField("id");
        query.setPageRequest(new PageRequest(params.getPage(), params.getPageSize()));
        query.addFilterQuery(new SimpleFilterQuery(buildFilters(params)));
        Result<T> result = new Result<>();
        Page<U> page = getTemplate().query(getIndexCore(), query, getUType());
        List<String> ids = page.getContent().stream().map(SolrDomainObject::getId).collect(Collectors.toList());
        List<T> sorted = findAllInList(ids);

        result.setItems(sorted);
        result.setCount(page.getTotalElements());

        return result;
    }

    default Criteria typeCriteria() {
        return inQuery("type", asSet(getIndexType()));
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
        SimpleQuery query = new SimpleQuery();
        query.addCriteria(typeCriteria());
        query.addSort(new Sort(new Sort.Order(Sort.Direction.valueOf(params.getOrder().toString()), params.getSort())));
        query.addProjectionOnField("id");
        query.setPageRequest(new PageRequest(params.getPage(), params.getPageSize()));
        query.addFilterQuery(new SimpleFilterQuery(buildFilters(params)));

        Page<U> page = getTemplate().query(query, getUType());
        return page.getTotalElements();
    }

    /**
     * Builds an IN query.
     *
     * <p>
     *     Tests if the attribute of an instance is found in provided {@link Set} of values. If the {@link Set}
     *     is empty, then this query is silently ignored.
     * </p>
     * <p>
     *     Used internally in {@link SolrStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name Name of the attribute to check
     * @param values {@link Set} of valid values
     * @return Solr query builder
     */
    default Criteria inQuery(String name, Set<?> values) {
        if (!values.isEmpty()) {
            return Criteria.where(name).in(values);
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
     *     Used internally in {@link SolrStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name Name of the attribute to check
     * @param values {@link Set} of invalid values
     * @return Solr query builder
     */
    default Criteria notInQuery(String name, Set<?> values) {
        if (!values.isEmpty()) {
            return Criteria.where(name).in(values).notOperator();
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
     *     Used internally in {@link SolrStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name Name of the attribute to check
     * @param value Value to test against
     * @return Solr query builder
     */
    default Criteria prefixQuery(String name, String value) {
        return Criteria.where(name).startsWith(value);
    }

    /**
     * Builds a string suffix query.
     *
     * <p>
     *     Tests if the attribute of an instance ends with the specified value.
     * </p>
     * <p>
     *     Used internally in {@link SolrStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     * <p>
     *     This query is considerably slower than prefix query due to the nature of indexing. Should be avoided if
     *     possible.
     * </p>
     *
     * @param name Name of the attribute to check
     * @param value Value to test against
     * @return Solr query builder
     */
    default Criteria suffixQuery(String name, String value) {
        return Criteria.where(name).endsWith(value);
    }

    /**
     * Builds a string contains query.
     *
     * <p>
     *     Tests if the attribute of an instance contains the specified value.
     * </p>
     * <p>
     *     Used internally in {@link SolrStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     * <p>
     *     This query is considerably slower than prefix query due to the nature of indexing. Should be avoided if
     *     possible.
     * </p>
     *
     * @param name Name of the attribute to check
     * @param value Value to test against
     * @return Solr query builder
     */
    default Criteria containsQuery(String name, String value) {
        return Criteria.where(name).contains(value);
    }

    /**
     * Builds a greater than query.
     *
     * <p>
     *     Tests if the attribute of an instance is greater than the specified value. Applicable to number and date
     *     attributes.
     * </p>
     * <p>
     *     Used internally in {@link SolrStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name Name of the attribute to check
     * @param value Value to test against
     * @return Solr query builder
     */
    default Criteria gtQuery(String name, String value) {
        return Criteria.where(name).greaterThan(value);
    }

    /**
     * Builds a less than query.
     *
     * <p>
     *     Tests if the attribute of an instance is less than the specified value. Applicable to number and date
     *     attributes.
     * </p>
     * <p>
     *     Used internally in {@link SolrStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name Name of the attribute to check
     * @param value Value to test against
     * @return Solr query builder
     */
    default Criteria ltQuery(String name, String value) {
        return Criteria.where(name).lessThan(value);
    }

    /**
     * Builds a greater than or equal query.
     *
     * <p>
     *     Tests if the attribute of an instance is greater than or equal to the specified value. Applicable to number
     *     and date attributes.
     * </p>
     * <p>
     *     Used internally in {@link SolrStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name Name of the attribute to check
     * @param value Value to test against
     * @return Solr query builder
     */
    default Criteria gteQuery(String name, String value) {
        return Criteria.where(name).greaterThanEqual(value);
    }

    /**
     * Builds a less than or equal query.
     *
     * <p>
     *     Tests if the attribute of an instance is less than or equal to the specified value. Applicable to number
     *     and date attributes.
     * </p>
     * <p>
     *     Used internally in {@link SolrStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name Name of the attribute to check
     * @param value Value to test against
     * @return Solr query builder
     */
    default Criteria lteQuery(String name, String value) {
        return Criteria.where(name).lessThanEqual(value);
    }

    /**
     * Builds a dummy query, which does nothing.
     *
     * <p>
     *     Should be used instead of conditionally skipping query building.
     * </p>
     * <p>
     *     Used internally in {@link SolrStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @return Solr query builder
     */
    default Criteria nopQuery() {
        return new Criteria();
    }

    /**
     * Builds set query.
     *
     * <p>
     *     Tests if the attribute of an instance is set.
     * </p>
     * <p>
     *     Used internally in {@link SolrStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name Name of the attribute to check
     * @return Solr query builder
     */
    default Criteria notNullQuery(String name) {
        return Criteria.where(name).isNotNull();
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

    default Criteria negateQuery(List<Filter> filters) {
        return andQuery(filters).notOperator();
    }
    /**
     * Builds not set query.
     *
     * <p>
     *     Tests if the attribute of an instance is not set.
     * </p>
     * <p>
     *     Used internally in {@link SolrStore#findAll(Params)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name Name of the attribute to check
     * @return Solr query builder
     */
    default Criteria isNullQuery(String name) {
        return Criteria.where(name).isNull();
    }

    /**
     * Builds an OR query between sub-filters.
     *
     * <p>
     *     Used internally in {@link SolrStore#findAll(Params)} or in custom search methods in inheriting classes.
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
     *
     * <p>
     *     Used internally in {@link SolrStore#findAll(Params)} or in custom search methods in inheriting classes.
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

    default String sanitizeFilterValue(String value) {
        if (value != null && value.trim().length() > 0) {
            return value.trim();
        } else {
            return null;
        }
    }

    default Criteria buildFilter(Filter filter) {
        String value = sanitizeFilterValue(filter.getValue());
        FilterOperation operation = filter.getOperation();

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

        String normalizedValue = normalize(value);

        switch (operation) {
            case EQ:
            default:
                return inQuery(filter.getField(), asSet(normalizedValue));
            case NEQ:
                return notInQuery(filter.getField(), asSet(normalizedValue));
            case STARTWITH:
                return prefixQuery(filter.getField(), normalizedValue);
            case ENDWITH:
                return suffixQuery(filter.getField(), normalizedValue);
            case CONTAINS:
                return containsQuery(filter.getField(), normalizedValue);
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
            case NEGATE:
                return negateQuery(filter.getFilter());
        }
    }

    default Criteria buildFilters(Params params) {
        if (params.getFilter() == null) {
            return nopQuery();
        }

        List<Criteria> queries = params.getFilter().stream()
                .map(this::buildFilter)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return andQueryInternal(queries);
    }

    default Criteria andQueryInternal(List<Criteria> filters) {
        return filters.stream()
                      .reduce(new Criteria(), Criteria::and);
    }

    default Criteria orQueryInternal(List<Criteria> filters) {
        return filters.stream()
                      .reduce(new Criteria(), Criteria::or);
    }
}