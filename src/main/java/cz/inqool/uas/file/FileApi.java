package cz.inqool.uas.file;

import cz.inqool.uas.exception.BadArgument;
import cz.inqool.uas.exception.MissingObject;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;

import static cz.inqool.uas.util.Utils.notNull;

/**
 * Api for accessing and storing files.
 */
@Slf4j
@RestController
@Api(value = "file", description = "Api for accessing and storing files")
@RequestMapping("/api/files")
public class FileApi {
    private FileRepository repository;

    /**
     * Gets the content of a file with specified id.
     *
     * <p>
     *     Also sets Content-Length and Content-Disposition http headers to values previously saved during upload.
     * </p>
     * @param id Id of file to retrieve
     * @throws MissingObject if the file was not found
     * @return Content of a file in input stream
     */
    @ApiOperation(value = "Gets the content of a file with specified id.",
            notes = "Returns content of a file in input stream.",
            response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = ResponseEntity.class),
            @ApiResponse(code = 404, message = "The file was not found")})
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<InputStreamResource> download(@ApiParam(value = "Id of file to retrieve", required = true)
                                                            @PathVariable("id") String id) {

        FileRef file = repository.get(id);
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
     * @param uploadFile Provided file with metadata
     * @param index Should be the content of file indexed
     * @return Reference to a stored file
     */
    @ApiOperation(value = "Uploads a file and returns the reference to the stored file.",
            notes = "File should be uploaded as multipart/form-data.",
            response = FileRef.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = FileRef.class)})
    @RequestMapping(value = "/", method = RequestMethod.POST)
    public FileRef upload(@ApiParam(value = "Provided file with metadata", required = true)
                              @RequestParam("file") MultipartFile uploadFile,
                          @ApiParam(value = "Should be the content of file indexed")
                              @RequestParam(name = "index", defaultValue = "false") Boolean index) {

        try (InputStream stream = uploadFile.getInputStream()) {
            String filename = uploadFile.getOriginalFilename();

            if (filename != null) {
                filename = FilenameUtils.getName(filename);
            }

            String contentType = uploadFile.getContentType();

            return repository.create(stream, filename, contentType, index);

        } catch (IOException e) {
            throw new BadArgument("file");
        }
    }

    @Inject
    public void setRepository(FileRepository repository) {
        this.repository = repository;
    }
}
