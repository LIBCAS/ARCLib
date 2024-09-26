package cz.cas.lib.arclib.formatlibrary.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import cz.cas.lib.arclib.formatlibrary.Permissions;
import cz.cas.lib.arclib.formatlibrary.domain.FormatDefinition;
import cz.cas.lib.arclib.formatlibrary.service.FormatLibraryUpdater;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.tuple.Pair;
import org.dom4j.DocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

@RestController
@Tag(name = "format library", description = "Api for interaction with format library entities")
@RequestMapping("/api/format_library")
public class FormatLibraryApi {
    private FormatLibraryUpdater formatLibraryUpdater;
    private UserDetails userDetails;

    @Operation(summary = "Update formats in format library with recent definitions from PRONOM server. [Perm.FORMAT_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response")})
    @RequestMapping(value = "/update_formats_from_external", method = RequestMethod.PUT)
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_WRITE + "')")
    public void updateFormatsFromExternal() throws ParseException, DocumentException {
        formatLibraryUpdater.updateFormatsFromExternal(userDetails.getUsername());
    }

    @Operation(summary = "Update format in format library with recent definition from PRONOM server. [Perm.FORMAT_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response")})
    @RequestMapping(value = "/update_format_from_external/{formatId}", method = RequestMethod.PUT)
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_WRITE + "')")
    public void updateFormatFromExternal(@Parameter(description = "Format id of the format", required = true)
                                         @PathVariable("formatId") Integer formatId) throws ParseException, DocumentException {
        formatLibraryUpdater.updateFormatFromExternal(userDetails.getUsername(), formatId);
    }

    @Operation(summary = "Update format with local format definition. [Perm.FORMAT_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response")})
    @RequestMapping(value = "/update_with_local_definition", method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_WRITE + "')")
    public Pair<FormatDefinition, String> updateFormatWithLocalDefinition(@Parameter(description = "Format definition", required = true)
                                                                          @RequestBody FormatDefinition format) {
        return formatLibraryUpdater.updateFormatWithLocalDefinition(format);
    }


    @Operation(summary = "Export format definition to JSON. [Perm.FORMAT_RECORDS_READ]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = ResponseEntity.class)))})
    @RequestMapping(value = "/export_format_definition_json/{formatDefinitionId}", method = RequestMethod.GET)
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_READ + "')")
    public ResponseEntity<InputStreamResource> exportFormatDefinitionToJson(@Parameter(description = "Format definition id", required = true)
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

    @Operation(summary = "Export format definition to byte array compressed in GZIP. [Perm.FORMAT_RECORDS_READ]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = ResponseEntity.class)))})
    @RequestMapping(value = "/export_format_definition_byte_array/{formatDefinitionId}", method = RequestMethod.GET)
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_READ + "')")
    public ResponseEntity<byte[]> exportFormatDefinitionToByteArray(@Parameter(description = "Format definition id", required = true)
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

    @Operation(summary = "Export format definitions to JSON. [Perm.FORMAT_RECORDS_READ]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = ResponseEntity.class)))})
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

    @Operation(summary = "Export format definitions to byte array compressed in GZIP. [Perm.FORMAT_RECORDS_READ]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = ResponseEntity.class)))})
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


    @Operation(summary = "Import format definition from JSON. [Perm.FORMAT_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = Pair.class)))})
    @RequestMapping(value = "/import_format_definition_json", method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_WRITE + "')")
    public Pair<FormatDefinition, String> importFormatDefinitionFromJson(@Parameter(description = "Format definition in JSON", required = true)
                                                                         @RequestBody String json) throws IOException {
        return formatLibraryUpdater.importFormatDefinitionFromJson(json);
    }

    @Operation(summary = "Import format definition from byte array compressed in GZIP. [Perm.FORMAT_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = Pair.class)))})
    @RequestMapping(value = "/import_format_definition_byte_array", method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_WRITE + "')")
    public Pair<FormatDefinition, String> importFormatDefinitionFromByteArray(@Parameter(description = "Format definition in byte array", required = true)
                                                                              @RequestBody byte[] bytes) throws IOException {
        return formatLibraryUpdater.importFormatDefinitionFromByteArray(bytes);
    }

    @Operation(summary = "Import format definitions from JSON. [Perm.FORMAT_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = List.class)))})
    @RequestMapping(value = "/import_format_definitions_json", method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_WRITE + "')")
    public List<Pair<FormatDefinition, String>> importFormatDefinitionsFromJson(@Parameter(description = "Format definitions in JSON", required = true)
                                                                                @RequestBody String json) throws IOException {
        return formatLibraryUpdater.importFormatDefinitionsFromJson(json);
    }

    @Operation(summary = "Import format definitions from byte array compressed in GZIP. [Perm.FORMAT_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = List.class)))})
    @RequestMapping(value = "/import_format_definitions_byte_array", method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_WRITE + "')")
    public List<Pair<FormatDefinition, String>> importFormatDefinitionsFromByteArray(@Parameter(description = "Format definitions in byte array", required = true)
                                                                                     @RequestBody byte[] bytes) throws IOException {
        return formatLibraryUpdater.importFormatDefinitionsFromByteArray(bytes);
    }

    @Autowired
    public void setFormatLibraryUpdater(FormatLibraryUpdater formatLibraryUpdater) {
        this.formatLibraryUpdater = formatLibraryUpdater;
    }

    @Autowired
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }
}
