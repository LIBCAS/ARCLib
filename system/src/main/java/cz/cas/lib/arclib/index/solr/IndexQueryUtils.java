package cz.cas.lib.arclib.index.solr;

import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.index.ArclibXmlIndexTypeConfig;
import cz.cas.lib.core.index.UnsupportedSearchParameterException;
import cz.cas.lib.core.index.dto.*;
import cz.cas.lib.core.index.solr.IndexField;
import cz.cas.lib.core.index.solr.IndexFieldType;
import cz.cas.lib.core.index.solr.IndexedDomainObject;
import cz.cas.lib.core.index.solr.IndexedStore;
import cz.cas.lib.core.index.solr.util.NestedCriteria;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.DefaultQueryParser;
import org.springframework.data.solr.core.mapping.SimpleSolrMappingContext;
import org.springframework.data.solr.core.query.AnyCriteria;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.SimpleStringCriteria;

import java.util.*;
import java.util.stream.Collectors;

import static cz.cas.lib.core.util.Utils.*;

public class IndexQueryUtils {

    public static final DefaultQueryParser queryParser = new DefaultQueryParser(new SimpleSolrMappingContext());
    public static final String TYPE_FIELD = "index_type";

    /**
     * Name of autocomplete field defined in {@link IndexedDomainObject#autoCompleteLabel}.
     */
    public static final String AUTOCOMPLETE_FIELD_NAME = "autoCompleteLabel";

    /**
     * <p>map containing {@link IndexQueryUtils#TYPE_FIELD} as key and fields configuration as value</p>
     * <p>fields configuration is a map which contains field name as key and its index config as value</p>
     * <p>every {@link IndexedStore} must have unique value of {@link IndexedStore#getIndexType()}, the same applies for every custom index
     * store and its index types, e.g. {@link ArclibXmlIndexTypeConfig#indexType}</p>
     */
    public static Map<String, Map<String, IndexField>> INDEXED_FIELDS_MAP = new HashMap<>();
    /**
     * Page size maximum configuration property.
     * If application property {@code 'solr.maxRows'} is set then default value is overridden by PostInitializer.
     */
    public static int solrMaxRows = Integer.MAX_VALUE;

    public static String sanitizeFilterValue(String value) {
        if (value != null && value.trim().length() > 0) {
            return value.trim();
        } else {
            return null;
        }
    }

    /**
     * Builds an IN query.
     * <p>
     * <p>
     * Tests if the attribute of an instance is found in provided {@link Set} of values.
     * </p>
     *
     * @param indexedField field to check
     * @param values       {@link Set} of valid values
     * @return Solr query builder
     */
    public static Criteria inQuery(IndexField indexedField, Set<?> values) {
        if (values.isEmpty())
            return new SimpleStringCriteria("(-*:*)");
        if (indexedField.getKeywordField() == null)
            throw new UnsupportedSearchParameterException("equality check not supported for field: " + indexedField.getFieldName());
        return Criteria.where(indexedField.getKeywordField()).in(values);
    }

    /**
     * Builds a NOT IN query.
     * <p>
     * <p>
     * Tests if the attribute of an instance is not found in provided {@link Set} of values.
     * </p>
     *
     * @param indexedField field to check
     * @param values       {@link Set} of invalid values
     * @return Solr query builder
     */
    public static Criteria notInQuery(IndexField indexedField, Set<?> values) {
        String join = String.join(" ", values.stream().map(o -> "\"" + o + "\"").collect(Collectors.toList()));
        if (!values.isEmpty()) {
            if (indexedField.getKeywordField() == null)
                throw new UnsupportedSearchParameterException("equality check not supported for field: " + indexedField.getFieldName());
            return new SimpleStringCriteria("(*:* -" + indexedField.getKeywordField() + ":(" + join + "))");
        }
        return new SimpleStringCriteria("(*:*)");
    }

