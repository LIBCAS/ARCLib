package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.index.solr.ReindexService;
import cz.cas.lib.arclib.init.PostInitializer;
import cz.cas.lib.arclib.service.arclibxml.SampleArclibXmlsGenerator;
import cz.cas.lib.core.store.Transactional;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.io.IOException;
import java.sql.SQLException;

@RestController
@Api(value = "administration", description = "Api for administration purposes")
@RequestMapping("/api/administration")
@Slf4j
@Transactional
public class AdministrationApi {

    @Inject
    private ReindexService solrReindexService;
    @Inject
    private PostInitializer postInitializer;
    @Inject
    private SampleArclibXmlsGenerator sampleArclibXmlsGenerator;

    @ApiOperation(value = "creates index for all entities in db", notes = "arclibxml and format entities are omitted as there are too many records")
    @RequestMapping(value = "/reindex", method = RequestMethod.POST)
    public void reindexAll() {
        solrReindexService.reindexAll();
    }

    @ApiOperation(value = "deletes all old index records and creates index for all entities in db", notes = "arclibxml and format entities are omitted as there are too many records")
    @RequestMapping(value = "/dropReindex", method = RequestMethod.POST)
    public void dropReindexAll() {
        solrReindexService.dropReindexAll();
    }

    @ApiOperation(value = "creates index for all arclibxmls")
    @RequestMapping(value = "/reindex/arclib_xml", method = RequestMethod.POST)
    public void reindexArclibXml() {
        solrReindexService.reindexArclibXml();
    }

    @ApiOperation(value = "creates index for all Format entities in db")
    @RequestMapping(value = "/reindex/format", method = RequestMethod.POST)
    public void reindexFormat() {
        solrReindexService.reindexFormat();
    }

    @ApiOperation(value = "deletes all old Format index records and creates index for all Format entities in db")
    @RequestMapping(value = "/dropReindex/format", method = RequestMethod.POST)
    public void dropReindexFormat() {
        solrReindexService.dropReindexFormat();
    }

    @ApiOperation(value = "creates index for all Format definition entities in db")
    @RequestMapping(value = "/reindex/format_definition", method = RequestMethod.POST)
    public void reindexFormatDefinition() {
        solrReindexService.reindexFormatDefinition();
    }

    @ApiOperation(value = "deletes all old Format definition index records and creates index for all Format definition entities in db")
    @RequestMapping(value = "/dropReindex/format_definition", method = RequestMethod.POST)
    public void dropReindexFormatDefinition() {
        solrReindexService.dropReindexFormatDefinition();
    }

    @ApiOperation(value = "WARNING: deletes current data and fills test data", notes = "cleans workspace, bpm engine, database and index and fills db&index with testing data ...does not affect archival storage")
    @RequestMapping(value = "/initialize_with_test_data", method = RequestMethod.POST)
    public void initializeWithTestData() throws IOException, SQLException {
        postInitializer.initializeWithTestData();
    }

    @ApiOperation(value = "WARNING: generates new arclib xml sample data and rewrites DB initialization script",
            notes = "generates arclib xml sample data according to the provided arclib xml")
    @RequestMapping(value = "/generate_sample_arclib_xmls", method = RequestMethod.POST)
    public void generateSampleArclibXmls(@ApiParam(value = "Path to sample ArclibXml", required = true)
                                         @RequestParam("path_to_sample_arclib_xml") String pathToSampleArclibXml)
            throws IOException, DocumentException {
        sampleArclibXmlsGenerator.generateSampleXmls(pathToSampleArclibXml);
    }

    @ApiOperation(value = "WARNING: updates sample arclib xml data at archival storage")
    @RequestMapping(value = "/update_sample_archival_storage_data", method = RequestMethod.POST)
    public void updateSampleDataAtArchivalStorage(@ApiParam(value = "Path to sample data folder", required = true)
                                                  @RequestParam("path_to_sample_data") String pathToSampleData,
                                                  @ApiParam(value = "Path to archival storage data folder", required = true)
                                                  @RequestParam("path_to_archival_storage_data") String pathToArchivalStorageData) {
        SampleArclibXmlsGenerator.updateSampleDataAtArchivalStorage(pathToSampleData, pathToArchivalStorageData);
    }

    @ApiOperation(value = "WARNING: deletes all data at archival storage")
    @RequestMapping(value = "/clean_up_archival_storage_data", method = RequestMethod.POST)
    public void cleanUpDataAtArchivalStorage(@ApiParam(value = "Path to archival storage data folder", required = true)
                                             @RequestParam("path_to_archival_storage_data") String pathToArchivalStorageData) {
        SampleArclibXmlsGenerator.cleanUpDataAtArchivalStorage(pathToArchivalStorageData);
    }
}
