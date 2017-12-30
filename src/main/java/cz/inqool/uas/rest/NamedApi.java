package cz.inqool.uas.rest;

import cz.inqool.uas.domain.DomainObject;
import cz.inqool.uas.index.IndexedStore;
import cz.inqool.uas.index.dto.Filter;
import cz.inqool.uas.index.dto.FilterOperation;
import cz.inqool.uas.index.dto.Params;
import cz.inqool.uas.index.dto.Result;
import cz.inqool.uas.rest.data.DataAdapter;
import cz.inqool.uas.store.Transactional;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import static cz.inqool.uas.util.Utils.asList;

/**
 * Generic RESTful CRUD API for accessing {@link IndexedStore}.
 *
 * @param <T> type of JPA entity
 */
public interface NamedApi<T extends DomainObject> extends GeneralApi<T> {
    DataAdapter<T> getAdapter();

    String getNameAttribute();

    /**
     * Gets all instances.
     *
     * <p>
     *     Used for named selects.
     * </p>
     *
     * @return Sorted {@link List} of instances
     */
    @ApiOperation(value = "Gets all instances that respect the selected prefix",
            notes = "Returns sorted list of instances", response = Result.class)
    @ApiResponses(value = @ApiResponse(code = 200, message = "Successful response", response = Result.class))
    @RequestMapping(value = "/all", method = RequestMethod.GET)
    @Transactional
    default List<T> all() {
        Params params = new Params();
        params.setSort(getNameAttribute());
        params.setPageSize(1000);

        return getAdapter().findAll(params).getItems();
    }

    /**
     * Gets all instances that respect the selected prefix.
     *
     * <p>
     *     Used for named selects.
     * </p>
     *
     * @param prefix Prefix to comply with
     * @return Sorted {@link List} of instances
     */
    @ApiOperation(value = "Gets all instances that respect the selected prefix",
            notes = "Filter is applied to main attribute. If filtering by other " +
                    "attributes is desired, use /parameterized endpoint.", response = Result.class)
    @ApiResponses(value = @ApiResponse(code = 200, message = "Successful response", response = Result.class))
    @RequestMapping(value = "/prefixed", method = RequestMethod.GET)
    @Transactional
    default List<T> prefixed(@ApiParam(value = "Parameters to comply with", required = true)
                             @RequestParam("prefix") String prefix) {
        Params params = new Params();
        params.setSort(getNameAttribute());
        params.setPageSize(100);
        params.setFilter(asList(new Filter(getNameAttribute(), FilterOperation.STARTWITH, prefix, null)));

        return getAdapter().findAll(params).getItems();
    }

    /**
     * Gets all instances that contains the specified string.
     *
     * <p>
     *     Used for named selects.
     * </p>
     *
     * @param q String to contain
     * @return Sorted {@link List} of instances
     */
    @ApiOperation(value = "Gets all instances that contains the specified string",
            notes = "Filter is applied to main attribute. If filtering by other " +
                    "attributes is desired, use /parameterized endpoint.", response = Result.class)
    @ApiResponses(value = @ApiResponse(code = 200, message = "Successful response", response = Result.class))
    @RequestMapping(value = "/containing", method = RequestMethod.GET)
    @Transactional
    default List<T> containing(@ApiParam(value = "Parameters to comply with", required = true)
                               @RequestParam("q") String q) {
        Params params = new Params();
        params.setSort(getNameAttribute());
        params.setPageSize(100);
        params.setFilter(asList(new Filter(getNameAttribute(), FilterOperation.CONTAINS, q, null)));

        return getAdapter().findAll(params).getItems();
    }
}
