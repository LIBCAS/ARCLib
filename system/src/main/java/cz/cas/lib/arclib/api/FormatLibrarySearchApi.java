package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.preservationPlanning.FormatOccurrence;
import cz.cas.lib.arclib.formatlibrary.domain.Format;
import cz.cas.lib.arclib.formatlibrary.domain.FormatDefinition;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.store.FormatOccurrenceStore;
import cz.cas.lib.arclib.store.IndexedFormatDefinitionStore;
import cz.cas.lib.arclib.store.IndexedFormatStore;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import io.swagger.annotations.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.List;

@RestController
@Api(value = "format", description = "Api for interaction with formats")
@RequestMapping("/api/search/format_library")
public class FormatLibrarySearchApi {

    private IndexedFormatStore indexedFormatStore;
    private IndexedFormatDefinitionStore indexedFormatDefinitionStore;
    private FormatOccurrenceStore formatOccurrenceStore;

    @ApiOperation(value = "Gets all instances that respect the selected parameters [Perm.FORMAT_RECORDS_READ]",
            notes = "Filter/Sort fields = id, formatId, puid, formatName, formatVersion, internalVersionNumber," +
                    " localDefinition, preferred, internalInformationFilled",
            response = Result.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Successful response", response = Result.class)})
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_READ + "')")
    @RequestMapping(method = RequestMethod.GET, value = "/format")
    public Result<Format> listFormats(@ApiParam(value = "Parameters to comply with", required = true)
                                      @ModelAttribute Params params) {
        return indexedFormatStore.findAll(params);
    }

    @ApiOperation(value = "Gets all instances that respect the selected parameters [Perm.FORMAT_RECORDS_READ]",
            notes = "Filter/Sort fields = formatId, puid, formatVersion, internalVersionNumber," +
                    " localDefinition, preferred, internalInformationFilled",
            response = Result.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Successful response", response = Result.class)})
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_READ + "')")
    @RequestMapping(method = RequestMethod.GET, value = "/format_definition")
    public Result<FormatDefinition> listFormatDefinitions(@ApiParam(value = "Parameters to comply with", required = true)
                                                          @ModelAttribute Params params) {
        return indexedFormatDefinitionStore.findAll(params);
    }

    @ApiOperation(value = "Gets all occurrences of the particular format definition through all producer profiles [Perm.FORMAT_RECORDS_READ]",
            response = FormatOccurrence.class, responseContainer = "List")
    @PreAuthorize("hasAuthority('" + Permissions.FORMAT_RECORDS_READ + "')")
    @RequestMapping(method = RequestMethod.GET, value = "/format_definition/{id}/occurrences")
    public List<FormatOccurrence> findOccurrencesOfFormatDef(
            @ApiParam(value = "Id of the format definition", required = true)
            @PathVariable("id") String id) {
        return formatOccurrenceStore.findAllOfFormatDefinition(id);
    }

    @Inject
    public void setIndexedFormatStore(IndexedFormatStore indexedFormatStore) {
        this.indexedFormatStore = indexedFormatStore;
    }

    @Inject
    public void setIndexedFormatDefinitionStore(IndexedFormatDefinitionStore indexedFormatDefinitionStore) {
        this.indexedFormatDefinitionStore = indexedFormatDefinitionStore;
    }

    @Inject
    public void setFormatOccurrenceStore(FormatOccurrenceStore formatOccurrenceStore) {
        this.formatOccurrenceStore = formatOccurrenceStore;
    }
}
