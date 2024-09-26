package cz.cas.lib.core.rest;

import cz.cas.lib.arclib.domainbase.domain.DomainObject;
import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.core.index.solr.IndexedStore;
import cz.cas.lib.core.store.Transactional;
import cz.cas.lib.core.util.Utils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


/**
 * Generic RESTful CRUD API for accessing {@link IndexedStore}.
 *
 * @param <T> type of JPA entity
 */
public interface GeneralApi<T extends DomainObject> extends ReadOnlyApi<T> {

    /**
     * Saves an instance.
     *
     * <p>
     * Specified id should correspond to {@link DomainObject#id} otherwise exception is thrown.
     * </p>
     *
     * @param id      Id of the instance
     * @param request Single instance
     * @return Single instance (possibly with computed attributes)
     * @throws BadArgument if specified id does not correspond to {@link DomainObject#id}
     */
    @Operation(summary = "Saves an instance", description = "Returns single instance (possibly with computed attributes)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = DomainObject.class))),
            @ApiResponse(responseCode = "400", description = "Specified id does not correspond to the id of the instance")})
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    @Transactional
    default T save(@Parameter(description = "Id of the instance", required = true) @PathVariable("id") String id,
                   @Parameter(description = "Single instance", required = true)
                   @RequestBody T request) {
        Utils.eq(id, request.getId(), () -> new BadArgument("id"));

        return getAdapter().save(request);
    }

    /**
     * Deletes an instance.
     *
     * @param id Id of the instance
     * @throws MissingObject if specified instance is not found
     */
    @Operation(summary = "Deletes an instance")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "404", description = "Instance does not exist")})
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @Transactional
    default void delete(@Parameter(description = "Id of the instance", required = true) @PathVariable("id") String id) {
        T entity = getAdapter().find(id);
        Utils.notNull(entity, () -> new MissingObject(getAdapter().getType(), id));

        getAdapter().delete(entity);
    }
}
