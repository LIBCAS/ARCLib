package cz.cas.lib.core.rest;

import cz.cas.lib.arclib.domainbase.domain.DomainObject;
import cz.cas.lib.core.index.dto.Filter;
import cz.cas.lib.core.index.dto.FilterOperation;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import cz.cas.lib.core.index.solr.IndexedStore;
import cz.cas.lib.core.rest.data.DataAdapter;
import cz.cas.lib.core.store.Transactional;
import cz.cas.lib.core.util.Utils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


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
     * Used for named selects.
     * </p>
     *
     * @return Sorted {@link List} of instances
     */
    @Operation(summary = "Gets all instances that respect the selected prefix",
            description = "Returns sorted list of instances")
    @ApiResponses(value = @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = Result.class))))
    @RequestMapping(value = "/all", method = RequestMethod.GET)
    @Transactional
    default List<T> all() {
        Params params = new Params();
        params.setSort(getNameAttribute());
        params.setPageSize(null);

        return getAdapter().findAll(params).getItems();
    }

    /**
     * Gets all instances that respect the selected prefix.
     *
     * <p>
     * Used for named selects.
     * </p>
     *
     * @param prefix Prefix to comply with
     * @return Sorted {@link List} of instances
     */
    @Operation(summary = "Gets all instances that respect the selected prefix",
            description = "Filter is applied to main attribute. If filtering by other " +
                    "attributes is desired, use /parameterized endpoint.")
    @ApiResponses(value = @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = Result.class))))
    @RequestMapping(value = "/prefixed", method = RequestMethod.GET)
    @Transactional
    default List<T> prefixed(@Parameter(description = "Parameters to comply with", required = true)
                             @RequestParam("prefix") String prefix) {
        Params params = new Params();
        params.setSort(getNameAttribute());
        params.setPageSize(null);
        params.setFilter(Utils.asList(new Filter(getNameAttribute(), FilterOperation.STARTWITH, prefix, null)));

        return getAdapter().findAll(params).getItems();
    }

    /**
     * Gets all instances that contains the specified string.
     *
     * <p>
     * Used for named selects.
     * </p>
     *
     * @param q String to contain
     * @return Sorted {@link List} of instances
     */
    @Operation(summary = "Gets all instances that contains the specified string",
            description = "Filter is applied to main attribute. If filtering by other " +
                    "attributes is desired, use /parameterized endpoint.")
    @ApiResponses(value = @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = Result.class))))
    @RequestMapping(value = "/containing", method = RequestMethod.GET)
    @Transactional
    default List<T> containing(@Parameter(description = "Parameters to comply with", required = true)
                               @RequestParam("q") String q) {
        Params params = new Params();
        params.setSort(getNameAttribute());
        params.setPageSize(null);
        params.setFilter(Utils.asList(new Filter(getNameAttribute(), FilterOperation.CONTAINS, q, null)));

        return getAdapter().findAll(params).getItems();
    }
}
