package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.index.solr.ReindexService;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "administration", description = "Api for administration purposes")
@RequestMapping("/api/administration")
@Slf4j
public class AdministrationApi {

    @Autowired
    private ReindexService solrReindexService;

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

    @Operation(summary = "deletes all old Format definition index records and creates index for all Format definition entities in db [Perm.REINDEX_ELIGIBILITY]")
    @PreAuthorize("hasAuthority('" + Permissions.REINDEX_ELIGIBILITY + "')")
    @RequestMapping(value = "/reindex/format_definition", method = RequestMethod.POST)
    public void reindexFormatDefinition() {
        solrReindexService.dropReindexFormatDefinition();
    }
}
