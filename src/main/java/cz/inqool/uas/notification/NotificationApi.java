package cz.inqool.uas.notification;

import cz.inqool.uas.exception.BadArgument;
import cz.inqool.uas.exception.ForbiddenObject;
import cz.inqool.uas.exception.MissingObject;
import cz.inqool.uas.index.dto.Params;
import cz.inqool.uas.index.dto.Result;
import io.swagger.annotations.*;
import lombok.Getter;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.List;

/**
 * Api for creating and retrieving notifications.
 */
@RestController
@Api(value = "notifications", description = "Api for creating and retrieving notifications")
@RequestMapping("/api/notifications")
public class NotificationApi {

    @Getter
    private NotificationService service;

    /**
     * Gets all received notifications, that respect the selected parameters
     *
     * <p>
     *     Though {@link Params} one could specify filtering, sorting and paging. For further explanation
     *     see {@link Params}.
     * </p>
     * <p>
     *     Returning also the total number of instances passed through the filtering phase.
     * </p>
     *
     * @param params Parameters to comply with
     * @return Sorted {@link List} of instances with total number
     */
    @ApiOperation(value = "Gets all received notifications, that respect the selected parameters", response = Result.class)
    @ApiResponses(value = @ApiResponse(code = 200, message = "Successful response"))
    @RequestMapping(value = "/received", method = RequestMethod.GET)
    public Result<Notification> listReceived(
            @ApiParam(value = "Parameters to comply with", required = true) @ModelAttribute Params params) {
        return service.listReceivedNotifications(params);
    }

    /**
     * Gets all sent notifications, that respect the selected parameters
     *
     * <p>
     *     Though {@link Params} one could specify filtering, sorting and paging. For further explanation
     *     see {@link Params}.
     * </p>
     * <p>
     *     Returning also the total number of instances passed through the filtering phase.
     * </p>
     *
     * @param params Parameters to comply with
     * @return Sorted {@link List} of instances with total number
     */
    @ApiOperation(value = "Gets all sent notifications, that respect the selected parameters", response = Result.class)
    @ApiResponses(value = @ApiResponse(code = 200, message = "Successful response"))
    @RequestMapping(value = "/sent", method = RequestMethod.GET)
    public Result<Notification> listSent(
            @ApiParam(value = "Parameters to comply with", required = true) @ModelAttribute Params params) {
        return service.listSentNotifications(params);
    }

    /**
     * Marks selected notification as read
     *
     * @param id Id of the Notification
     * @throws ForbiddenObject Thrown if selected Notification is not owned by logged in user
     * @throws MissingObject Thrown if Notification does not exist
     */
    @ApiOperation(value = "Gets all received notifications, that respect the selected parameters", response = Result.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 403, message = "Notification is not owned by logged in user"),
            @ApiResponse(code = 404, message = "Notification does not exist"),
    })
    @RequestMapping(value = "/{id}/read", method = RequestMethod.POST)
    public void read(
            @ApiParam(value = "Id of the notification", required = true) @PathVariable("id") String id) {
        service.readNotification(id);
    }

    /**
     * Create new notification
     *
     * @param title Title of the notification
     * @param description Description of the notification
     * @param recipientId Recipient Id of the notification
     * @param flash One time notification
     * @param emailing Notification will be also send by email
     * @throws IllegalArgumentException Thrown if argument missing
     */
    @ApiOperation(value = "Gets all received notifications, that respect the selected parameters", response = Result.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 400, message = "Argument missing")
    })
    @RequestMapping(method = RequestMethod.POST)
    public void create(
            @ApiParam(value = "Title of the notification", required = true) @RequestParam("title") String title,
            @ApiParam(value = "Description of the notification", required = true) @RequestParam("description") String description,
            @ApiParam(value = "Id of the recipient user", required = false) @RequestParam(value = "recipientId", required = false) String recipientId,
            @ApiParam(value = "Id of the recipient user", required = false) @RequestParam(value = "roleId", required = false) String roleId,
            @ApiParam(value = "One time notification", required = true) @RequestParam("flash") boolean flash,
            @ApiParam(value = "Notification will be also send by email", required = true) @RequestParam("emailing") boolean emailing) {

        if ((recipientId != null) == (roleId != null)) {
            throw new BadArgument("exactly one of recipientId and roleId needs to be set");
        } else if (recipientId != null) {
            service.createNotification(title, description, recipientId, flash, emailing);
        } else {
            service.createMultiNotification(title, description, roleId, flash, emailing);
        }
    }

    @Inject
    public void setService(NotificationService service) {
        this.service = service;
    }
}
