package cz.cas.lib.arclib.formatlibrary.api;

import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.formatlibrary.Permissions;
import cz.cas.lib.arclib.formatlibrary.domain.Format;
import cz.cas.lib.arclib.formatlibrary.domain.Risk;
import cz.cas.lib.arclib.formatlibrary.service.FormatService;
import cz.cas.lib.arclib.formatlibrary.store.RiskStore;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;


import java.util.Collection;
import java.util.List;

import static cz.cas.lib.arclib.domainbase.util.DomainBaseUtils.eq;
import static cz.cas.lib.arclib.domainbase.util.DomainBaseUtils.notNull;

@RestController
@Tag(name = "risk", description = "Api for interaction with risks")
@RequestMapping("/api/risk")
public class RiskApi {
    private RiskStore store;
    private FormatService formatService;

    @Operation(summary = "Saves an instance. [Perm.RISK_RECORDS_WRITE]", description = "Returns single instance (possibly with computed attributes)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = Risk.class))),
            @ApiResponse(responseCode = "400", description = "Specified id does not correspond to the id of the instance")})
    @PreAuthorize("hasAuthority('" + Permissions.RISK_RECORDS_WRITE + "')")
    @PutMapping(value = "/{id}")
    @Transactional
    public Risk save(@Parameter(description = "Id of the instance", required = true)
                     @PathVariable("id") String id,
                     @Parameter(description = "Single instance", required = true)
                     @RequestBody Risk request) {
        eq(id, request.getId(), () -> new BadArgument("id"));

        return store.save(request);
    }

    @Operation(summary = "Deletes an instance. [Perm.RISK_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "404", description = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.RISK_RECORDS_WRITE + "')")
    @DeleteMapping(value = "/{id}")
    @Transactional
    public void delete(@Parameter(description = "Id of the instance", required = true)
                       @PathVariable("id") String id) {
        Risk risk = store.find(id);
        notNull(risk, () -> new MissingObject(store.getType(), id));

        store.delete(risk);
    }

    @Operation(summary = "Gets one instance specified by id [Perm.RISK_RECORDS_READ]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = Risk.class))),
            @ApiResponse(responseCode = "404", description = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.RISK_RECORDS_READ + "')")
    @GetMapping(value = "/{id}")
    @Transactional
    public Risk get(@Parameter(description = "Id of the instance", required = true)
                    @PathVariable("id") String id) {
        Risk entity = store.find(id);
        notNull(entity, () -> new MissingObject(store.getType(), id));

        return entity;
    }

    @Operation(summary = "Gets all instances that respect the selected parameters [Perm.RISK_RECORDS_READ]")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Successful response", content = @Content(schema = @Schema(implementation = Collection.class)))})
    @PreAuthorize("hasAuthority('" + Permissions.RISK_RECORDS_READ + "')")
    @GetMapping
    @Transactional
    public Collection<Risk> list() {
        return store.findAll();
    }

    @Operation(summary = "Gets formats related to risk [Perm.RISK_RECORDS_READ]",
            responses = {@ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = Format.class))))})
    @PreAuthorize("hasAuthority('" + Permissions.RISK_RECORDS_READ + "')")
    @GetMapping(value = "/{id}/related_formats")
    public List<Format> listFormatsOfRisk(
            @Parameter(description = "Id of the risk", required = true) @PathVariable("id") String id) {
        return formatService.findFormatsOfRisk(id);
    }

    @Autowired
    public void setStore(RiskStore store) {
        this.store = store;
    }

    @Autowired
    public void setFormatService(FormatService formatService) {
        this.formatService = formatService;
    }
}
