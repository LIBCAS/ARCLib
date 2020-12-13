package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.preservationPlanning.IngestIssueDefinition;
import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.dto.IngestIssueDefinitionUpdateDto;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.store.IngestIssueDefinitionStore;
import cz.cas.lib.core.store.Transactional;
import io.swagger.annotations.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.Collection;

import static cz.cas.lib.core.util.Utils.eq;
import static cz.cas.lib.core.util.Utils.notNull;

@RestController
@Api(value = "ingest issue definition", description = "Api for interaction with ingest issue definitions")
@RequestMapping("/api/ingestIssueDefinition")
public class IngestIssueDefinitionApi {
    private IngestIssueDefinitionStore store;

    @ApiOperation(value = "Gets all instances [Perm.ISSUE_DEFINITIONS_READ]", response = Collection.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Successful response", response = Collection.class)})
    @PreAuthorize("hasAuthority('" + Permissions.ISSUE_DEFINITIONS_READ + "')")
    @RequestMapping(method = RequestMethod.GET)
    public Collection<IngestIssueDefinition> listAll() {
        return store.findAll();
    }

    @ApiOperation(value = "Gets one instance specified by id. [Perm.ISSUE_DEFINITIONS_READ]", response = IngestIssueDefinition.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = IngestIssueDefinition.class),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.ISSUE_DEFINITIONS_READ + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public IngestIssueDefinition get(@ApiParam(value = "Id of the instance", required = true)
                                     @PathVariable("id") String id) {
        IngestIssueDefinition entity = store.find(id);
        notNull(entity, () -> new MissingObject(store.getType(), id));
        return entity;
    }

//    @ApiOperation(value = "Saves an instance. Roles.SUPER_ADMIN", notes = "Returns single instance (possibly with computed attributes)",
//            response = IngestIssueDefinition.class)
//    @ApiResponses(value = {
//            @ApiResponse(code = 200, message = "Successful response", response = IngestIssueDefinition.class),
//            @ApiResponse(code = 400, message = "Specified id does not correspond to the id of the instance")})
//    @RolesAllowed({Roles.SUPER_ADMIN})
//    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
//    @Transactional
//    public IngestIssueDefinition save(@ApiParam(value = "Id of the instance", required = true)
//                                      @PathVariable("id") String id,
//                                      @ApiParam(value = "Single instance", required = true)
//                                      @RequestBody IngestIssueDefinition request) {
//        eq(id, request.getId(), () -> new BadArgument("id"));
//
//        return store.save(request);
//    }

//    @ApiOperation(value = "Deletes an instance. Roles.SUPER_ADMIN")
//    @ApiResponses(value = {
//            @ApiResponse(code = 200, message = "Successful response"),
//            @ApiResponse(code = 404, message = "Instance does not exist")})
//    @RolesAllowed({Roles.SUPER_ADMIN})
//    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
//    @Transactional
//    public void delete(@ApiParam(value = "Id of the instance", required = true)
//                       @PathVariable("id") String id) {
//        IngestIssueDefinition tool = store.find(id);
//        notNull(tool, () -> new MissingObject(store.getType(), id));
//
//        store.delete(tool);
//    }

    @ApiOperation(value = "Updates instance. [Perm.ISSUE_DEFINITIONS_READ]",
            response = IngestIssueDefinition.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = IngestIssueDefinition.class),
            @ApiResponse(code = 400, message = "Specified id does not correspond to the id of the instance")})
    @PreAuthorize("hasAuthority('" + Permissions.ISSUE_DEFINITIONS_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    @Transactional
    public IngestIssueDefinition update(@ApiParam(value = "Id of the instance", required = true)
                                        @PathVariable("id") String id,
                                        @ApiParam(value = "Single instance", required = true)
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

    @Inject
    public void setStore(IngestIssueDefinitionStore store) {
        this.store = store;
    }
}
