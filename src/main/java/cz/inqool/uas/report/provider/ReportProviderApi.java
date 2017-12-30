package cz.inqool.uas.report.provider;


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
 * Api for retrieving supported report providers.
 *
 * <p>
 *     See {@link ReportProvider} description for more information.
 * </p>
 */
@RestController
@Api(value = "report provider", description = "Api for retrieving supported report providers")
@RequestMapping("/api/reports/providers")
public class ReportProviderApi {
    private ReportProviders providers;

    /**
     * Gets all {@link ReportProvider} that are defined in the system.
     *
     *
     * @return {@link List} of report locations
     */
    @ApiOperation(value = "Gets all report providers that are defined in the system.",
            response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = List.class)})
    @RequestMapping(method = RequestMethod.GET)
    public List<ReportProvider> list() {
        return providers.getProviders();
    }

    @Inject
    public void setProviders(ReportProviders providers) {
        this.providers = providers;
    }
}
