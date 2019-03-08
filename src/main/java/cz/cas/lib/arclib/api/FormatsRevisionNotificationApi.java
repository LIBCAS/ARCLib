package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.FormatsRevisionNotification;
import cz.cas.lib.arclib.security.authorization.Roles;
import cz.cas.lib.arclib.service.FormatsRevisionNotificationService;
import cz.cas.lib.core.exception.BadArgument;
import cz.cas.lib.core.store.Transactional;
import io.swagger.annotations.*;
import lombok.Getter;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import java.util.Collection;

import static cz.cas.lib.core.util.Utils.eq;

@RestController
@Api(value = "formatsRevisionNotification", description = "Api for interaction with formats revision notifications")
@RequestMapping("/api/formats_revision_notification")
public class FormatsRevisionNotificationApi {

    @Getter
    private FormatsRevisionNotificationService formatsRevisionNotificationService;

    @ApiOperation(value = "Saves an instance", notes = "Returns single instance (possibly with computed attributes). Roles.ADMIN, Roles.SUPER_ADMIN",
            response = FormatsRevisionNotification.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = FormatsRevisionNotification.class),
            @ApiResponse(code = 400, message = "Specified id does not correspond to the id of the instance")})
    @RolesAllowed({Roles.ADMIN, Roles.SUPER_ADMIN})
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    @Transactional
    public FormatsRevisionNotification save(@ApiParam(value = "Id of the instance", required = true)
                                            @PathVariable("id") String id,
                                            @ApiParam(value = "Single instance", required = true)
                                            @RequestBody FormatsRevisionNotification formatsRevisionNotification) {
        eq(id, formatsRevisionNotification.getId(), () -> new BadArgument("id"));

        return formatsRevisionNotificationService.save(formatsRevisionNotification);
    }

    @ApiOperation(value = "Deletes an instance. Roles.ADMIN, Roles.SUPER_ADMIN")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @Transactional
    @RolesAllowed({Roles.ADMIN, Roles.SUPER_ADMIN})
    public void delete(@ApiParam(value = "Id of the instance", required = true)
                       @PathVariable("id") String id) {
        formatsRevisionNotificationService.delete(id);
    }

    @ApiOperation(value = "Gets one instance specified by id", response = FormatsRevisionNotification.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = FormatsRevisionNotification.class),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @Transactional
    public FormatsRevisionNotification get(@ApiParam(value = "Id of the instance", required = true)
                                           @PathVariable("id") String id) {
        return formatsRevisionNotificationService.find(id);
    }


    @ApiOperation(value = "Gets all instances", response = Collection.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Collection.class)})
    @RequestMapping(method = RequestMethod.GET)
    @Transactional
    public Collection<FormatsRevisionNotification> list() {
        return formatsRevisionNotificationService.findAll();
    }

    @Inject
    public void setFormatRevisionNotificationService(FormatsRevisionNotificationService formatsRevisionNotificationService) {
        this.formatsRevisionNotificationService = formatsRevisionNotificationService;
    }
}
