package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.notification.Notification;
import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.index.autocomplete.AutoCompleteItem;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.service.NotificationService;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import io.swagger.annotations.*;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.Valid;
import java.util.Collection;

import static cz.cas.lib.core.util.Utils.eq;

@Api(value = "Notification", description = "Api for interaction with notifications")
@RestController
@RequestMapping("/api/notification")
public class NotificationApi {

    private NotificationService service;

    @ApiOperation(value = "Saves an instance [Perm.NOTIFICATION_RECORDS_WRITE]",
            notes = "Returns single instance (possibly with computed attributes).", response = Notification.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Notification.class),
            @ApiResponse(code = 400, message = "Specified id does not correspond to the id of the instance")
    })
    @PreAuthorize("hasAuthority('" + Permissions.NOTIFICATION_RECORDS_WRITE + "')")
    @PutMapping(value = "/{id}")
    public Notification save(@ApiParam(value = "Id of the instance", required = true) @PathVariable("id") String id,
                             @ApiParam(value = "Single instance", required = true) @RequestBody Notification notification) {
        eq(id, notification.getId(), () -> new BadArgument("id"));
        return service.save(notification);
    }

    @ApiOperation(value = "Gets one instance specified by id [Perm.NOTIFICATION_RECORDS_READ]", response = Notification.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Notification.class),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.NOTIFICATION_RECORDS_READ + "')")
    @GetMapping(value = "/{id}")
    public Notification get(@ApiParam(value = "Id of the instance", required = true) @PathVariable("id") String id) {
        return service.find(id);
    }

    @ApiOperation(value = "Gets all instances [Perm.NOTIFICATION_RECORDS_READ]", response = Notification.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Notification.class, responseContainer = "List")
    })
    @PreAuthorize("hasAuthority('" + Permissions.NOTIFICATION_RECORDS_READ + "')")
    @GetMapping
    public Collection<Notification> list() {
        return service.findAll();
    }

    @ApiOperation(value = "List of autocomplete REPORTS satisfying given params. [Perm.REPORT_TEMPLATE_RECORDS_READ]")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", response = AutoCompleteItem.class, responseContainer = "List")})
    @PreAuthorize("hasAuthority('" + Permissions.REPORT_TEMPLATE_RECORDS_READ + "')")
    @PostMapping(value = "/list/autocomplete/reports", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<AutoCompleteItem> listAutoCompleteReports(@ApiParam(value = "Parameters to comply with") @Valid @RequestBody(required = false) Params params) {
        if (params == null) params = new Params();
        return service.listAutoCompleteReports(params);
    }

    @ApiOperation(value = "List of autocomplete FORMATS satisfying given params. [Perm.FORMAT_RECORDS_READ]")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", response = AutoCompleteItem.class, responseContainer = "List")})
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_READ + "')")
    @PostMapping(value = "/list/autocomplete/formats", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<AutoCompleteItem> listAutoCompleteFormats(@ApiParam(value = "Parameters to comply with") @Valid @RequestBody(required = false) Params params) {
        if (params == null) params = new Params();
        return service.listAutoCompleteFormats(params);
    }

    @ApiOperation(value = "Deletes an instance. [Perm.NOTIFICATION_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.NOTIFICATION_RECORDS_WRITE + "')")
    @DeleteMapping(value = "/{id}")
    public void delete(@ApiParam(value = "Id of the instance", required = true) @PathVariable("id") String id) {
        service.delete(id);
    }

    @Inject
    public void setFormatRevisionNotificationService(NotificationService NotificationService) {
        this.service = NotificationService;
    }
}
