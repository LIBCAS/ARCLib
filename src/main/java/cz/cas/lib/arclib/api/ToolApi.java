package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.preservationPlanning.Tool;
import cz.cas.lib.arclib.dto.ToolUpdateDto;
import cz.cas.lib.arclib.service.preservationPlanning.ToolService;
import cz.cas.lib.core.exception.BadArgument;
import cz.cas.lib.core.exception.MissingObject;
import io.swagger.annotations.*;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.Collection;

import static cz.cas.lib.core.util.Utils.eq;
import static cz.cas.lib.core.util.Utils.notNull;

@RestController
@Api(value = "tool", description = "Api for interaction with tools")
@RequestMapping("/api/tool")
public class ToolApi {
    private ToolService service;


    @ApiOperation(value = "Gets all instances", response = Collection.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Successful response", response = Collection.class)})
    @RequestMapping(method = RequestMethod.GET)
    public Collection<Tool> listAll() {
        return service.findAll();
    }

    @ApiOperation(value = "Gets one instance specified by id.", response = Tool.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Tool.class),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public Tool get(@ApiParam(value = "Id of the instance", required = true)
                    @PathVariable("id") String id) {
        Tool entity = service.find(id);
        notNull(entity, () -> new MissingObject(service.getType(), id));
        return entity;
    }

//    @ApiOperation(value = "Saves an instance. Roles.SUPER_ADMIN", notes = "Returns single instance (possibly with computed attributes)",
//            response = Tool.class)
//    @ApiResponses(value = {
//            @ApiResponse(code = 200, message = "Successful response", response = Tool.class),
//            @ApiResponse(code = 400, message = "Specified id does not correspond to the id of the instance")})
//    @RolesAllowed({Roles.SUPER_ADMIN})
//    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
//    @Transactional
//    public Tool save(@ApiParam(value = "Id of the instance", required = true)
//                     @PathVariable("id") String id,
//                     @ApiParam(value = "Single instance", required = true)
//                     @RequestBody Tool request) {
//        eq(id, request.getId(), () -> new BadArgument("id"));
//
//        return service.save(request);
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
//        Tool tool = service.find(id);
//        notNull(tool, () -> new MissingObject(service.getType(), id));
//
//        service.delete(tool);
//    }

    @ApiOperation(value = "Updates instance",
            response = Tool.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Tool.class),
            @ApiResponse(code = 400, message = "Specified id does not correspond to the id of the instance")})
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public Tool update(@ApiParam(value = "Id of the instance", required = true)
                     @PathVariable("id") String id,
                     @ApiParam(value = "Single instance", required = true)
                     @RequestBody ToolUpdateDto request) {
        eq(id, request.getId(), () -> new BadArgument("id"));
        return service.update(request);
    }


    @Inject
    public void setService(ToolService service) {
        this.service = service;
    }
}
