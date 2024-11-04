package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.preservationPlanning.FormatOccurrence;
import cz.cas.lib.arclib.formatlibrary.domain.Format;
import cz.cas.lib.arclib.formatlibrary.domain.FormatDefinition;
import cz.cas.lib.arclib.formatlibrary.store.FormatDefinitionStore;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.store.FormatOccurrenceStore;
import cz.cas.lib.arclib.store.IndexedFormatStore;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "format", description = "Api for interaction with formats")
@RequestMapping("/api/search/format_library")
public class FormatLibrarySearchApi {

    private IndexedFormatStore indexedFormatStore;
    private FormatDefinitionStore formatDefinitionStore;
    private FormatOccurrenceStore formatOccurrenceStore;

    @Operation(summary = "Gets all instances that respect the selected parameters [Perm.FORMAT_RECORDS_READ]",
            description = "Filter/Sort fields = id, formatId, puid, formatName, formatVersion, internalVersionNumber," +
                    " localDefinition, preferred, internalInformationFilled")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = Result.class)))})
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_READ + "')")
    @RequestMapping(method = RequestMethod.GET, value = "/format")
    public Result<Format> listFormats(@Parameter(description = "Parameters to comply with", required = true)
                                      @ModelAttribute Params params) {
        return indexedFormatStore.findAll(params);
    }

    @Operation(summary = "Gets all instances that respect the selected parameters [Perm.FORMAT_RECORDS_READ]",
            description = "Filter/Sort fields = formatId, puid, formatVersion, internalVersionNumber," +
                    " localDefinition, preferred, internalInformationFilled")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = Result.class)))})
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_READ + "')")
    @RequestMapping(method = RequestMethod.GET, value = "/format/{id}/definition")
    public Result<FormatDefinition> listFormatDefinitions(
            @Parameter(description = "Id of the format", required = true)
            @PathVariable("id") Integer id
    ) {
        List<FormatDefinition> defs = formatDefinitionStore.findByFormatId(id, null);
        return new Result<>(defs, (long) defs.size());
    }

    @Operation(summary = "Gets all occurrences of the particular format definition through all producer profiles [Perm.FORMAT_RECORDS_READ]",
            responses = {@ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = FormatOccurrence.class))))})
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_READ + "')")
    @RequestMapping(method = RequestMethod.GET, value = "/format_definition/{id}/occurrences")
    public List<FormatOccurrence> findOccurrencesOfFormatDef(
            @Parameter(description = "Id of the format definition", required = true)
            @PathVariable("id") String id) {
        return formatOccurrenceStore.findAllOfFormatDefinition(id);
    }

    @Autowired
    public void setIndexedFormatStore(IndexedFormatStore indexedFormatStore) {
        this.indexedFormatStore = indexedFormatStore;
    }

    @Autowired
    public void setFormatDefinitionStore(FormatDefinitionStore formatDefinitionStore) {
        this.formatDefinitionStore = formatDefinitionStore;
    }

    @Autowired
    public void setFormatOccurrenceStore(FormatOccurrenceStore formatOccurrenceStore) {
        this.formatOccurrenceStore = formatOccurrenceStore;
    }
}
