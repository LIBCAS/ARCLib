package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.report.ExportFormat;
import cz.cas.lib.arclib.report.ExporterService;
import cz.cas.lib.arclib.report.Report;
import cz.cas.lib.arclib.report.ReportStore;
import cz.cas.lib.core.exception.BadArgument;
import cz.cas.lib.core.exception.MissingObject;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static cz.cas.lib.core.util.Utils.checked;

@Slf4j
@RestController
@Api(value = "report", description = "Api for interaction with reports")
@RequestMapping("/api/report")
public class ReportApi {

    private ReportStore store;
    private ExporterService exporter;

    @ApiOperation(value = "Finds report template by ID and exports it to specified format")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 400, message = "Given parameter is not defined within template or the value can not be parsed")})
    @RequestMapping(value = "/{reportId}/{format}", method = RequestMethod.GET)
    public void getReport(@ApiParam(value = "Id of the report", required = true)
                          @PathVariable("reportId") String reportId,
                          @ApiParam(value = "Output format, one of CSV, XLSX, HTML, PDF", required = true)
                          @PathVariable("format") String format,
                          @ApiParam(value = "Query string with parameters used to override default parameters values" +
                                  " which are specified inside template itself. Boolean parameter value should be in" +
                                  " string form i.e. true/false", required = true)
                          @RequestParam Map<String, String> params, HttpServletResponse response) throws IOException {
        checkUUID(reportId);
        switch (format.toUpperCase()) {
            case "PDF":
                response.setContentType("application/pdf");
                break;
            case "CSV":
                response.setContentType("text/csv");
                break;
            case "XLSX":
                response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                break;
            case "HTML":
                response.setContentType("text/html");
                break;
            default:
                String e = String.format("Unsupported export format: %s", format);
                log.warn(e);
                throw new BadArgument(e);
        }
        Report r = store.find(reportId);
        if (r == null) throw new MissingObject(Report.class, reportId);
        response.addHeader("Content-Disposition", "attachment; filename=" + r.getName() + "_" + LocalDate.now().toString() + "." + format.toLowerCase());
        response.setStatus(200);
        exporter.export(r, ExportFormat.valueOf(format.toUpperCase()), params, response.getOutputStream());
    }

    @ApiOperation(value = "Compiles and stores report template", notes = "Validate type of template custom parameters " +
            "and their default values if there are any.<br>" +
            "Supported parameter types:" +
            "<ul>" +
            "<li>java.lang.String</li>" +
            "<li>java.lang.Short</li>" +
            "<li>java.lang.Long</li>" +
            "<li>java.lang.Integer</li>" +
            "<li>java.lang.Float</li>" +
            "<li>java.lang.Double</li>" +
            "<li>java.lang.Boolean</li>" +
            "</ul>" +
            "Boolean parameter value should be in string form i.e. true/false",
            response = String.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = String.class),
            @ApiResponse(code = 400, message = "Parameters validation failed.")})
    @RequestMapping(method = RequestMethod.POST)
    public String save(@ApiParam(value = "Template file", required = true)
                       @RequestParam("template") MultipartFile template,
                       @ApiParam(value = "Template name", required = true)
                       @RequestParam("name") String name) throws IOException {
        try (InputStream is = template.getInputStream()) {
            return store.saveReport(name, template.getInputStream());
        }
    }

    private void checkUUID(String id) {
        checked(() -> UUID.fromString(id), () -> new BadArgument(id));
    }

    @Inject
    public void setReportStore(ReportStore store) {
        this.store = store;
    }

    @Inject
    public void setExporter(ExporterService exporter) {
        this.exporter = exporter;
    }
}
