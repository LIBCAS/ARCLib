package cz.cas.lib.core.rest;

import cz.cas.lib.arclib.domainbase.domain.DomainObject;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * Generic RESTful CRUD API for accessing {@link IndexedStore}.
 *
 * @param <T> type of JPA entity
 */
public interface ReadOnlyApi<T extends DomainObject> {

    DataAdapter<T> getAdapter();

    /**
     * Gets one instance specified by id.
     *
     * @param id Id of the instance
     * @return Single instance
     * @throws MissingObject if instance does not exists
     */
    @Operation(summary = "Gets one instance specified by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = DomainObject.class))),
            @ApiResponse(responseCode = "404", description = "Instance does not exist")})
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @Transactional
    default T get(@Parameter(description = "Id of the instance", required = true) @PathVariable("id") String id) {
        T entity = getAdapter().find(id);
        Utils.notNull(entity, () -> new MissingObject(getAdapter().getType(), id));

        return entity;
    }

    /**
     * Gets all instances that respect the selected {@link Params}.
     *
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
    @Operation(summary = "Gets all instances that respect the selected parameters",
            description = "Returns sorted list of instances with total number")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = Result.class)))})
    @RequestMapping(method = RequestMethod.GET)
    @Transactional
    default Result<T> list(@Parameter(description = "Parameters to comply with", required = true)
                           @ModelAttribute Params params) {
        return getAdapter().findAll(params);
    }

    @Operation(summary = "Gets all instances that respect the selected parameters",
            description = "Returns sorted list of instances with total number. Same as the GET / method, " +
                    "but parameters are supplied in POST body.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = Result.class)))})
    @RequestMapping(value = "/parametrized", method = RequestMethod.POST)
    @Transactional
    default Result<T> listPost(@Parameter(description = "Parameters to comply with", required = true)
                               @RequestBody Params params) {
        return list(params);
    }
}
