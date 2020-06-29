package cz.cas.lib.arclib.service.formatIdentification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.arclib.bpm.FormatIdentificationDelegate;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestIssue;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.preservationPlanning.IngestIssueDefinition;
import cz.cas.lib.arclib.domain.preservationPlanning.IngestIssueDefinitionCode;
import cz.cas.lib.arclib.domain.preservationPlanning.Tool;
import cz.cas.lib.arclib.exception.bpm.ConfigParserException;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import cz.cas.lib.arclib.formatlibrary.service.FormatDefinitionService;
import cz.cas.lib.arclib.formatlibrary.store.DbFormatDefinitionStore;
import cz.cas.lib.arclib.mail.ArclibMailCenter;
import cz.cas.lib.arclib.service.IngestIssueService;
import cz.cas.lib.arclib.service.IngestWorkflowService;
import cz.cas.lib.arclib.service.formatIdentification.droid.DroidFormatIdentificationTool;
import cz.cas.lib.arclib.service.preservationPlanning.ToolService;
import cz.cas.lib.arclib.store.IngestIssueDefinitionStore;
import cz.cas.lib.arclib.store.IngestIssueStore;
import cz.cas.lib.arclib.store.IngestWorkflowStore;
import cz.cas.lib.arclib.store.ToolStore;
import cz.cas.lib.core.sequence.Generator;
import cz.cas.lib.core.sequence.Sequence;
import cz.cas.lib.core.sequence.SequenceStore;
import helper.SrDbTest;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.*;

