package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.notification.Notification;
import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.index.autocomplete.AutoCompleteItem;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.service.NotificationService;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

import static cz.cas.lib.core.util.Utils.eq;

@Tag(name = "Notification", description = "Api for interaction with notifications")
@RestController
@RequestMapping("/api/notification")
public class NotificationApi {

    private NotificationService service;

    @Operation(summary = "Saves an instance [Perm.NOTIFICATION_RECORDS_WRITE]",
            description = "Returns single instance (possibly with computed attributes).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = Notification.class))),
            @ApiResponse(responseCode = "400", description = "Specified id does not correspond to the id of the instance")
    })
    @PreAuthorize("hasAuthority('" + Permissions.NOTIFICATION_RECORDS_WRITE + "')")
    @PutMapping(value = "/{id}")
    public Notification save(@Parameter(description = "Id of the instance", required = true) @PathVariable("id") String id,
                             @Parameter(description = "Single instance", required = true) @RequestBody @Valid Notification notification) {
        eq(id, notification.getId(), () -> new BadArgument("id"));
        return service.save(notification);
    }

    @Operation(summary = "Gets one instance specified by id [Perm.NOTIFICATION_RECORDS_READ]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = Notification.class))),
            @ApiResponse(responseCode = "404", description = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.NOTIFICATION_RECORDS_READ + "')")
    @GetMapping(value = "/{id}")
    public Notification get(@Parameter(description = "Id of the instance", required = true) @PathVariable("id") String id) {
        return service.find(id);
    }

    @Operation(summary = "Gets all instances [Perm.NOTIFICATION_RECORDS_READ]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(array = @ArraySchema(schema = @Schema(implementation = Notification.class))))
    })
    @PreAuthorize("hasAuthority('" + Permissions.NOTIFICATION_RECORDS_READ + "')")
    @GetMapping
    public Collection<Notification> list() {
        return service.findAll();
    }

    @Operation(summary = "List of autocomplete REPORTS satisfying given params. [Perm.REPORT_TEMPLATE_RECORDS_READ]")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = AutoCompleteItem.class))))})
    @PreAuthorize("hasAuthority('" + Permissions.REPORT_TEMPLATE_RECORDS_READ + "')")
    @PostMapping(value = "/list/autocomplete/reports", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<AutoCompleteItem> listAutoCompleteReports(@Parameter(description = "Parameters to comply with") @Valid @RequestBody(required = false) Params params) {
        if (params == null) params = new Params();
        return service.listAutoCompleteReports(params);
    }

    @Operation(summary = "List of autocomplete FORMATS satisfying given params. [Perm.FORMAT_RECORDS_READ]")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = AutoCompleteItem.class)))})
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_READ + "')")
    @PostMapping(value = "/list/autocomplete/formats", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<AutoCompleteItem> listAutoCompleteFormats(@Parameter(description = "Parameters to comply with") @Valid @RequestBody(required = false) Params params) {
        if (params == null) params = new Params();
        return service.listAutoCompleteFormats(params);
    }

    @Operation(summary = "Deletes an instance. [Perm.NOTIFICATION_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "404", description = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.NOTIFICATION_RECORDS_WRITE + "')")
    @DeleteMapping(value = "/{id}")
    public void delete(@Parameter(description = "Id of the instance", required = true) @PathVariable("id") String id) {
        service.delete(id);
    }

    @Autowired
    public void setFormatRevisionNotificationService(NotificationService NotificationService) {
        this.service = NotificationService;
    }
}
