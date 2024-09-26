package cz.cas.lib.core.file;

import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

import static cz.cas.lib.core.util.Utils.notNull;

/**
 * Api for accessing and storing files.
 */
@Slf4j
@RestController
@Tag(name = "file", description = "Api for accessing and storing files")
@RequestMapping("/api/files")
public class FileRefApi {

    private FileRefService service;

    /**
     * Gets the content of a file with specified id.
     *
     * <p>
     *     Also sets Content-Length and Content-Disposition http headers to values previously saved during upload.
     * </p>
     *
     * @param id Id of file to retrieve
     * @return Content of a file in input stream
     * @throws MissingObject if the file was not found
     */
    @Operation(summary = "Gets the content of a file with specified id.",
            description = "Returns content of a file in input stream.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = ResponseEntity.class))),
            @ApiResponse(responseCode = "404", description = "The file was not found")})
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<InputStreamResource> download(@Parameter(description = "Id of file to retrieve", required = true) @PathVariable("id") String id) {

        FileRef file = service.get(id);
        notNull(file, () -> new MissingObject(FileRef.class, id));

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
     * <p>
     *     File should be uploaded as multipart/form-data.
     * </p>
     *
     * @param uploadFile Provided file with metadata
     * @param index      Should be the content of file indexed
     * @return Reference to a stored file
     */
    @Operation(summary = "Uploads a file and returns the reference to the stored file.",
            description = "File should be uploaded as multipart/form-data.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = FileRef.class)))})
    @RequestMapping(value = "/", method = RequestMethod.POST)
    public FileRef upload(@Parameter(description = "Provided file with metadata", required = true) @RequestParam("file") MultipartFile uploadFile,
                          @Parameter(description = "Should be the content of file indexed") @RequestParam(name = "index", defaultValue = "false") Boolean index) {

        try (InputStream stream = uploadFile.getInputStream()) {
            String filename = uploadFile.getOriginalFilename();

            if (filename != null) {
                filename = FilenameUtils.getName(filename);
            }

            String contentType = uploadFile.getContentType();

            return service.create(stream, filename, contentType, index);

        } catch (IOException e) {
            throw new BadArgument("file");
        }
    }

    @Autowired
    public void setService(FileRefService service) {
        this.service = service;
    }
}