    /**
     * Builds a string prefix query.
     * <p>
     * Tests if the attribute of an instance starts with the specified value.
     * </p>
     *
     * @param indexedField field to check
     * @param value        Value to test against
     * @return Solr query builder
     */
    public static Criteria prefixQuery(IndexField indexedField, String value) {
        if (indexedField.getKeywordField() == null)
            throw new UnsupportedSearchParameterException("prefix query not supported for field: " + indexedField.getFieldName());
        return new SimpleStringCriteria(indexedField.getKeywordField() + ":" + value.replace(" ", "\\ ") + "*");
    }

    /**
     * Builds a string suffix query.
     * <p>
     * Tests if the attribute of an instance ends with the specified value.
     * </p>
     *
     * @param indexedField field to check
     * @param value        Value to test against
     * @return Solr query builder
     */
    public static Criteria suffixQuery(IndexField indexedField, String value) {
        if (indexedField.getKeywordField() == null)
            throw new UnsupportedSearchParameterException("suffix query not supported for field: " + indexedField.getFieldName());
        return new SimpleStringCriteria(indexedField.getKeywordField() + ":*" + value.replace(" ", "\\ "));
    }


    /**
     * Builds a string contains query.
     * <p>
     * Tests if the attribute of an instance contains the specified value.
     * </p>
     *
     * @param indexedField field to check
     * @param value        Value to test against
     * @return Solr query builder
     */
    public static Criteria containsQuery(IndexField indexedField, String value) {
        String fieldName = indexedField.getFieldName();
        switch (indexedField.getFieldType()) {
            case IndexFieldType.FOLDING:
            case IndexFieldType.STRING:
                return new SimpleStringCriteria(fieldName + ":*" + value.replace(" ", "\\ ") + "*");
            case IndexFieldType.TEXT:
                return new SimpleStringCriteria(fieldName + ":\"" + value + "\"");
            default:
                throw new UnsupportedSearchParameterException();
        }
    }

    /**
     * Builds a greater than query.
     * <p>
     * Tests if the attribute of an instance is greater than the specified value. Applicable to number and date
     * fields.
     * </p>
     *
     * @param name  Name of the attribute to check
     * @param value Value to test against
     * @return Solr query builder
     */
    public static Criteria gtQuery(String name, String value) {
        return Criteria.where(name).greaterThan(value);
    }

    /**
     * Builds a less than query.
     * <p>
     * Tests if the attribute of an instance is less than the specified value. Applicable to number and date
     * fields.
     * </p>
     *
     * @param name  Name of the attribute to check
     * @param value Value to test against
     * @return Solr query builder
     */
    public static Criteria ltQuery(String name, String value) {
        return Criteria.where(name).lessThan(value);
    }

    /**
     * Builds a greater than or equal query.
     * <p>
     * Tests if the attribute of an instance is greater than or equal to the specified value. Applicable to number
     * and date fields.
     * </p>
     *
     * @param name  Name of the attribute to check
     * @param value Value to test against
     * @return Solr query builder
     */
    public static Criteria gteQuery(String name, String value) {
        return Criteria.where(name).greaterThanEqual(value);
    }

    /**
     * Builds a less than or equal query.
     * <p>
     * Tests if the attribute of an instance is less than or equal to the specified value. Applicable to number
     * and date fields.
     * </p>
     *
     * @param name  Name of the attribute to check
     * @param value Value to test against
     * @return Solr query builder
     */
    public static Criteria lteQuery(String name, String value) {
        return Criteria.where(name).lessThanEqual(value);
    }

    /**
     * Builds set query.
     * <p>
     * Tests if the attribute of an instance is set.
     * </p>
     *
     * @param name Name of the attribute to check
     * @return Solr query builder
     */
    public static Criteria notNullQuery(String name) {
        return Criteria.where(name).isNotNull();
    }

    /**
     * Builds not set query.
     * <p>
     * Tests if the attribute of an instance is not set.
     * </p>
     *
     * @param name Name of the attribute to check
     * @return Solr query builder
     */
    public static Criteria isNullQuery(String name) {
        return new SimpleStringCriteria("(*:* -" + name + ":[* TO *])");
    }

