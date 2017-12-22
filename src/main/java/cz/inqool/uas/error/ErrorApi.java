package cz.inqool.uas.error;

import cz.inqool.uas.rest.ReadOnlyApi;
import cz.inqool.uas.security.Permissions;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.Getter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;

/**
 * Api for gathering application errors
 */
@RolesAllowed(Permissions.ERROR)
@RestController
@Api(value = "error", description = "Api for gathering application errors")
@RequestMapping("/api/errors")
public class ErrorApi implements ReadOnlyApi<Error> {

    @Getter
    private ErrorStore adapter;

    private ErrorService service;

    @ApiOperation(value = "Logs error.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Sequence not found")
    })
    @RequestMapping(value = "/log", method = RequestMethod.POST)
    public void log(@RequestParam(value = "message") String message,
                      @RequestParam(value = "stacktrace") String stackTrace,
                      @RequestParam(value = "url") String url,
                      @RequestParam(value = "userAgent") String userAgent) {
        service.logError(message, stackTrace, url, userAgent);
    }

    @Inject
    public void setAdapter(ErrorStore store) {
        this.adapter = store;
    }

    @Inject
    public void setService(ErrorService service) {
        this.service = service;
    }
}
