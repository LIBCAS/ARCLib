package cz.cas.lib.arclib.service.formatIdentification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestIssue;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.exception.bpm.ConfigParserException;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import cz.cas.lib.arclib.service.formatIdentification.droid.CsvResultColumn;
import cz.cas.lib.arclib.service.formatIdentification.droid.DroidFormatIdentificationTool;
import cz.cas.lib.arclib.store.IngestIssueStore;
import cz.cas.lib.arclib.store.IngestWorkflowStore;
import cz.cas.lib.core.sequence.Generator;
import cz.cas.lib.core.sequence.Sequence;
import cz.cas.lib.core.sequence.SequenceStore;
import cz.cas.lib.core.util.Utils;
import helper.SrDbTest;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static cz.cas.lib.arclib.service.formatIdentification.FormatIdentificationTool.initialize;
import static helper.ThrowableAssertion.assertThrown;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class FormatIdentificationToolTest extends SrDbTest {
    private static final String INGEST_CONFIG = "{\"formatIdentification\":{\"type\":\"DROID\",\"parsedColumn\": \"PUID\",\"pathsAndFormats\":[ {\"filePath\":\"file://\", \"format\":\"fmt/101\"}, {\"filePath\":\".*/another/filepath\", \"format\":\"format1\"}]}}";

    private IngestWorkflow ingestWorkflow;
    private IngestWorkflowStore ingestWorkflowStore;

    private ObjectMapper objectMapper;
    private JsonNode INGEST_CONFIG_JSON_NODE;

    private FormatIdentificationTool formatIdentificationTool;
    private IngestIssueStore ingestIssueStore;

    private Generator generator;
    private SequenceStore sequenceStore;

    @Before
    public void before() throws IOException {
        ingestIssueStore = new IngestIssueStore();
        ingestWorkflowStore = new IngestWorkflowStore();

        sequenceStore = new SequenceStore();

        initializeStores(ingestIssueStore, ingestWorkflowStore, sequenceStore);
        ingestIssueStore.setTemplate(getTemplate());

        generator = new Generator();
        generator.setStore(sequenceStore);
        ingestWorkflowStore.setGenerator(generator);

        Sequence sequence = new Sequence();
        sequence.setCounter(2L);
        sequence.setFormat("'#'#");
        sequence.setId(ingestWorkflowStore.getSEQUENCE_ID());
        sequenceStore.save(sequence);

        formatIdentificationTool = new DroidFormatIdentificationTool(CsvResultColumn.PUID);
        formatIdentificationTool.setIngestIssueStore(ingestIssueStore);
        formatIdentificationTool.setIngestWorkflowStore(ingestWorkflowStore);

        ingestWorkflow = new IngestWorkflow();
        ingestWorkflowStore.save(ingestWorkflow);

        objectMapper = new ObjectMapper();
        INGEST_CONFIG_JSON_NODE = objectMapper.readTree(INGEST_CONFIG);
    }

    @Test
    public void testDroidConfigParser() throws IOException, ConfigParserException {
        String droidConfig = "{\"formatIdentification\":{\"type\":\"DROID\",\"parsedColumn\":\"PUID\"}}";
        JsonNode jsonRoot = new ObjectMapper().readTree(droidConfig);
        FormatIdentificationTool a = initialize(jsonRoot);
        assertThat(a, instanceOf(DroidFormatIdentificationTool.class));
        DroidFormatIdentificationTool ca = (DroidFormatIdentificationTool) a;
        assertThat(ca.getParsedColumn(), equalTo(CsvResultColumn.PUID));
    }

    @Test
    public void testDroidConfigParserException() throws IOException {
        String droidConfig = "{\"formatIdentification\":{\"type\":\"blah\",\"parsedColumn\":\"PUID\"}}";
        JsonNode jsonRoot = new ObjectMapper().readTree(droidConfig);
        assertThrown(() -> initialize(jsonRoot)).isInstanceOf(ConfigParserException.class);
    }

    @Test
    public void testPathsToConfigParser() throws IOException, IncidentException {
        String droidConfig = "{\"formatIdentification\":{\"pathsAndFormats\":[ {\"filePath\":\"this/is/a/filepath\", \"format\":\"fmt/101\"}, {\"filePath\":\"this/is/another/filepath\", \"format\":\"fmt/993\"}]}}";
        JsonNode jsonRoot = new ObjectMapper().readTree(droidConfig);
        List<Utils.Pair<String, String>> pathToFormatDtos = formatIdentificationTool.parsePathsToFormats(jsonRoot, ingestWorkflow.getExternalId());
        assertThat(pathToFormatDtos, hasSize(2));

        assertThat(asList(pathToFormatDtos.get(0).getL(), pathToFormatDtos.get(1).getL()),
                containsInAnyOrder("this/is/a/filepath", "this/is/another/filepath"));
        assertThat(asList(pathToFormatDtos.get(0).getR(), pathToFormatDtos.get(1).getR()),
                containsInAnyOrder("fmt/101", "fmt/993"));
    }

    @Test
    public void testPathsToConfigParserExceptionWrongType() throws IOException {
        String droidConfig = "{\"formatIdentification\":{\"type\":\"blah\",\"parsedColumn\":\"PUID\"}}";
        JsonNode jsonRoot = new ObjectMapper().readTree(droidConfig);
        assertThrown(() -> initialize(jsonRoot)).isInstanceOf(ConfigParserException.class);
    }

    @Test
    public void testPathsToConfigParserExceptionMissingType() throws IOException {
        String droidConfig = "{\"formatIdentification\":{\"parsedColumn\":\"PUID\"}}";
        JsonNode jsonRoot = new ObjectMapper().readTree(droidConfig);
        assertThrown(() -> initialize(jsonRoot)).isInstanceOf(ConfigParserException.class);
    }

    @Test
    public void testPathsToConfigParserExceptionWrongParsedColumn() throws IOException {
        String droidConfig = "{\"formatIdentification\":{\"type\":\"blah\",\"parsedColumn\":\"blah\"}}";
        JsonNode jsonRoot = new ObjectMapper().readTree(droidConfig);
        assertThrown(() -> initialize(jsonRoot)).isInstanceOf(ConfigParserException.class);
    }

    @Test
    public void testPathsToConfigParserExceptionMissingParsedColumn() throws IOException {
        String droidConfig = "{\"formatIdentification\":{\"type\":\"blah\"}}";
        JsonNode jsonRoot = new ObjectMapper().readTree(droidConfig);
        assertThrown(() -> initialize(jsonRoot)).isInstanceOf(ConfigParserException.class);
    }

    @Test
    public void testPathsToConfigParserExceptionWrongPathsToFormats() throws IOException, IncidentException {
        //no exception thrown when an empty array is provided
        String droidConfig = "{\"formatIdentification\":{\"type\":\"DROID\",\"parsedColumn\": \"PUID\",\"pathsAndFormats\":[]}}";
        JsonNode jsonRoot = new ObjectMapper().readTree(droidConfig);
        formatIdentificationTool.parsePathsToFormats(jsonRoot, ingestWorkflow.getExternalId());

        //exception thrown when an invalid element is provided in the array
        String droidConfig2 = "{\"formatIdentification\":{\"type\":\"DROID\",\"parsedColumn\": \"PUID\",\"pathsAndFormats\":[{}]}}";
        JsonNode jsonRoot2 = new ObjectMapper().readTree(droidConfig2);
        assertThrown(() -> formatIdentificationTool.parsePathsToFormats(jsonRoot2, ingestWorkflow.getExternalId()))
                .isInstanceOf(IncidentException.class);

        Collection<IngestIssue> issues = ingestIssueStore.findAll();
        assertThat(issues.size(), is(1));

        IngestIssue issue = (IngestIssue) issues.toArray()[0];
        assertThat(issue.isSolvedByConfig(), is(false));
        assertThat(issue.getConfigNote(), is("invalid config: [{}] at: /formatIdentification/pathsAndFormats " +
                "supported values: [List of pairs of paths to files and their respective file formats, e.g." +
                " [ {\"filePath\":\"this/is/a/filepath\", \"format\":\"fmt/101\"}, {\"filePath\":" +
                "\"this/is/another/filepath\", \"format\":\"fmt/993\"} ]]"));

        assertThat(issue.getIssue(), is("Invalid config."));
    }

    @Test
    public void testResolveAmbiguousFormatIdentificationsSuccess() throws IncidentException {
        final String testFilePath = "this/is/another/filepath";
        final String regex = ".*/another/filepath";
        List<Utils.Pair<String, String>> formatList = new ArrayList<>();
        formatList.add(new Utils.Pair("format1", "some method"));
        formatList.add(new Utils.Pair("format2", "another method"));

        Map<String, List<Utils.Pair<String, String>>> identifiedFormats = new HashMap<>();
        identifiedFormats.put(testFilePath, formatList);

        List<Utils.Pair<String, String>> predefinedFormats = new ArrayList<>();
        predefinedFormats.add(new Utils.Pair(regex, "format1"));

        Map<String, Utils.Pair<String, String>> result =
                formatIdentificationTool.resolveAmbiguousIdentifications(identifiedFormats, INGEST_CONFIG_JSON_NODE, ingestWorkflow.getExternalId());

        assertThat(result.keySet(), hasSize(1));
        assertThat(result.keySet(), contains(testFilePath));

        Utils.Pair<String, String> formatToIdentificationMethod = result.get(testFilePath);
        assertThat(formatToIdentificationMethod.getL(), is("format1"));
        assertThat(formatToIdentificationMethod.getR(), is("Manually determined format as defined for the" +
                " files located at the path matching regex: " + regex));

        Collection<IngestIssue> issues = ingestIssueStore.findAll();
        assertThat(issues.size(), is(1));

        IngestIssue issue = (IngestIssue) issues.toArray()[0];
        assertThat(issue.isSolvedByConfig(), is(true));
        assertThat(issue.getIssue(), is("Ambiguous format resolved by config."));
        assertThat(issue.getConfigNote(), is("used config: {\"filePath\":\".*/another/filepath\", \"format\":\"format1\"} at: /formatIdentification/pathsAndFormats"));
    }

    @Test
    public void testResolveAmbiguousFormatIdentificationsFailure() {
        final String testFilePath = "this/is/a/file/path";

        List<Utils.Pair<String, String>> formatList = new ArrayList<>();
        formatList.add(new Utils.Pair("format1", "some method"));
        formatList.add(new Utils.Pair("format2", "another method"));

        Map<String, List<Utils.Pair<String, String>>> identifiedFormats = new HashMap<>();
        identifiedFormats.put(testFilePath, formatList);

        assertThrown(() -> formatIdentificationTool.resolveAmbiguousIdentifications(identifiedFormats,
                INGEST_CONFIG_JSON_NODE, ingestWorkflow.getExternalId())).isInstanceOf(IncidentException.class);

        Collection<IngestIssue> issues = ingestIssueStore.findAll();
        assertThat(issues.size(), is(1));

        IngestIssue issue = (IngestIssue) issues.toArray()[0];
        assertThat(issue.isSolvedByConfig(), is(false));
        assertThat(issue.getIssue(), is("File at path: this/is/a/file/path was identified with multiple formats: {\n" +
                "format: format1, identification method: some method\n" +
                "format: format2, identification method: another method\n" +
                "}"));
    }
}
