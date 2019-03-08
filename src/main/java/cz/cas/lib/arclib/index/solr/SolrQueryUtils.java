package cz.cas.lib.arclib.index.solr;

import cz.cas.lib.core.exception.BadArgument;
import cz.cas.lib.core.index.dto.Params;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.SimpleStringCriteria;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
     *
     * @param name   Name of the attribute to check
     * @param values {@link Set} of valid values
     * @return Solr query builder
     */
    public static Criteria inQuery(String name, Set<?> values) {
        if (values.isEmpty())
            return new SimpleStringCriteria("(-*:*)");
        return Criteria.where(name).in(values);
    }

    /**
     * Builds a NOT IN query.
     * <p>
     * <p>
     * Tests if the attribute of an instance is not found in provided {@link Set} of values.
     * </p>
     *
     * @param name   Name of the attribute to check
     * @param values {@link Set} of invalid values
     * @return Solr query builder
     */
    public static Criteria notInQuery(String name, Set<?> values) {
        String join = String.join(" ", values.stream().map(o -> "\"" + o + "\"").collect(Collectors.toList()));
        if (!values.isEmpty())
            return new SimpleStringCriteria("(*:* -" + name + ":(" + join + "))");
        return new SimpleStringCriteria("(*:*)");
    }

    /**
     * Builds a string prefix query.
     * <p>
     * Tests if the attribute of an instance starts with the specified value.
     * </p>
     * <p>
     * WARNING: if value contains multiple strings separated by space, the strings should be full words and query should
     * be used only field types originating from solr.TextField (text_general, folding ...) not on field type string     * </p>
     *
     * @param name  Name of the attribute to check
     * @param value Value to test against
     * @return Solr query builder
     */
    public static Criteria prefixQuery(String name, String value) {
        if (value.trim().contains(" "))
            return new SimpleStringCriteria(name + ":\"^" + value + "\"");
        return Criteria.where(name).startsWith(value);
    }

    /**
     * Builds a string suffix query.
     * <p>
     * Tests if the attribute of an instance ends with the specified value.
     * </p>
     * <p>
     * WARNING: if value contains multiple strings separated by space, the strings should be full words and query should
     * be used only field types originating from solr.TextField (text_general, folding ...) not on field type string     * </p>
     *
     * @param name  Name of the attribute to check
     * @param value Value to test against
     * @return Solr query builder
     */
    public static Criteria suffixQuery(String name, String value) {
        if (value.trim().contains(" "))
            return new SimpleStringCriteria(name + ":\"" + value + "$\"");
        return Criteria.where(name).endsWith(value);
    }

    /**
     * Builds a string contains query.
     * <p>
     * Tests if the attribute of an instance contains the specified value.
     * </p>
     * <p>
     * WARNING: if value contains multiple strings separated by space, the strings should be full words and query should
     * be used only field types originating from solr.TextField (text_general, folding ...) not on field type string
     * </p>
     *
     * @param name  Name of the attribute to check
     * @param value Value to test against
     * @return Solr query builder
     */
    public static Criteria containsQuery(String name, String value) {
        if (value.trim().contains(" "))
            return new SimpleStringCriteria(name + ":\"" + value + "\"");
        return Criteria.where(name).contains(value);
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
        if (params.getPageSize() != null && params.getPageSize() > 0) {
            notNull(params.getPage(), () -> new BadArgument("page"));
            gte(params.getPage(), 0, () -> new BadArgument("page"));
            query.setPageRequest(new PageRequest(params.getPage(), params.getPageSize()));
        }
        return query;
    }
}
