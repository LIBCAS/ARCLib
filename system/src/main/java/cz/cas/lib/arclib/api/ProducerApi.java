package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.ConflictException;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.exception.BadRequestException;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.store.ProducerStore;
import cz.cas.lib.core.store.Transactional;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

import static cz.cas.lib.core.util.Utils.eq;
import static cz.cas.lib.core.util.Utils.notNull;

@RestController
@Tag(name = "producer", description = "Api for interaction with producers")
@RequestMapping("/api/producer")
public class ProducerApi {
    @Getter
    private ProducerStore store;

    @Operation(summary = "Saves an instance. [Perm.PRODUCER_RECORDS_WRITE]", description = "Returns single instance (possibly with computed attributes)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = Producer.class))),
            @ApiResponse(responseCode = "400", description = "Specified id does not correspond to the id of the instance")})
    @PreAuthorize("hasAuthority('" + Permissions.PRODUCER_RECORDS_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    @Transactional
    public Producer save(@Parameter(description = "Id of the instance", required = true)
                         @PathVariable("id") String id,
                         @Parameter(description = "Single instance", required = true)
                         @RequestBody Producer request) throws ConflictException {
        eq(id, request.getId(), () -> new BadArgument("id"));
        notNull(request.getName(), () -> new BadRequestException("name can't be null"));
        Producer existing = store.find(id);
        if (existing != null && !existing.getName().equals(request.getName()))
            throw new ConflictException("Producer name can't be updated");

        return store.save(request);
    }

    @Operation(summary = "Deletes an instance. [Perm.PRODUCER_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "404", description = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.PRODUCER_RECORDS_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @Transactional
    public void delete(@Parameter(description = "Id of the instance", required = true)
                       @PathVariable("id") String id) {
        Producer entity = store.find(id);
        notNull(entity, () -> new MissingObject(store.getType(), id));

        store.delete(entity);
    }

    @Operation(summary = "Gets one instance specified by id. [Perm.PRODUCER_RECORDS_READ]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = Producer.class))),
            @ApiResponse(responseCode = "404", description = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.PRODUCER_RECORDS_READ + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @Transactional
    public Producer get(@Parameter(description = "Id of the instance", required = true)
                        @PathVariable("id") String id) {
        Producer entity = store.find(id);
        notNull(entity, () -> new MissingObject(store.getType(), id));

        return entity;
    }

    @Operation(summary = "Gets all instances [Perm.PRODUCER_RECORDS_READ]")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Successful response", content = @Content(array = @ArraySchema(schema = @Schema(implementation = Producer.class))))})
    @RequestMapping(method = RequestMethod.GET)
    @PreAuthorize("hasAuthority('" + Permissions.PRODUCER_RECORDS_READ + "')")
    @Transactional
    public Collection<Producer> list() {
        return store.findAll();
    }

    @Autowired
    public void setStore(ProducerStore store) {
        this.store = store;
    }
}
