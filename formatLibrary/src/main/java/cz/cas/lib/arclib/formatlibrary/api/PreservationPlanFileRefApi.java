package cz.cas.lib.arclib.formatlibrary.api;

import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.formatlibrary.Permissions;
import cz.cas.lib.arclib.formatlibrary.domain.PreservationPlanFileRef;
import cz.cas.lib.arclib.formatlibrary.service.PreservationPlanFileRefService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.io.InputStream;

import static cz.cas.lib.arclib.domainbase.util.DomainBaseUtils.notNull;

/**
 * Somewhat mirrored version of <b>FileRefApi</b> from module <b>system</b>.
 *
 * Api for accessing and storing files {@link PreservationPlanFileRef}
 */
@Slf4j
@RestController
@Tag(name = "file", description = "Api for accessing and storing format files. [Mirror of /api/files]")
@RequestMapping("/api/format_files/")
public class PreservationPlanFileRefApi {

    private PreservationPlanFileRefService service;

    /**
     * Gets the content of a file with specified id.
     *
     * Also sets Content-Length and Content-Disposition http headers to values previously saved during upload.
     *
     * @param id Id of file to retrieve
     * @return Content of a file in input stream
     * @throws MissingObject if the file was not found
     */
    @Operation(summary = "Gets the content of a file with specified id.",
            description = "Returns content of a file in input stream.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = ResponseEntity.class))),
            @ApiResponse(responseCode = "404", description = "The file was not found")
    })
    @GetMapping(value = "/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_READ + "')")
    public ResponseEntity<InputStreamResource> download(@Parameter(description = "File ID", required = true) @PathVariable("id") String id) {

        PreservationPlanFileRef file = service.get(id);
        notNull(file, () -> new MissingObject(PreservationPlanFileRef.class, id));

        return ResponseEntity
                .ok()
                .header("Content-Disposition", "attachment; filename=" + file.getName())
                .header("Content-Length", String.valueOf(file.getSize()))
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .body(new InputStreamResource(file.getStream()));
    }

    /**
     * Uploads a file.
     *
     * File should be uploaded as multipart/form-data.
     *
     * @param uploadFile Provided file with metadata
     * @return Reference to a stored file
     */
    @Operation(summary = "Uploads a file and returns the reference to the stored file.",
            description = "File should be uploaded as multipart/form-data.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = PreservationPlanFileRef.class)))
    })
    @PostMapping(value = "/")
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_WRITE + "')")
    public PreservationPlanFileRef upload(@Parameter(description = "Provided file with metadata", required = true) @RequestParam("file") MultipartFile uploadFile) {

        try (InputStream stream = uploadFile.getInputStream()) {
            String filename = uploadFile.getOriginalFilename();

            if (filename != null) {
                filename = FilenameUtils.getName(filename);
            }

            String contentType = uploadFile.getContentType();

            return service.create(stream, filename, contentType);

        } catch (IOException e) {
            throw new BadArgument("file");
        }
    }

    @Autowired
    public void setService(PreservationPlanFileRefService service) {
        this.service = service;
    }
}