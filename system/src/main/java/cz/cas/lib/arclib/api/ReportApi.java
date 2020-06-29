package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.report.ExportFormat;
import cz.cas.lib.arclib.report.ExporterService;
import cz.cas.lib.arclib.report.Report;
import cz.cas.lib.arclib.security.authorization.Roles;
import cz.cas.lib.arclib.service.ReportService;
import io.swagger.annotations.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cz.cas.lib.core.util.Utils.checkUUID;
import static cz.cas.lib.core.util.Utils.eq;

@Slf4j
@RestController
@Api(value = "report", description = "Api for interaction with reports")
@RequestMapping("/api/report")
public class ReportApi {

    private ReportService reportService;
    private ExporterService exporter;

    @ApiOperation(value = "Finds report template by ID and exports it to specified format. Roles.SUPER_ADMIN")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 400, message = "Given parameter is not defined within template or the value can not be parsed")})
    @RequestMapping(value = "/{reportId}/{format}", method = RequestMethod.GET)
    @RolesAllowed({Roles.SUPER_ADMIN})
    public void getReport(@ApiParam(value = "Id of the report", required = true)
                          @PathVariable("reportId") String reportId,
                          @ApiParam(value = "Output format, one of CSV, XLSX, HTML, PDF", required = true)
                          @PathVariable("format") String format,
                          @ApiParam(value = "Query string with parameters used to override default parameters values" +
                                  " which are specified inside template itself. Boolean parameter value should be in" +
                                  " string form i.e. true/false")
                              @RequestParam(required = false) Map<String, String> params, HttpServletResponse response) throws IOException {
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
        Report r = reportService.find(reportId);
        response.addHeader("Content-Disposition", "attachment; filename=" + r.getName() + "_" + LocalDate.now().toString() + "." + format.toLowerCase());
        response.setStatus(200);
        exporter.export(r, ExportFormat.valueOf(format.toUpperCase()), params, response.getOutputStream());
    }

    @ApiOperation(value = "Compiles and stores report template. Roles.SUPER_ADMIN", notes = "If the arclibXmlDs query param is set to true, " +
            "report will use JDBC collection to ARCLib XML SOLR collection, otherwise report uses JDBC connection to the ARCLib system database. " +
            "<p>Validate type of template custom parameters " +
            "and their default values if there are any.</p>" +
            "Supported parameter types:" +
            "<ul>" +
            "<li>java.lang.String</li>" +
            "<li>java.lang.Short</li>" +
            "<li>java.lang.Long</li>" +
            "<li>java.lang.Integer</li>" +
            "<li>java.lang.Float</li>" +
            "<li>java.lang.Double</li>" +
            "<li>java.lang.Boolean</li>" +
            "</ul>",
            response = Report.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Report.class),
            @ApiResponse(code = 400, message = "Parameters validation failed.")})
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    @RolesAllowed({Roles.SUPER_ADMIN})
    public Report save(@ApiParam(value = "Id of the instance", required = true) @PathVariable("id") String id,
                       @ApiParam(value = "Single instance", required = true) @RequestBody Report request) throws IOException {
        eq(id, request.getId(), () -> new BadArgument("id"));
        return reportService.saveReport(request);
    }

    @ApiOperation(value = "Deletes an instance. Roles.SUPER_ADMIN")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RolesAllowed({Roles.SUPER_ADMIN})
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void delete(@ApiParam(value = "Id of the instance", required = true)
                       @PathVariable("id") String id) {
        reportService.delete(id);
    }

    @ApiOperation(value = "Gets one instance specified by id. Roles.SUPER_ADMIN", response = Report.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RolesAllowed({Roles.SUPER_ADMIN})
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public Report get(@ApiParam(value = "Id of the instance", required = true)
                      @PathVariable("id") String id) {
        return reportService.find(id);
    }

    @ApiOperation(value = "Gets all instances. Roles.SUPER_ADMIN", response = ReportListDto.class, responseContainer = "list")
    @ApiResponses({@ApiResponse(code = 200, message = "Successful response")})
    @RolesAllowed({Roles.SUPER_ADMIN})
    @RequestMapping(method = RequestMethod.GET)
    public Collection<ReportListDto> listAll() {
        Collection<Report> all = reportService.findAll();
        List<ReportListDto> collect = all.stream().map(r -> new ReportListDto(r.getId(), r.getName(), r.getCreated(), r.getUpdated(), r.isArclibXmlDs())).collect(Collectors.toList());
        return collect;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public class ReportListDto {
        private String id;
        private String name;
        private Instant created;
        private Instant updated;
        private boolean arclibXmlDs;
    }

    @Inject
    public void setReportService(ReportService reportService) {
        this.reportService = reportService;
    }

    @Inject
    public void setExporter(ExporterService exporter) {
        this.exporter = exporter;
    }
}
