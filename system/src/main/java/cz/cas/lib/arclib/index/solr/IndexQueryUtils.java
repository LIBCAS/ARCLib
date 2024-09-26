package cz.cas.lib.arclib.index.solr;

import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.index.ArclibXmlIndexTypeConfig;
import cz.cas.lib.core.index.UnsupportedSearchParameterException;
import cz.cas.lib.core.index.dto.*;
import cz.cas.lib.core.index.solr.IndexField;
import cz.cas.lib.core.index.solr.IndexFieldType;
import cz.cas.lib.core.index.solr.IndexedDomainObject;
import cz.cas.lib.core.index.solr.IndexedStore;
import org.apache.solr.client.solrj.SolrQuery;

import java.util.*;
import java.util.stream.Collectors;

import static cz.cas.lib.core.util.Utils.*;

public class IndexQueryUtils {

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
     * @return Solr query
     */

    public static String inQuery(IndexField indexedField, Set<?> values) {
        if (values.isEmpty())
            return "(-*:*)";
        if (indexedField.getKeywordField() == null)
            throw new UnsupportedSearchParameterException("equality check not supported for field: " + indexedField.getFieldName());
        String result = indexedField.getKeywordField() + ":(" + extractValues(values) + ")";
        return result;
    }

    private static String extractValues(Collection<?> values) {
        StringBuilder valuesStr = new StringBuilder();
        boolean first = true;
        for (Object value : values) {
            if (!first) {
                valuesStr.append(" ");
            }
            if (value instanceof Collection) {
                valuesStr.append(extractValues((Collection<?>) value));
            } else {
                valuesStr.append(value);
            }
        }
        return valuesStr.toString();
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
     * @return Solr query
     */
    public static String notInQuery(IndexField indexedField, Set<?> values) {
        String join = values.stream().map(o -> "\"" + o + "\"").collect(Collectors.joining(" "));
        if (!values.isEmpty()) {
            if (indexedField.getKeywordField() == null)
                throw new UnsupportedSearchParameterException("equality check not supported for field: " + indexedField.getFieldName());
            return "(*:* -" + indexedField.getKeywordField() + ":(" + join + "))";
        }
        return "(*:*)";
    }

    /**
     * Builds a string prefix query.
     * <p>
     * Tests if the attribute of an instance starts with the specified value.
     * </p>
     *
     * @param indexedField field to check
     * @param value        Value to test against
     * @return Solr query
     */
    public static String prefixQuery(IndexField indexedField, String value) {
        if (indexedField.getKeywordField() == null)
            throw new UnsupportedSearchParameterException("prefix query not supported for field: " + indexedField.getFieldName());
        return indexedField.getKeywordField() + ":" + value.replace(" ", "\\ ") + "*";
    }

    /**
     * Builds a string suffix query.
     * <p>
     * Tests if the attribute of an instance ends with the specified value.
     * </p>
     *
     * @param indexedField field to check
     * @param value        Value to test against
     * @return Solr query
     */
    public static String suffixQuery(IndexField indexedField, String value) {
        if (indexedField.getKeywordField() == null)
            throw new UnsupportedSearchParameterException("suffix query not supported for field: " + indexedField.getFieldName());
        return indexedField.getKeywordField() + ":*" + value.replace(" ", "\\ ");
    }


    /**
     * Builds a string contains query.
     * <p>
     * Tests if the attribute of an instance contains the specified value.
     * </p>
     *
     * @param indexedField field to check
     * @param value        Value to test against
     * @return Solr query
     */
    public static String containsQuery(IndexField indexedField, String value) {
        String fieldName = indexedField.getFieldName();
        switch (indexedField.getFieldType()) {
            case IndexFieldType.FOLDING:
            case IndexFieldType.STRING:
                return fieldName + ":*" + value.replace(" ", "\\ ") + "*";
            case IndexFieldType.TEXT:
                return fieldName + ":\"" + value + "\"";
            default:
                throw new UnsupportedSearchParameterException();
        }
    }

    /**
     * Builds set query.
     * <p>
     * Tests if the attribute of an instance is set.
     * </p>
     *
     * @param name Name of the attribute to check
     * @return Solr query
     */
    public static String notNullQuery(String name) {
        return name + ":[* TO *])";
    }

    /**
     * Builds not set query.
     * <p>
     * Tests if the attribute of an instance is not set.
     * </p>
     *
     * @param name Name of the attribute to check
     * @return Solr query
     */
    public static String isNullQuery(String name) {
        return "(*:* -" + name + ":[* TO *])";
    }

    /**
     * Builds an OR query between sub-filters.
     * <p>
     * Used internally in {@link IndexedStore#findAll(Params)} )} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param filters {@link List} of {@link Filter}
     * @return Solr query
     */
    public static String orQuery(List<Filter> filters, String indexType, Map<String, IndexField> indexedFields) {
        List<String> filterStrings = filters.stream()
                .map(f -> IndexQueryUtils.buildFilter(f, indexType, indexedFields))
                .collect(Collectors.toList());
        return orQueryInternal(filterStrings);
    }

    public static String orQueryInternal(List<String> filters) {
        if (filters.isEmpty())
            return "*:*";
        if (filters.size() == 1) {
            return filters.get(0);
        }
        String compound = filters.get(0) + " OR " + andQueryInternal(filters.subList(1, filters.size()));
//        if (isRoot) {
//            return compound;
//        }
        return "(" + compound + ")";
    }

    /**
     * Builds an AND query between sub-filters.
     * <p>
     * Used internally in {@link IndexedStore#findAll(Params)} )} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param filters {@link List} of {@link Filter}
     * @return Solr query
     */
    public static String andQuery(List<Filter> filters, String indexType, Map<String, IndexField> indexedFields) {
        List<String> filterStrings = filters.stream()
                .map(f -> IndexQueryUtils.buildFilter(f, indexType, indexedFields))
                .collect(Collectors.toList());
        return andQueryInternal(filterStrings);
    }

    public static String andQueryInternal(List<String> filters) {
        if (filters.isEmpty())
            return "*:*";
        if (filters.size() == 1) {
            return filters.get(0);
        }
        String compound = filters.get(0) + " AND " + andQueryInternal(filters.subList(1, filters.size()));
//        if (isRoot) {
//            return compound;
//        }
        return "(" + compound + ")";
    }

    public static String negateQuery(List<Filter> filters, String indexType, Map<String, IndexField> indexedFields) {
        return "*:* -" + andQuery(filters, indexType, indexedFields);
    }

    /**
     * @param childIndexType type of the children collection, i.e. value identical to result of calling {@link IndexedDomainObject#getType()} on the
     *                       child object
     * @param filters        filters applied to the child collection
     * @param indexType      type of the parent object
     */
    public static String nestedQuery(String childIndexType, List<Filter> filters, String indexType) {
        String parentCriteria = IndexQueryUtils.TYPE_FIELD + ":" + indexType;
        Map<String, IndexField> childIndexedFields = INDEXED_FIELDS_MAP.get(childIndexType);
        if (childIndexedFields == null)
            throw new UnsupportedSearchParameterException("unknown child index type: " + childIndexType);
        String childrenCriteria = "(" + andQuery(filters, indexType, childIndexedFields) + " AND " + IndexQueryUtils.TYPE_FIELD + ":" + childIndexType + ")";
        return "({!parent which=" + parentCriteria + " score=max v='" + childrenCriteria + "'})";
    }

    public static void initializeQuery(SolrQuery query, Params params, Map<String, IndexField> indexedFields) {
        if (params.getSorting() != null && !params.getSorting().isEmpty()) {
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
                if (sortSpecification.getOrder() == Order.ASC) {
                    query.addSort(SolrQuery.SortClause.asc(sortField));
                } else {
                    query.addSort(SolrQuery.SortClause.desc(sortField));
                }
            }
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
            if (params.getOrder() == Order.ASC) {
                query.addSort(SolrQuery.SortClause.asc(sortField));
            } else {
                query.addSort(SolrQuery.SortClause.desc(sortField));
            }
        }
        notNull(params.getPageSize(), () -> new BadArgument("pageSize can't be null"));
        gte(params.getPageSize(), 1, () -> new BadArgument("pageSize must be >= 1"));
        lte(params.getPageSize(), solrMaxRows, () -> new BadArgument("pageSize must be lesser than solrMaxRows config property: " + solrMaxRows));
        notNull(params.getPage(), () -> new BadArgument("page can't be null"));
        gte(params.getPage(), 0, () -> new BadArgument("page must be >= 0"));
        query.setRows(params.getPageSize());
        query.setStart(params.getPage() * params.getPageSize());
    }

    public static String buildFilters(Params params, String indexType, Map<String, IndexField> indexedFields) {
        if (params.getFilter() == null || params.getFilter().isEmpty())
            return "*:*";
        List<String> queries = new ArrayList<>();
        for (Filter filter : params.getFilter()) {
            String query = IndexQueryUtils.buildFilter(filter, indexType, indexedFields);
            if (query != null) {
                queries.add(query);
            }
        }
        if (params.getOperation() == RootFilterOperation.OR) {
            return orQueryInternal(queries);
        } else {
            return andQueryInternal(queries);
        }
    }

    public static String buildFilter(Filter filter, String indexType, Map<String, IndexField> indexedFields) {
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
                return field.getFieldName() + ":{" + value + " TO *]";
            case LT:
                return field.getFieldName() + ":[* TO " + value + "}";
            case GTE:
                return field.getFieldName() + ":[" + value + " TO *]";
            case LTE:
                return field.getFieldName() + ":[* TO " + value + "]";
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
