package cz.cas.lib.arclib.formatlibrary.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import cz.cas.lib.arclib.formatlibrary.Permissions;
import cz.cas.lib.arclib.formatlibrary.domain.FormatDefinition;
import cz.cas.lib.arclib.formatlibrary.service.FormatLibraryUpdater;
import io.swagger.annotations.*;
import org.apache.commons.lang3.tuple.Pair;
import org.dom4j.DocumentException;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

@RestController
@Api(value = "format library", description = "Api for interaction with format library entities")
@RequestMapping("/api/format_library")
public class FormatLibraryApi {
    private FormatLibraryUpdater formatLibraryUpdater;
    private UserDetails userDetails;

    @ApiOperation(value = "Update formats in format library with recent definitions from PRONOM server. [Perm.FORMAT_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")})
    @RequestMapping(value = "/update_formats_from_external", method = RequestMethod.PUT)
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_WRITE + "')")
    public void updateFormatsFromExternal() throws ParseException, DocumentException {
        formatLibraryUpdater.updateFormatsFromExternal(userDetails.getUsername());
    }

    @ApiOperation(value = "Update format in format library with recent definition from PRONOM server. [Perm.FORMAT_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")})
    @RequestMapping(value = "/update_format_from_external/{formatId}", method = RequestMethod.PUT)
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_WRITE + "')")
    public void updateFormatFromExternal(@ApiParam(value = "Format id of the format", required = true)
                                         @PathVariable("formatId") Integer formatId) throws ParseException, DocumentException {
        formatLibraryUpdater.updateFormatFromExternal(formatId);
    }

    @ApiOperation(value = "Update format with local format definition. [Perm.FORMAT_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")})
    @RequestMapping(value = "/update_with_local_definition", method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_WRITE + "')")
    public Pair<FormatDefinition, String> updateFormatWithLocalDefinition(@ApiParam(value = "Format definition", required = true)
                                                                                @RequestBody FormatDefinition format) {
        return formatLibraryUpdater.updateFormatWithLocalDefinition(format);
    }


    @ApiOperation(value = "Export format definition to JSON. [Perm.FORMAT_RECORDS_READ]",
            response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = ResponseEntity.class)})
    @RequestMapping(value = "/export_format_definition_json/{formatDefinitionId}", method = RequestMethod.GET)
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_READ + "')")
    public ResponseEntity<InputStreamResource> exportFormatDefinitionToJson(@ApiParam(value = "Format definition id", required = true)
                                                                            @PathVariable("formatDefinitionId") String formatDefinitionId)
            throws JsonProcessingException {
        String json = formatLibraryUpdater.exportFormatDefinitionToJson(formatDefinitionId);

        return ResponseEntity
                .ok()
                .header("Content-Disposition", "attachment; filename=" + "format_definition_" + formatDefinitionId + ".json")
                .header("Content-Length", String.valueOf((long) json.getBytes().length))
                .contentType(MediaType.parseMediaType("application/json"))
                .body(new InputStreamResource(new ByteArrayInputStream(json.getBytes())));
    }

    @ApiOperation(value = "Export format definition to byte array compressed in GZIP. [Perm.FORMAT_RECORDS_READ]",
            response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = ResponseEntity.class)})
    @RequestMapping(value = "/export_format_definition_byte_array/{formatDefinitionId}", method = RequestMethod.GET)
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_READ + "')")
    public ResponseEntity<byte[]> exportFormatDefinitionToByteArray(@ApiParam(value = "Format definition id", required = true)
                                                                    @PathVariable("formatDefinitionId") String formatDefinitionId)
            throws JsonProcessingException {
        byte[] bytes = formatLibraryUpdater.exportFormatDefinitionToByteArray(formatDefinitionId);

        return ResponseEntity
                .ok()
                .header("Content-Disposition", "attachment; filename=" + "format_definition_" + formatDefinitionId + ".bytes")
                .header("Content-Length", String.valueOf((long) bytes.length))
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(bytes);
    }

    @ApiOperation(value = "Export format definitions to JSON. [Perm.FORMAT_RECORDS_READ]",
            response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = ResponseEntity.class)})
    @RequestMapping(value = "/export_format_definitions_json", method = RequestMethod.GET)
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_READ + "')")
    public ResponseEntity<InputStreamResource> exportFormatDefinitionsToJson() throws JsonProcessingException {
        String json = formatLibraryUpdater.exportFormatDefinitionsToJson();

        return ResponseEntity
                .ok()
                .header("Content-Disposition", "attachment; filename=" + "format_definitions.json")
                .header("Content-Length", String.valueOf((long) json.getBytes().length))
                .contentType(MediaType.parseMediaType("application/json"))
                .body(new InputStreamResource(new ByteArrayInputStream(json.getBytes())));
    }

    @ApiOperation(value = "Export format definitions to byte array compressed in GZIP. [Perm.FORMAT_RECORDS_READ]",
            response = ResponseEntity.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = ResponseEntity.class)})
    @RequestMapping(value = "/export_format_definitions_byte_array", method = RequestMethod.GET)
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_READ + "')")
    public ResponseEntity<byte[]> exportFormatDefinitionsToByteArray() throws JsonProcessingException {
        byte[] bytes = formatLibraryUpdater.exportFormatDefinitionsToByteArray();

        return ResponseEntity
                .ok()
                .header("Content-Disposition", "attachment; filename=" + "format_definitions.bytes")
                .header("Content-Length", String.valueOf((long) bytes.length))
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(bytes);
    }


    @ApiOperation(value = "Import format definition from JSON. [Perm.FORMAT_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Pair.class)})
    @RequestMapping(value = "/import_format_definition_json", method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_WRITE + "')")
    public Pair<FormatDefinition, String> importFormatDefinitionFromJson(@ApiParam(value = "Format definition in JSON", required = true)
                                                                               @RequestBody String json) throws IOException {
        return formatLibraryUpdater.importFormatDefinitionFromJson(json);
    }

    @ApiOperation(value = "Import format definition from byte array compressed in GZIP. [Perm.FORMAT_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Pair.class)})
    @RequestMapping(value = "/import_format_definition_byte_array", method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_WRITE + "')")
    public Pair<FormatDefinition, String> importFormatDefinitionFromByteArray(@ApiParam(value = "Format definition in byte array", required = true)
                                                                                    @RequestBody byte[] bytes) throws IOException {
        return formatLibraryUpdater.importFormatDefinitionFromByteArray(bytes);
    }

    @ApiOperation(value = "Import format definitions from JSON. [Perm.FORMAT_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = List.class)})
    @RequestMapping(value = "/import_format_definitions_json", method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_WRITE + "')")
    public List<Pair<FormatDefinition, String>> importFormatDefinitionsFromJson(@ApiParam(value = "Format definitions in JSON", required = true)
                                                                                      @RequestBody String json) throws IOException {
        return formatLibraryUpdater.importFormatDefinitionsFromJson(json);
    }

    @ApiOperation(value = "Import format definitions from byte array compressed in GZIP. [Perm.FORMAT_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = List.class)})
    @RequestMapping(value = "/import_format_definitions_byte_array", method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_WRITE + "')")
    public List<Pair<FormatDefinition, String>> importFormatDefinitionsFromByteArray(@ApiParam(value = "Format definitions in byte array", required = true)
                                                                                           @RequestBody byte[] bytes) throws IOException {
        return formatLibraryUpdater.importFormatDefinitionsFromByteArray(bytes);
    }

    @Inject
    public void setFormatLibraryUpdater(FormatLibraryUpdater formatLibraryUpdater) {
        this.formatLibraryUpdater = formatLibraryUpdater;
    }

    @Inject
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }
}
