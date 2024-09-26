package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.report.ExportFormat;
import cz.cas.lib.arclib.report.ExporterService;
import cz.cas.lib.arclib.report.Report;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
@Tag(name = "report", description = "Api for interaction with reports")
@RequestMapping("/api/report")
public class ReportApi {

    private ReportService reportService;
    private ExporterService exporter;

    @Operation(summary = "Finds report template by ID and exports it to specified format. [Perm.REPORT_TEMPLATE_RECORDS_READ]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "400", description = "Given parameter is not defined within template or the value can not be parsed")})
    @PreAuthorize("hasAuthority('" + Permissions.REPORT_TEMPLATE_RECORDS_READ + "')")
    @RequestMapping(value = "/{reportId}/{format}", method = RequestMethod.GET)
    public void getReport(@Parameter(description = "Id of the report", required = true) @PathVariable("reportId") String reportId,
                          @Parameter(description = "Output format, one of CSV, XLSX, HTML, PDF", required = true) @PathVariable("format") String format,
                          @Parameter(description = "Query string with parameters used to override default parameters values" +
                                  " which are specified inside template itself. Boolean parameter value should be in" +
                                  " string form i.e. true/false") @RequestParam(required = false) Map<String, String> params,
                          HttpServletResponse response) throws IOException {
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

    @Operation(summary = "Compiles and stores report template. [Perm.REPORT_TEMPLATE_RECORDS_WRITE]", description = "If the arclibXmlDs query param is set to true, " +
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
            "</ul>")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = Report.class))),
            @ApiResponse(responseCode = "400", description = "Parameters validation failed.")})
    @PreAuthorize("hasAuthority('" + Permissions.REPORT_TEMPLATE_RECORDS_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public Report save(@Parameter(description = "Id of the instance", required = true) @PathVariable("id") String id,
                       @Parameter(description = "Single instance", required = true) @RequestBody Report request) throws IOException {
        eq(id, request.getId(), () -> new BadArgument("id"));
        return reportService.saveReport(request);
    }

    @Operation(summary = "Deletes an instance. [Perm.REPORT_TEMPLATE_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "404", description = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.REPORT_TEMPLATE_RECORDS_WRITE + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void delete(@Parameter(description = "Id of the instance", required = true)
                       @PathVariable("id") String id) {
        reportService.delete(id);
    }

    @Operation(summary = "Gets one instance specified by id. [Perm.REPORT_TEMPLATE_RECORDS_READ]", responses = {@ApiResponse(content = @Content(schema = @Schema(implementation = Report.class)))})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "404", description = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.REPORT_TEMPLATE_RECORDS_READ + "')")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public Report get(@Parameter(description = "Id of the instance", required = true)
                      @PathVariable("id") String id) {
        return reportService.find(id);
    }

    @Operation(summary = "Gets all instances. [Perm.REPORT_TEMPLATE_RECORDS_READ]")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Successful response", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReportListDto.class))))})
    @PreAuthorize("hasAuthority('" + Permissions.REPORT_TEMPLATE_RECORDS_READ + "')")
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

    @Autowired
    public void setReportService(ReportService reportService) {
        this.reportService = reportService;
    }

    @Autowired
    public void setExporter(ExporterService exporter) {
        this.exporter = exporter;
    }
}
