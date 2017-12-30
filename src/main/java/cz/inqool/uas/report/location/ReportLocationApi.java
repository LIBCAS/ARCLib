package cz.inqool.uas.report.location;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.util.List;

/**
 * Api for retrieving supported report locations.
 *
 * <p>
 *     See {@link ReportLocation} description for more information.
 * </p>
 */
@RestController
@Api(value = "report location", description = "Api for retrieving supported report locations")
@RequestMapping("/api/reports/locations")
public class ReportLocationApi {
    private ReportLocations locations;

    /**
     * Gets all {@link ReportLocation} that are defined in the system.
     *
     *
     * @return {@link List} of report locations
     */
    @ApiOperation(value = "Gets all report locations that are defined in the system.",
            response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = List.class)})
    @RequestMapping(method = RequestMethod.GET)
    public List<ReportLocation> list() {
        return locations.getLocations();
    }

    @Inject
    public void setLocations(ReportLocations locations) {
        this.locations = locations;
    }
}