    /**
     * Builds an OR query between sub-filters.
     * <p>
     * Used internally in {@link IndexedStore#findAll(Params)} )} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param filters {@link List} of {@link Filter}
     * @return Solr query builder
     */
    public static Criteria orQuery(List<Filter> filters, String indexType, Map<String, IndexField> indexedFields) {
        List<Criteria> builders = filters.stream()
                .map(f -> IndexQueryUtils.buildFilter(f, indexType, indexedFields))
                .collect(Collectors.toList());
        return orQueryInternal(builders);
    }

    public static Criteria orQueryInternal(List<Criteria> filters) {
        if (filters.isEmpty())
            return AnyCriteria.any();
        return filters.stream().skip(1).reduce(filters.get(0), Criteria::or).connect();
    }

    /**
     * Builds an AND query between sub-filters.
     * <p>
     * Used internally in {@link IndexedStore#findAll(Params)} )} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param filters {@link List} of {@link Filter}
     * @return Solr query builder
     */
    public static Criteria andQuery(List<Filter> filters, String indexType, Map<String, IndexField> indexedFields) {
        List<Criteria> builders = filters.stream()
                .map(f -> IndexQueryUtils.buildFilter(f, indexType, indexedFields))
                .collect(Collectors.toList());
        return andQueryInternal(builders);
    }

    public static Criteria andQueryInternal(List<Criteria> filters) {
        if (filters.isEmpty())
            return AnyCriteria.any();
        return filters.stream().skip(1).reduce(filters.get(0), Criteria::and).connect();
    }

    public static Criteria negateQuery(List<Filter> filters, String indexType, Map<String, IndexField> indexedFields) {
        String queryTobeNegated = queryParser.createQueryStringFromNode(andQuery(filters, indexType, indexedFields).notOperator(), null);
        return new SimpleStringCriteria("*:* " + queryTobeNegated);
    }

    /**
     * @param childIndexType type of the children collection, i.e. value identical to result of calling {@link IndexedDomainObject#getType()} on the
     *                       child object
     * @param filters        filters applied to the the child collection
     * @param indexType      type of the parent object
     */
    public static Criteria nestedQuery(String childIndexType, List<Filter> filters, String indexType) {
        Criteria parentCriteria = Criteria.where(IndexQueryUtils.TYPE_FIELD).is(indexType);
        Map<String, IndexField> childIndexedFields = INDEXED_FIELDS_MAP.get(childIndexType);
        if (childIndexedFields == null)
            throw new UnsupportedSearchParameterException("unknown child index type: " + childIndexType);
        Criteria childrenCriteria = andQuery(filters, indexType, childIndexedFields).and(Criteria.where(IndexQueryUtils.TYPE_FIELD).is(childIndexType));
        return new NestedCriteria(parentCriteria, childrenCriteria);
    }

    public static void initializeQuery(SimpleQuery query, Params params, Map<String, IndexField> indexedFields) {
        if (params.getSorting() != null && !params.getSorting().isEmpty()) {
            Sort s = Sort.unsorted();
            for (int i = 0; i < params.getSorting().size(); i++) {
                String sortField;
                SortSpecification sortSpecification = params.getSorting().get(i);
                if ("score".equals(sortSpecification.getSort()))
                    sortField = "score";
                else {
                    IndexField field = indexedFields.get(sortSpecification.getSort());
                    notNull(field, () -> new UnsupportedSearchParameterException("sort field: " + sortSpecification.getSort() + " not mapped"));
                    sortField = field.getSortField();
                    notNull(sortField, () -> new UnsupportedSearchParameterException("sort is not supported on field type: " + field.getFieldType() + " consider adding " + IndexField.STRING_SUFFIX + " copy field"));
                }
                s = s.and(Sort.by(Sort.Direction.valueOf(sortSpecification.getOrder().toString()), sortField));
            }
            query.addSort(s);
        } else {
            notNull(params.getSort(), () -> new BadArgument("sort"));
            notNull(params.getOrder(), () -> new BadArgument("order"));
            String sortField;
            if ("score".equals(params.getSort()))
                sortField = "score";
            else {
                IndexField field = indexedFields.get(params.getSort());
                notNull(field, () -> new UnsupportedSearchParameterException("sort field: " + params.getSort() + " not mapped"));
                sortField = field.getSortField();
                notNull(sortField, () -> new UnsupportedSearchParameterException("sort is not supported on field type: " + field.getFieldType() + " consider adding " + IndexField.STRING_SUFFIX + " copy field"));
            }
            query.addSort(Sort.by(Sort.Direction.valueOf(params.getOrder().toString()), sortField));
        }
        notNull(params.getPageSize(), () -> new BadArgument("pageSize can't be null"));
        gte(params.getPageSize(), 1, () -> new BadArgument("pageSize must be >= 1"));
        lte(params.getPageSize(), solrMaxRows, () -> new BadArgument("pageSize must be lesser than solrMaxRows config property: " + solrMaxRows));
        notNull(params.getPage(), () -> new BadArgument("page can't be null"));
        gte(params.getPage(), 0, () -> new BadArgument("page must be >= 0"));
        query.setPageRequest(PageRequest.of(params.getPage(), params.getPageSize()));
    }