import static helper.ThrowableAssertion.assertThrown;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class FormatIdentificationToolTest extends SrDbTest {
    private static final String INGEST_CONFIG = "{\"formatIdentification\":{\"0\":{\"type\":\"DROID\",\"pathsAndFormats\":{\"0\":{\"filePath\":\"\", \"format\":\"fmt/101\"}, \"1\":{\"filePath\":\".*/another/filepath\", \"format\":\"format1\"}}}}}";

    private IngestWorkflow ingestWorkflow;
    private IngestWorkflowStore ingestWorkflowStore;
    private IngestIssueService ingestIssueService = new IngestIssueService();
    private DbFormatDefinitionStore formatDefinitionStore;
    private FormatDefinitionService formatDefinitionService = new FormatDefinitionService();
    private ToolService toolService = new ToolService();
    private IngestIssueDefinitionStore ingestIssueDefinitionStore;
    private ToolStore toolStore;
    @Mock
    private ArclibMailCenter arclibMailCenter;

    private ObjectMapper objectMapper;
    private JsonNode INGEST_CONFIG_JSON_NODE;

    private FormatIdentificationTool formatIdentificationTool;
    private FormatIdentificationDelegate formatIdentificationDelegate;
    private IngestIssueStore ingestIssueStore;

    private Generator generator;
    private SequenceStore sequenceStore;

    @Before
    public void before() throws IOException {
        MockitoAnnotations.initMocks(this);
        ingestWorkflowStore = new IngestWorkflowStore();
        ingestIssueStore = new IngestIssueStore();
        formatDefinitionStore = new DbFormatDefinitionStore();
        sequenceStore = new SequenceStore();
        ingestIssueDefinitionStore = new IngestIssueDefinitionStore();
        toolStore = new ToolStore();

        initializeStores(ingestIssueStore, toolStore, ingestWorkflowStore, sequenceStore, formatDefinitionStore, ingestIssueDefinitionStore);

        formatDefinitionService.setStore(formatDefinitionStore);
        ingestIssueService.setIngestIssueStore(ingestIssueStore);
        toolService.setToolStore(toolStore);
        toolService.setArclibMailCenter(arclibMailCenter);

        generator = new Generator();
        generator.setStore(sequenceStore);
        ingestWorkflowStore.setGenerator(generator);

        Sequence sequence = new Sequence();
        sequence.setCounter(2L);
        sequence.setFormat("'#'#");
        sequence.setId(ingestWorkflowStore.getSEQUENCE_ID());
        sequenceStore.save(sequence);

        Tool t = new Tool();
        t.setName(FormatIdentificationToolType.DROID.toString());
        t.setVersion("v1");
        toolStore.save(t);

        IngestIssueDefinition ingestIssueDefinition = new IngestIssueDefinition();
        ingestIssueDefinition.setNumber("500");
        ingestIssueDefinition.setCode(IngestIssueDefinitionCode.FILE_FORMAT_RESOLVED_BY_CONFIG);
        ingestIssueDefinition.setName("Ambiguous format resolved by config.");
        ingestIssueDefinitionStore.save(ingestIssueDefinition);

        IngestIssueDefinition def2 = new IngestIssueDefinition();
        def2.setNumber("501");
        def2.setCode(IngestIssueDefinitionCode.CONFIG_PARSE_ERROR);
        def2.setName("Invalid config.");
        ingestIssueDefinitionStore.save(def2);

        IngestIssueDefinition def3 = new IngestIssueDefinition();
        def3.setNumber("502");
        def3.setCode(IngestIssueDefinitionCode.FILE_FORMAT_UNRESOLVABLE);
        def3.setName("Invalid config.");
        ingestIssueDefinitionStore.save(def3);


        ingestWorkflow = new IngestWorkflow();
        ingestWorkflow.setExternalId("externalid");
        ingestWorkflowStore.save(ingestWorkflow);

        objectMapper = new ObjectMapper();
        INGEST_CONFIG_JSON_NODE = objectMapper.readTree(INGEST_CONFIG);

        formatIdentificationTool = new DroidFormatIdentificationTool();
        formatIdentificationTool.inject(formatDefinitionService, ingestIssueService, ingestIssueDefinitionStore, ingestWorkflow, t, 0);

        formatIdentificationDelegate = new FormatIdentificationDelegate();
        formatIdentificationDelegate.setToolService(toolService);
        IngestWorkflowService ingestWorkflowService = new IngestWorkflowService();
        ingestWorkflowService.setStore(ingestWorkflowStore);
        formatIdentificationDelegate.setIngestWorkflowService(ingestWorkflowService);
        formatIdentificationDelegate.setFormatDefinitionService(formatDefinitionService);
        formatIdentificationDelegate.setIngestIssueDefinitionStore(ingestIssueDefinitionStore);
        formatIdentificationDelegate.setIngestIssueService(ingestIssueService);

        Tool t2 = new Tool();
        t2.setName(formatIdentificationDelegate.getToolName());
        toolStore.save(t2);
    }

    @Test
    public void testDroidConfigParser() throws IOException, ConfigParserException {
        String droidConfig = "{\"formatIdentification\":{\"0\":{\"type\":\"DROID\"}}}";
        JsonNode jsonRoot = new ObjectMapper().readTree(droidConfig);
        FormatIdentificationTool a = formatIdentificationDelegate.initialize(jsonRoot, ingestWorkflow, 0);
        assertThat(a, instanceOf(DroidFormatIdentificationTool.class));
    }

    @Test
    public void testDroidConfigParserException() throws IOException {
        String droidConfig = "{\"formatIdentification\":{\"0\":{\"type\":\"blah\"}}}";
        JsonNode jsonRoot = new ObjectMapper().readTree(droidConfig);
        assertThrown(() -> formatIdentificationDelegate.initialize(jsonRoot, ingestWorkflow, 0)).isInstanceOf(ConfigParserException.class);
    }

    @Test
    public void testPathsToConfigParser() throws IOException, IncidentException {
        String droidConfig = "{\"formatIdentification\":{\"0\":{\"pathsAndFormats\":{\"0\":{\"filePath\":\"this/is/a/filepath\", \"format\":\"fmt/101\"}, \"1\":{\"filePath\":\"this/is/another/filepath\", \"format\":\"fmt/993\"}}}}}";
        JsonNode jsonRoot = new ObjectMapper().readTree(droidConfig);
        List<Pair<String, String>> pathToFormatDtos = formatIdentificationTool.parsePathsAndFormats(jsonRoot, ingestWorkflow.getExternalId());
        assertThat(pathToFormatDtos, hasSize(2));

        assertThat(asList(pathToFormatDtos.get(0).getLeft(), pathToFormatDtos.get(1).getLeft()),
                containsInAnyOrder("this/is/a/filepath", "this/is/another/filepath"));
        assertThat(asList(pathToFormatDtos.get(0).getRight(), pathToFormatDtos.get(1).getRight()),
                containsInAnyOrder("fmt/101", "fmt/993"));
    }

    @Test
    public void testPathsToConfigParserExceptionWrongType() throws IOException {
        String droidConfig = "{\"formatIdentification\":{\"0\":{\"type\":\"blah\"}}}";
        JsonNode jsonRoot = new ObjectMapper().readTree(droidConfig);
        assertThrown(() -> formatIdentificationDelegate.initialize(jsonRoot, ingestWorkflow, 0)).isInstanceOf(ConfigParserException.class);
    }

    @Test
    public void testPathsToConfigParserExceptionMissingType() throws Exception {
        String droidConfig = "{\"formatIdentification\":{\"0\":{}}}";
        JsonNode jsonRoot = new ObjectMapper().readTree(droidConfig);
        assertThrown(() -> formatIdentificationDelegate.initialize(jsonRoot, ingestWorkflow, 0)).isInstanceOf(ConfigParserException.class);
    }

    @Test
    public void testPathsToConfigParserExceptionWrongPathsToFormats() throws IOException, IncidentException {
        //no exception thrown when an empty array is provided
        String droidConfig = "{\"formatIdentification\":{\"0\":{\"type\":\"DROID\",\"pathsAndFormats\":{}}}}";
        JsonNode jsonRoot = new ObjectMapper().readTree(droidConfig);
        formatIdentificationTool.parsePathsAndFormats(jsonRoot, ingestWorkflow.getExternalId());

        //exception thrown when an invalid element is provided in the array
        String droidConfig2 = "{\"formatIdentification\":{\"0\":{\"type\":\"DROID\",\"pathsAndFormats\":{\"0\":{}}}}}";
        JsonNode jsonRoot2 = new ObjectMapper().readTree(droidConfig2);
        assertThrown(() -> formatIdentificationTool.parsePathsAndFormats(jsonRoot2, ingestWorkflow.getExternalId()))
                .isInstanceOf(IncidentException.class);

        Collection<IngestIssue> issues = ingestIssueStore.findAll();
        assertThat(issues.size(), is(1));

        IngestIssue issue = (IngestIssue) issues.toArray()[0];
        assertThat(issue.isSuccess(), is(false));
        assertThat(issue.getDescription(), startsWith("invalid config: {\"0\":{}} at: /formatIdentification/0/pathsAndFormats " +
                "supported values: [List of pairs of paths to files and their respective file formats, e.g." +
                " {\"0\":{\"filePath\""));

        assertThat(issue.getIngestIssueDefinition().getName(), is("Invalid config."));
    }

    @Test
    public void testResolveAmbiguousFormatIdentificationsSuccess() throws IncidentException {
        final String testFilePath = "this/is/another/filepath";
        final String regex = ".*/another/filepath";
        List<Pair<String, String>> formatList = new ArrayList<>();
        formatList.add(Pair.of("format1", "some method"));
        formatList.add(Pair.of("format2", "another method"));

        Map<String, List<Pair<String, String>>> identifiedFormats = new HashMap<>();
        identifiedFormats.put(testFilePath, formatList);

        List<Pair<String, String>> predefinedFormats = new ArrayList<>();
        predefinedFormats.add(Pair.of(regex, "format1"));

        Map<String, Pair<String, String>> result =
                formatIdentificationTool.resolveAmbiguousIdentifications(identifiedFormats, INGEST_CONFIG_JSON_NODE, ingestWorkflow.getExternalId());

        assertThat(result.keySet(), hasSize(1));
        assertThat(result.keySet(), contains(testFilePath));

        Pair<String, String> formatToIdentificationMethod = result.get(testFilePath);
        assertThat(formatToIdentificationMethod.getLeft(), is("format1"));
        assertThat(formatToIdentificationMethod.getRight(), is("Manually determined format as defined for the" +
                " files located at the path matching regex: " + regex));

        Collection<IngestIssue> issues = ingestIssueStore.findAll();
        assertThat(issues.size(), is(1));

        IngestIssue issue = (IngestIssue) issues.toArray()[0];
        assertThat(issue.isSuccess(), is(true));
        assertThat(issue.getIngestIssueDefinition().getName(), is("Ambiguous format resolved by config."));
        assertThat(issue.getDescription(), is("used config: {\"filePath\":\".*/another/filepath\", \"format\":\"format1\"} at: /formatIdentification/0/pathsAndFormats"));
    }

    @Test
    public void testResolveAmbiguousFormatIdentificationsFailure() {
        final String testFilePath = "this/is/a/file/path";

        List<Pair<String, String>> formatList = new ArrayList<>();
        formatList.add(Pair.of("format1", "some method"));
        formatList.add(Pair.of("format2", "another method"));

        Map<String, List<Pair<String, String>>> identifiedFormats = new HashMap<>();
        identifiedFormats.put(testFilePath, formatList);

        assertThrown(() -> formatIdentificationTool.resolveAmbiguousIdentifications(identifiedFormats,
                INGEST_CONFIG_JSON_NODE, ingestWorkflow.getExternalId())).isInstanceOf(IncidentException.class);

        Collection<IngestIssue> issues = ingestIssueStore.findAll();
        assertThat(issues.size(), is(2));

        IngestIssue issue = (IngestIssue) issues.toArray()[0];
        assertThat(issue.isSuccess(), is(false));
        assertThat(issue.getDescription(), is("File at path: this/is/a/file/path was identified with multiple formats: {\n" +
                "format: format1, identification method: some method\n" +
                "format: format2, identification method: another method\n" +
                "}\n"));
    }
}
