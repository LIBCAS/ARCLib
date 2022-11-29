package cz.cas.lib.core.index.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.cas.lib.arclib.index.solr.IndexQueryUtils;
import cz.cas.lib.core.index.solr.IndexedStore;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.solr.core.query.Criteria;

import javax.persistence.Transient;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Data transfer object for specification of filtering, sorting and paging.
 *
 * <p>
 * Sorting is specified by name of attribute to sort on {@link Params#sort} and ascending or descending order
 * specified by {@link Params#order}.
 * </p>
 * <p>
 * Paging is specified by number of items to retrieve {@link Params#pageSize} and the page to start
 * on {@link Params#page}.
 * </p>
 * <p>
 * Filtering is specified by a {@link List} of filters {@link Params#filter}.
 * {@link IndexedStore} does AND between individual filters.
 * </p>
 */
@Getter
@Setter
public class Params {
    /**
     * Attribute name to sort on.
     */
    @NotNull
    protected String sort = "created";

    /**
     * Order of sorting.
     *
     * <p>
     * For possible values see {@link Order}.
     * </p>
     */
    @NotNull
    protected Order order = Order.DESC;

    /**
     * Support for sorting on multiple values, if this is not null, fields sort and order above are ignored.
     */
    protected List<SortSpecification> sorting = new ArrayList<>();

    /**
     * Initial page.
     */
    @NotNull
    protected Integer page = 0;

    /**
     * Number of requested instances.
     * <p>
     * Minimum is 1 and maximum is configured by {@link IndexQueryUtils#solrMaxRows}
     */
    @NotNull
    @Min(1)
    protected Integer pageSize = 10;

    /**
     * Logic operation between root filters
     */
    @NotNull
    protected RootFilterOperation operation = RootFilterOperation.AND;

    /**
     * Filter conditions.
     */
    @Valid
    protected List<Filter> filter = new ArrayList<>();

    @JsonIgnore
    @Transient
    protected boolean prefilterAdded = false;

    /**
     * Internal query.
     * <p>
     * Used for additional complicated queries added on backend.
     * </p>
     * <p>
     * This will be used in query field, not filter query. Boost will work.
     * </p>
     */
    @JsonIgnore
    @Transient
    protected Criteria internalQuery;

    public void addFilter(Filter filter) {
        List<Filter> newList = new ArrayList<>(getFilter());
        newList.add(filter);
        setFilter(newList);
    }

    public void addSorting(SortSpecification sort) {
        this.sorting.add(sort);
    }

    public Params copy() {
        Params params = new Params();
        params.setPage(getPage());
        params.setPageSize(getPageSize());
        params.setFilter(getFilter());
        params.setSort(getSort());
        params.setOrder(getOrder());
        params.setSorting(getSorting());
        params.setOperation(getOperation());
        params.setPrefilterAdded(isPrefilterAdded());
        params.setInternalQuery(getInternalQuery());
        return params;
    }

}
