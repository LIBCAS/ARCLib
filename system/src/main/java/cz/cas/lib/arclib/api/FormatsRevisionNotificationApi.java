package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.FormatsRevisionNotification;
import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.service.FormatsRevisionNotificationService;
import cz.cas.lib.core.store.Transactional;
import io.swagger.annotations.*;
import lombok.Getter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.Collection;

import static cz.cas.lib.core.util.Utils.eq;

@RestController
@Api(value = "formats revision notification", description = "Api for interaction with formats revision notifications")
@RequestMapping("/api/formats_revision_notification")
public class FormatsRevisionNotificationApi {

    private FormatsRevisionNotificationService formatsRevisionNotificationService;

    @ApiOperation(value = "Saves an instance [Perm.NOTIFICATION_RECORDS_WRITE]", notes = "Returns single instance (possibly with computed attributes).",
            response = FormatsRevisionNotification.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = FormatsRevisionNotification.class),
            @ApiResponse(code = 400, message = "Specified id does not correspond to the id of the instance")})
    @PreAuthorize("hasAuthority('" + Permissions.NOTIFICATION_RECORDS_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    @Transactional
    public FormatsRevisionNotification save(@ApiParam(value = "Id of the instance", required = true)
                                            @PathVariable("id") String id,
                                            @ApiParam(value = "Single instance", required = true)
                                            @RequestBody FormatsRevisionNotification formatsRevisionNotification) {
        eq(id, formatsRevisionNotification.getId(), () -> new BadArgument("id"));

        return formatsRevisionNotificationService.save(formatsRevisionNotification);
    }

    @ApiOperation(value = "Deletes an instance. [Perm.NOTIFICATION_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.NOTIFICATION_RECORDS_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @Transactional
    public void delete(@ApiParam(value = "Id of the instance", required = true)
                       @PathVariable("id") String id) {
        formatsRevisionNotificationService.delete(id);
    }

    @ApiOperation(value = "Gets one instance specified by id [Perm.NOTIFICATION_RECORDS_READ]", response = FormatsRevisionNotification.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = FormatsRevisionNotification.class),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.NOTIFICATION_RECORDS_READ + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @Transactional
    public FormatsRevisionNotification get(@ApiParam(value = "Id of the instance", required = true)
                                           @PathVariable("id") String id) {
        return formatsRevisionNotificationService.find(id);
    }


    @ApiOperation(value = "Gets all instances [Perm.NOTIFICATION_RECORDS_READ]", response = Collection.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Collection.class)})
    @PreAuthorize("hasAuthority('" + Permissions.NOTIFICATION_RECORDS_READ + "')")
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

