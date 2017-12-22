package cz.inqool.uas.report;

import cz.inqool.uas.file.FileRef;
import cz.inqool.uas.file.FileRepository;
import cz.inqool.uas.report.location.ReportLocation;
import cz.inqool.uas.report.provider.ReportProvider;
import cz.inqool.uas.rest.DictionaryApi;
import cz.inqool.uas.store.Transactional;
import io.swagger.annotations.*;
import lombok.Getter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

/**
 * Api for managing and producing reports.
 *
 */
@RestController
@Api(value = "report", description = "Endpoint for reports management (main attribute: name).")
@RequestMapping("/api/reports")
public class ReportApi implements DictionaryApi<Report> {
    private ReportGenerator generator;

    @Getter
    private ReportStore adapter;

    /**
     * Lists all {@link Report}s for specified {@link ReportLocation}.
     *
     * @param location Specified location
     * @return {@link List} of {@link Report}
     */
    @ApiOperation(value = "Lists all reports for specified report location", response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = List.class)})
    @RequestMapping(value = "/public", method = RequestMethod.GET)
    @Transactional
    public List<Report> list(@ApiParam(value = "Specified location", required = true)
                                 @RequestParam("location") String location) {
        return adapter.findByLocation(location);
    }

    /**
     * Generates {@link Report} specified by id and {@link Map} of params and returns it to the caller.
     *
     * <p>
     *     Report generation contains three stages of params supplying:
     * </p>
     * <ol>
     *     <li>User supplied through ui</li>
     *     <li>Report globally supplied</li>
     *     <li>Provided by selected {@link ReportProvider}</li>
     * </ol>
     *
     * @param reportId Id of the {@link Report} to generate
     * @param params User supplied parameters
     * @return {@link ResponseEntity} containing generated content
     */
    @ApiOperation(value = "Generates report specified by id and map of params and returns it to the caller.",
            notes = "Returns response entity containing generated content.", response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = ResponseEntity.class)})
    @RequestMapping(value = "/{id}/generate", method = RequestMethod.POST)
    @Transactional
    public ResponseEntity<byte[]> generate(@ApiParam(value = "Id of the report to generate", required = true)
                                               @PathVariable("id") String reportId,
                                           @ApiParam(value = "User supplied parameters", required = true)
                                               @RequestBody Map<String, Object> params) {
        GeneratedReport report = generator.generate(reportId, params);

        return ResponseEntity
                .ok()
                .header("Content-Disposition", "attachment; filename=\"" + report.getName() + "\"")
                .header("Content-Length", String.valueOf(report.content.length))
                .contentType(MediaType.parseMediaType(report.contentType))
                .body(report.content);
    }

    /**
     * Generates {@link Report} specified by id and {@link Map} of params and stores it as a file in
     * {@link FileRepository}.
     *
     * <p>
     *     Report generation contains three stages of params supplying:
     * </p>
     * <ol>
     *     <li>User supplied through ui</li>
     *     <li>Report globally supplied</li>
     *     <li>Provided by selected {@link ReportProvider}</li>
     * </ol>
     *
     * <p>
     *     User can also specify if the content indexing should happen.
     * </p>
     *
     * @param reportId Id of the {@link Report} to generate
     * @param index Should the content be indexed
     * @param params User supplied parameters
     * @return {@link ResponseEntity} containing generated content
     */
    @ApiOperation(value = "Generates report specified by id and map of params" +
            " and stores it as a file in file repository.",
            notes = "Returns response entity containing generated content.", response = FileRef.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = FileRef.class)})
    @RequestMapping(value = "/{id}/generateToFile", method = RequestMethod.POST)
    @Transactional
    public FileRef generateToFile(@ApiParam(value = "Id of the report to generate", required = true)
                                      @PathVariable("id") String reportId,
                                  @ApiParam(value = "Should the content be indexed", required = true)
                                      @RequestParam("index") boolean index,
                                  @ApiParam(value = "User supplied parameters", required = true)
                                      @RequestBody Map<String, Object> params) {
        return generator.generateToFile(reportId, params, index);
    }

    @Inject
    public void setGenerator(ReportGenerator generator) {
        this.generator = generator;
    }

    @Inject
    public void setAdapter(ReportStore store) {
        this.adapter = store;
    }
}
