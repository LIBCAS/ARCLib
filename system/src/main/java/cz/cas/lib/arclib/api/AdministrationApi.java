package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.index.solr.ReindexService;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;

@RestController
@Api(value = "administration", description = "Api for administration purposes")
@RequestMapping("/api/administration")
@Slf4j
public class AdministrationApi {

    @Inject
    private ReindexService solrReindexService;

    @ApiOperation(value = "deletes all old index records and creates index for all entities in db [Perm.REINDEX_ELIGIBILITY]",
            notes = "arclibxml and format entities are omitted as there are too many records")
    @PreAuthorize("hasAuthority('" + Permissions.REINDEX_ELIGIBILITY + "')")
    @RequestMapping(value = "/reindex/core", method = RequestMethod.POST)
    public void reindexCore() {
        solrReindexService.dropReindexAll();
    }

    @ApiOperation(value = "creates index for all arclibxmls [Perm.REINDEX_ELIGIBILITY]")
    @PreAuthorize("hasAuthority('" + Permissions.REINDEX_ELIGIBILITY + "')")
    @RequestMapping(value = "/reindex/arclib_xml", method = RequestMethod.POST)
    public void reindexArclibXml() {
        solrReindexService.reindexArclibXml();
    }

    @ApiOperation(value = "deletes all old Format index records and creates index for all Format entities in db [Perm.REINDEX_ELIGIBILITY]")
    @PreAuthorize("hasAuthority('" + Permissions.REINDEX_ELIGIBILITY + "')")
    @RequestMapping(value = "/reindex/format", method = RequestMethod.POST)
    public void reindexFormat() {
        solrReindexService.dropReindexFormat();
    }

    @ApiOperation(value = "deletes all old Format definition index records and creates index for all Format definition entities in db [Perm.REINDEX_ELIGIBILITY]")
    @PreAuthorize("hasAuthority('" + Permissions.REINDEX_ELIGIBILITY + "')")
    @RequestMapping(value = "/reindex/format_definition", method = RequestMethod.POST)
    public void reindexFormatDefinition() {
        solrReindexService.dropReindexFormatDefinition();
    }
}
