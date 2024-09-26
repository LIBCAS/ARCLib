package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.preservationPlanning.IngestIssueDefinition;
import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.dto.IngestIssueDefinitionUpdateDto;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.store.IngestIssueDefinitionStore;
import cz.cas.lib.core.store.Transactional;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

import static cz.cas.lib.core.util.Utils.eq;
import static cz.cas.lib.core.util.Utils.notNull;

@RestController
@Tag(name = "ingest issue definition", description = "Api for interaction with ingest issue definitions")
@RequestMapping("/api/ingestIssueDefinition")
public class IngestIssueDefinitionApi {
    private IngestIssueDefinitionStore store;

    @Operation(summary = "Gets all instances [Perm.ISSUE_DEFINITIONS_READ]")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Successful response", content = @Content(array = @ArraySchema(schema = @Schema(implementation = IngestIssueDefinition.class))))})
    @PreAuthorize("hasAuthority('" + Permissions.ISSUE_DEFINITIONS_READ + "')")
    @RequestMapping(method = RequestMethod.GET)
    public Collection<IngestIssueDefinition> listAll() {
        return store.findAll();
    }

    @Operation(summary = "Gets one instance specified by id. [Perm.ISSUE_DEFINITIONS_READ]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = IngestIssueDefinition.class))),
            @ApiResponse(responseCode = "404", description = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.ISSUE_DEFINITIONS_READ + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public IngestIssueDefinition get(@Parameter(description = "Id of the instance", required = true)
                                     @PathVariable("id") String id) {
        IngestIssueDefinition entity = store.find(id);
        notNull(entity, () -> new MissingObject(store.getType(), id));
        return entity;
    }

//    @Operation(summary = "Saves an instance. Roles.SUPER_ADMIN", description = "Returns single instance (possibly with computed attributes)",
//            responses = {@ApiResponse(content = @Content(schema = @Schema(implementation = IngestIssueDefinition.class)))})
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "Successful response", response = IngestIssueDefinition.class),
//            @ApiResponse(responseCode = "400", description = "Specified id does not correspond to the id of the instance")})
//    @RolesAllowed({Roles.SUPER_ADMIN})
//    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
//    @Transactional
//    public IngestIssueDefinition save(@Parameter(description = "Id of the instance", required = true)
//                                      @PathVariable("id") String id,
//                                      @Parameter(description = "Single instance", required = true)
//                                      @RequestBody IngestIssueDefinition request) {
//        eq(id, request.getId(), () -> new BadArgument("id"));
//
//        return store.save(request);
//    }

//    @Operation(summary = "Deletes an instance. Roles.SUPER_ADMIN")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "Successful response"),
//            @ApiResponse(responseCode = "404", description = "Instance does not exist")})
//    @RolesAllowed({Roles.SUPER_ADMIN})
//    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
//    @Transactional
//    public void delete(@Parameter(description = "Id of the instance", required = true)
//                       @PathVariable("id") String id) {
//        IngestIssueDefinition tool = store.find(id);
//        notNull(tool, () -> new MissingObject(store.getType(), id));
//
//        store.delete(tool);
//    }

    @Operation(summary = "Updates instance. [Perm.ISSUE_DEFINITIONS_READ]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = IngestIssueDefinition.class))),
            @ApiResponse(responseCode = "400", description = "Specified id does not correspond to the id of the instance")})
    @PreAuthorize("hasAuthority('" + Permissions.ISSUE_DEFINITIONS_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    @Transactional
    public IngestIssueDefinition update(@Parameter(description = "Id of the instance", required = true)
                                        @PathVariable("id") String id,
                                        @Parameter(description = "Single instance", required = true)
                                        @RequestBody IngestIssueDefinitionUpdateDto request) {
        eq(id, request.getId(), () -> new BadArgument("id"));
        IngestIssueDefinition ingestIssueDefinition = store.find(id);
        notNull(ingestIssueDefinition, () -> new MissingObject(store.getType(), id));
        ingestIssueDefinition.setName(request.getName());
        ingestIssueDefinition.setDescription(request.getDescription());
        ingestIssueDefinition.setSolution(request.getSolution());
        ingestIssueDefinition.setNumber(request.getNumber());
        return store.save(ingestIssueDefinition);
    }

    @Autowired
    public void setStore(IngestIssueDefinitionStore store) {
        this.store = store;
    }
}
