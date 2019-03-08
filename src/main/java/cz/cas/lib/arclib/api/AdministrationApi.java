package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.index.solr.SolrReindexService;
import cz.cas.lib.arclib.init.PostInitializer;
import cz.cas.lib.core.store.Transactional;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.io.IOException;
import java.sql.SQLException;

@RestController
@Api(value = "aip", description = "Api for administration purposes")
@RequestMapping("/api/administration")
@Slf4j
@Transactional
public class AdministrationApi {

    @Inject
    private SolrReindexService solrReindexService;
    @Inject
    private PostInitializer postInitializer;

    @ApiOperation(value = "creates index for all entities in db", notes = "format entities are omitted as there are too many records")
    @RequestMapping(value = "/reindex", method = RequestMethod.POST)
    public void reindexAll() {
        solrReindexService.reindexAll();
    }

    @ApiOperation(value = "deletes all old index records and creates index for all entities in db", notes = "format entities are omitted as there are too many records")
    @RequestMapping(value = "/refresh", method = RequestMethod.POST)
    public void refreshAll() {
        solrReindexService.refreshAll();
    }

    @ApiOperation(value = "creates index for all Format entities in db")
    @RequestMapping(value = "/reindex/format", method = RequestMethod.POST)
    public void reindexFormat() {
        solrReindexService.reindexFormat();
    }

    @ApiOperation(value = "deletes all old Format index records and creates index for all Format entities in db")
    @RequestMapping(value = "/refresh/format", method = RequestMethod.POST)
    public void refreshFormat() {
        solrReindexService.refreshFormat();
    }

    @ApiOperation(value = "creates index for all Format definition entities in db")
    @RequestMapping(value = "/reindex/format_definition", method = RequestMethod.POST)
    public void reindexFormatDefinition() {
        solrReindexService.reindexFormatDefinition();
    }

    @ApiOperation(value = "deletes all old Format definition index records and creates index for all Format definition entities in db")
    @RequestMapping(value = "/refresh/format_definition", method = RequestMethod.POST)
    public void refreshFormatDefinition() {
        solrReindexService.refreshFormatDefinition();
    }

    @ApiOperation(value = "WARNING: deletes current data and fills test data", notes = "cleans workspace, bpm engine, database and index and fills db&index with testing data.. does not affect archival storage")
    @RequestMapping(value = "/initialize_with_test_data", method = RequestMethod.POST)
    public void initializeWithTestData() throws IOException, SQLException {
        postInitializer.initializeWithTestData();
    }
}
