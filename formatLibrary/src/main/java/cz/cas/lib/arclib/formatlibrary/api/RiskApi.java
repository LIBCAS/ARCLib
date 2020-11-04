package cz.cas.lib.arclib.formatlibrary.api;

import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.formatlibrary.Permissions;
import cz.cas.lib.arclib.formatlibrary.domain.Format;
import cz.cas.lib.arclib.formatlibrary.domain.Risk;
import cz.cas.lib.arclib.formatlibrary.service.FormatService;
import cz.cas.lib.arclib.formatlibrary.store.RiskStore;
import io.swagger.annotations.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

import static cz.cas.lib.arclib.domainbase.util.DomainBaseUtils.eq;
import static cz.cas.lib.arclib.domainbase.util.DomainBaseUtils.notNull;

@RestController
@Api(value = "risk", description = "Api for interaction with risks")
@RequestMapping("/api/risk")
public class RiskApi {
    private RiskStore store;
    private FormatService formatService;

    @ApiOperation(value = "Saves an instance. [Perm.RISK_RECORDS_WRITE]", notes = "Returns single instance (possibly with computed attributes)",
            response = Risk.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Risk.class),
            @ApiResponse(code = 400, message = "Specified id does not correspond to the id of the instance")})
    @PreAuthorize("hasAuthority('" + Permissions.RISK_RECORDS_WRITE + "')")
    @PutMapping(value = "/{id}")
    @Transactional
    public Risk save(@ApiParam(value = "Id of the instance", required = true)
                     @PathVariable("id") String id,
                     @ApiParam(value = "Single instance", required = true)
                     @RequestBody Risk request) {
        eq(id, request.getId(), () -> new BadArgument("id"));

        return store.save(request);
    }

    @ApiOperation(value = "Deletes an instance. [Perm.RISK_RECORDS_WRITE]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.RISK_RECORDS_WRITE + "')")
    @DeleteMapping(value = "/{id}")
    @Transactional
    public void delete(@ApiParam(value = "Id of the instance", required = true)
                       @PathVariable("id") String id) {
        Risk risk = store.find(id);
        notNull(risk, () -> new MissingObject(store.getType(), id));

        store.delete(risk);
    }

    @ApiOperation(value = "Gets one instance specified by id [Perm.RISK_RECORDS_READ]", response = Risk.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Risk.class),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.RISK_RECORDS_READ + "')")
    @GetMapping(value = "/{id}")
    @Transactional
    public Risk get(@ApiParam(value = "Id of the instance", required = true)
                    @PathVariable("id") String id) {
        Risk entity = store.find(id);
        notNull(entity, () -> new MissingObject(store.getType(), id));

        return entity;
    }

    @ApiOperation(value = "Gets all instances that respect the selected parameters [Perm.RISK_RECORDS_READ]",
            response = Collection.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Successful response", response = Collection.class)})
    @PreAuthorize("hasAuthority('" + Permissions.RISK_RECORDS_READ + "')")
    @GetMapping
    @Transactional
    public Collection<Risk> list() {
        return store.findAll();
    }

    @ApiOperation(value = "Gets formats related to risk [Perm.RISK_RECORDS_READ]",
            response = Format.class, responseContainer = "List")
    @PreAuthorize("hasAuthority('" + Permissions.RISK_RECORDS_READ + "')")
    @GetMapping(value = "/{id}/related_formats")
    public List<Format> listFormatsOfRisk(
            @ApiParam(value = "Id of the risk", required = true) @PathVariable("id") String id) {
        return formatService.findFormatsOfRisk(id);
    }

    @Inject
    public void setStore(RiskStore store) {
        this.store = store;
    }

    @Inject
    public void setFormatService(FormatService formatService) {
        this.formatService = formatService;
    }
}