    public static Criteria buildFilters(Params params, String indexType, Map<String, IndexField> indexedFields) {
        if (params.getFilter() == null || params.getFilter().isEmpty())
            return AnyCriteria.any();
        List<Criteria> queries = params.getFilter().stream()
                .map(f -> IndexQueryUtils.buildFilter(f, indexType, indexedFields))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (params.getOperation() == RootFilterOperation.OR) {
            return orQueryInternal(queries);
        } else {
            return andQueryInternal(queries);
        }
    }

    public static Criteria buildFilter(Filter filter, String indexType, Map<String, IndexField> indexedFields) {
        String value = sanitizeFilterValue(filter.getValue());
        FilterOperation operation = filter.getOperation();
        if (operation == null) {
            throw new BadArgument("operation not specified: " + filter);
        }
        boolean logicalFilter = operation == FilterOperation.AND
                || operation == FilterOperation.OR
                || operation == FilterOperation.NESTED
                || operation == FilterOperation.NEGATE;
        boolean nullFilter = operation == FilterOperation.IS_NULL || operation == FilterOperation.NOT_NULL;
        if (!logicalFilter && !nullFilter && value == null) {
            throw new BadArgument("value not specified: " + filter);
        }
        IndexField field = null;
        if (!logicalFilter) {
            field = indexedFields.get(filter.getField());
            if (field == null)
                throw new UnsupportedSearchParameterException("field: " + filter.getField() + " not mapped in index of: " + indexType);
        }
        switch (operation) {
            case EQ:
            default:
                return inQuery(field, asSet(value));
            case IN:
                return inQuery(field, asSet(value.split(",")));
            case NEQ:
                return notInQuery(field, asSet(value));
            case STARTWITH:
                return prefixQuery(field, value);
            case ENDWITH:
                return suffixQuery(field, value);
            case CONTAINS:
                return containsQuery(field, value);
            case GT:
                return gtQuery(field.getFieldName(), value);
            case LT:
                return ltQuery(field.getFieldName(), value);
            case GTE:
                return gteQuery(field.getFieldName(), value);
            case LTE:
                return lteQuery(field.getFieldName(), value);
            case AND:
                return andQuery(filter.getFilter(), indexType, indexedFields);
            case OR:
                return orQuery(filter.getFilter(), indexType, indexedFields);
            case IS_NULL:
                return isNullQuery(field.getFieldName());
            case NOT_NULL:
                return notNullQuery(field.getFieldName());
            case NESTED:
                //nested objects must be in the same collection as parent object
                if (!INDEXED_FIELDS_MAP.containsKey(filter.getField()))
                    throw new UnsupportedSearchParameterException("unknown child index of type: " + filter.getField());
                return nestedQuery(filter.getField(), filter.getFilter(), indexType);
            case NEGATE:
                return negateQuery(filter.getFilter(), indexType, indexedFields);
        }
    }
}
