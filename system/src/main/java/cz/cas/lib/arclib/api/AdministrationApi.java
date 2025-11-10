package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.reingest.Reingest;
import cz.cas.lib.arclib.exception.ReingestInProgressException;
import cz.cas.lib.arclib.exception.ReingestStateException;
import cz.cas.lib.arclib.index.solr.ReindexService;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.service.ReingestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@Tag(name = "administration", description = "Api for administration purposes")
@RequestMapping("/api/administration")
@Slf4j
public class AdministrationApi {

    @Autowired
    private ReindexService solrReindexService;

    @Autowired
    private ReingestService reingestService;

    @Operation(summary = "deletes all old index records and creates index for all entities in db [Perm.REINDEX_ELIGIBILITY]",
            description = "arclibxml and format entities are omitted as there are too many records")
    @PreAuthorize("hasAuthority('" + Permissions.REINDEX_ELIGIBILITY + "')")
    @RequestMapping(value = "/reindex/core", method = RequestMethod.POST)
    public void reindexCore() {
        solrReindexService.dropReindexAll();
    }

    @Operation(summary = "creates index for all arclibxmls [Perm.REINDEX_ELIGIBILITY]")
    @PreAuthorize("hasAuthority('" + Permissions.REINDEX_ELIGIBILITY + "')")
    @RequestMapping(value = "/reindex/arclib_xml", method = RequestMethod.POST)
    public void reindexArclibXml() {
        solrReindexService.reindexArclibXml();
    }

    @Operation(summary = "deletes all old Format index records and creates index for all Format entities in db [Perm.REINDEX_ELIGIBILITY]")
    @PreAuthorize("hasAuthority('" + Permissions.REINDEX_ELIGIBILITY + "')")
    @RequestMapping(value = "/reindex/format", method = RequestMethod.POST)
    public void reindexFormat() {
        solrReindexService.dropReindexFormat();
    }

    @Operation(summary = "gets current reingest (if there is any) [Perm.REINGEST_ELIGIBILITY]")
    @PreAuthorize("hasAuthority('" + Permissions.REINGEST_ELIGIBILITY + "')")
    @RequestMapping(value = "/reingest", method = RequestMethod.GET)
    public Reingest getReingest() {
        return reingestService.getCurrent();
    }

    @Operation(summary = "initates new reingest, fails if other already exists [Perm.REINGEST_ELIGIBILITY]")
    @PreAuthorize("hasAuthority('" + Permissions.REINGEST_ELIGIBILITY + "')")
    @RequestMapping(value = "/reingest/init", method = RequestMethod.POST)
    public Reingest initReingest() throws ReingestStateException, IOException {
        return reingestService.initiateReingest();
    }

    @Operation(summary = "resumes reingest job [Perm.REINGEST_ELIGIBILITY]")
    @PreAuthorize("hasAuthority('" + Permissions.REINGEST_ELIGIBILITY + "')")
    @RequestMapping(value = "/reingest/resume", method = RequestMethod.POST)
    public Reingest resumeReingestExport() throws ReingestStateException {
        return reingestService.startReingestJob();
    }

    @Operation(summary = "stops reingest job [Perm.REINGEST_ELIGIBILITY]")
    @PreAuthorize("hasAuthority('" + Permissions.REINGEST_ELIGIBILITY + "')")
    @RequestMapping(value = "/reingest/stop", method = RequestMethod.POST)
    public Reingest stopReingestExport() throws ReingestStateException {
        return reingestService.stopReingestJob();
    }

    @Operation(summary = "counts reingest packages in transfer area [Perm.REINGEST_ELIGIBILITY]")
    @PreAuthorize("hasAuthority('" + Permissions.REINGEST_ELIGIBILITY + "')")
    @RequestMapping(value = "/reingest/count", method = RequestMethod.GET)
    public Long countReingestPackages() throws ReingestStateException {
        return reingestService.countPackagesInTransferArea();
    }

    @Operation(summary = "terminates successful or unsuccessful reingest [Perm.REINGEST_ELIGIBILITY]")
    @PreAuthorize("hasAuthority('" + Permissions.REINGEST_ELIGIBILITY + "')")
    @RequestMapping(value = "/reingest/terminate", method = RequestMethod.POST)
    public void terminateReingest() throws ReingestStateException, IOException, ReingestInProgressException {
        reingestService.terminateReingest();
    }

}
