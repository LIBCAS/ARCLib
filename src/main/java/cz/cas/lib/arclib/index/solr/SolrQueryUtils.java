package cz.cas.lib.arclib.index.solr;

import cz.cas.lib.arclib.index.solr.arclibxml.SolrArclibXmlStore;
import cz.cas.lib.core.exception.BadArgument;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.solr.SolrStore;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleQuery;

import java.util.List;
import java.util.Set;

import static cz.cas.lib.core.util.Utils.gte;
import static cz.cas.lib.core.util.Utils.notNull;

public class SolrQueryUtils {

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
     * <p>
     * Used internally in {@link SolrStore#findAll(Params)}, {@link SolrArclibXmlStore#findAll(Params, boolean)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name   Name of the attribute to check
     * @param values {@link Set} of valid values
     * @return Solr query builder
     */
    public static Criteria inQuery(String name, Set<?> values) {
        Criteria c = Criteria.where(name).in(values);
        if (values.isEmpty())
            return c.not();
        return c;
    }

    /**
     * Builds a NOT IN query.
     * <p>
     * <p>
     * Tests if the attribute of an instance is not found in provided {@link Set} of values.
     * </p>
     * <p>
     * Used internally in {@link SolrStore#findAll(Params)}, {@link SolrArclibXmlStore#findAll(Params, boolean)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name   Name of the attribute to check
     * @param values {@link Set} of invalid values
     * @return Solr query builder
     */
    public static Criteria notInQuery(String name, Set<?> values) {
        Criteria c = Criteria.where(name).in(values);
        if (!values.isEmpty())
            return c.not();
        return c;
    }

    /**
     * Builds a string prefix query.
     * <p>
     * Tests if the attribute of an instance starts with the specified value.
     * </p>
     * <p>
     * Used internally in {@link SolrArclibXmlStore#findAll(Params, boolean)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name  Name of the attribute to check
     * @param value Value to test against
     * @return Solr query builder
     */
    public static Criteria prefixQuery(String name, String value) {
        return Criteria.where(name).startsWith(value);
    }

    /**
     * Builds a string suffix query.
     * <p>
     * Tests if the attribute of an instance ends with the specified value.
     * </p>
     * <p>
     * Used internally in {@link SolrArclibXmlStore#findAll(Params, boolean)} or in custom search methods in inheriting classes.
     * </p>
     * <p>
     * This query is considerably slower than prefix query due to the nature of indexing. Should be avoided if
     * possible.
     * </p>
     *
     * @param name  Name of the attribute to check
     * @param value Value to test against
     * @return Solr query builder
     */
    public static Criteria suffixQuery(String name, String value) {
        return Criteria.where(name).endsWith(value);
    }

    /**
     * Builds a string contains query.
     * <p>
     * Tests if the attribute of an instance contains the specified value.
     * </p>
     * <p>
     * Used internally in {@link SolrArclibXmlStore#findAll(Params, boolean)} or in custom search methods in inheriting classes.
     * </p>
     * <p>
     * This query is considerably slower than prefix query due to the nature of indexing. Should be avoided if
     * possible.
     * </p>
     *
     * @param name  Name of the attribute to check
     * @param value Value to test against
     * @return Solr query builder
     */
    public static Criteria containsQuery(String name, String value) {
        return Criteria.where(name).contains(value);
    }

    /**
     * Builds a greater than query.
     * <p>
     * Tests if the attribute of an instance is greater than the specified value. Applicable to number and date
     * fields.
     * </p>
     * <p>
     * Used internally in {@link SolrArclibXmlStore#findAll(Params, boolean)} or in custom search methods in inheriting classes.
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
     * <p>
     * Used internally in {@link SolrArclibXmlStore#findAll(Params, boolean)} or in custom search methods in inheriting classes.
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
     * <p>
     * Used internally in {@link SolrArclibXmlStore#findAll(Params, boolean)} or in custom search methods in inheriting classes.
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
     * <p>
     * Used internally in {@link SolrArclibXmlStore#findAll(Params, boolean)} or in custom search methods in inheriting classes.
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
     * <p>
     * Used internally in {@link SolrArclibXmlStore#findAll(Params, boolean)} or in custom search methods in inheriting classes.
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
     * <p>
     * Used internally in {@link SolrArclibXmlStore#findAll(Params, boolean)} or in custom search methods in inheriting classes.
     * </p>
     *
     * @param name Name of the attribute to check
     * @return Solr query builder
     */
    public static Criteria isNullQuery(String name) {
        return Criteria.where(name).isNull();
    }

    public static Criteria orQueryInternal(List<Criteria> filters) {
        if (filters.isEmpty())
            return Criteria.where("id");
        return filters.stream().skip(1).reduce(filters.get(0), Criteria::or).connect();
    }

    public static Criteria andQueryInternal(List<Criteria> filters) {
        if (filters.isEmpty())
            return Criteria.where("id");
        return filters.stream().skip(1).reduce(filters.get(0), Criteria::and).connect();
    }

    public static SimpleQuery initializeQuery(Params params) {
        notNull(params.getSort(), () -> new BadArgument("sort"));
        notNull(params.getOrder(), () -> new BadArgument("order"));
        notNull(params.getPage(), () -> new BadArgument("page"));
        notNull(params.getPageSize(), () -> new BadArgument("pageSize"));
        gte(params.getPage(), 0, () -> new BadArgument("page"));
        if (params.getPageSize() != null) {
            gte(params.getPageSize(), 1, () -> new BadArgument("pageSize"));
        }
        SimpleQuery query = new SimpleQuery();
        if (params.getSorting() != null && !params.getSorting().isEmpty()) {
            Sort s = new Sort(new Sort.Order(Sort.Direction.valueOf(params.getSorting().get(0).getOrder().toString()), params.getSorting().get(0).getSort()));
            for (int i = 1; i < params.getSorting().size(); i++) {
                s = s.and(new Sort(new Sort.Order(Sort.Direction.valueOf(params.getSorting().get(i).getOrder().toString()), params.getSorting().get(i).getSort())));
            }
            query.addSort(s);
        } else {
            query.addSort(new Sort(new Sort.Order(Sort.Direction.valueOf(params.getOrder().toString()), params.getSort())));
        }
        query.setPageRequest(new PageRequest(params.getPage(), params.getPageSize()));
        return query;
    }
}
