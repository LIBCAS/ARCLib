package cz.inqool.uas.report.form;


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
 * Api for retrieving supported report generation forms.
 */
@RestController
@Api(value = "report form", description = "Api for retrieving supported report generation forms")
@RequestMapping("/api/reports/forms")
public class ReportFormApi {
    private ReportForms forms;

    /**
     * Gets all {@link ReportForm} that are defined in the system.
     *
     *
     * @return {@link List} of report forms
     */
    @ApiOperation(value = "Gets all report forms that are defined in the system.", response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = List.class)})
    @RequestMapping(method = RequestMethod.GET)
    public List<ReportForm> list() {
        return forms.getForms();
    }

    @Inject
    public void setForms(ReportForms forms) {
        this.forms = forms;
    }
}
