package cz.cas.lib.arclib.service.antivirus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.arclib.domain.IngestToolFunction;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestIssue;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.packages.Sip;
import cz.cas.lib.arclib.domain.preservationPlanning.IngestIssueDefinition;
import cz.cas.lib.arclib.domain.preservationPlanning.IngestIssueDefinitionCode;
import cz.cas.lib.arclib.domain.preservationPlanning.Tool;
import cz.cas.lib.arclib.formatlibrary.service.FormatDefinitionService;
import cz.cas.lib.arclib.service.ExternalProcessRunner;
import cz.cas.lib.arclib.service.IngestIssueService;
import cz.cas.lib.arclib.store.*;
import cz.cas.lib.core.sequence.Generator;
import cz.cas.lib.core.sequence.Sequence;
import cz.cas.lib.core.sequence.SequenceStore;
import helper.SrDbTest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.TreeMap;

import static helper.ThrowableAssertion.assertThrown;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class AntivirusTests extends SrDbTest {

    private static Antivirus antivirus;
    private IngestIssueStore ingestIssueStore;
    private ObjectMapper mapper;
    private IngestWorkflow ingestWorkflow;
    private IngestWorkflowStore ingestWorkflowStore;
    private IndexedFormatDefinitionStore formatDefinitionStore;
    private IngestIssueDefinitionStore ingestIssueDefinitionStore;
    private SipStore sipStore;
    private Sip sip;
    private JsonNode configRoot;
    private Generator generator;
    private static final String[] CMD = {"clamscan", "-r"};
    private static final String INGEST_CONFIG = "{\"antivirus\":{\"type\":\"clamav\",\"cmd\":[\"clamscan\",\"-r\"],\"infectedSipAction\":\"IGNORE\"}}";
    private static final String CORRUPTED_FILE_NAME = "eicar.com";
    private static final Path CORRUPTED_FILE_REPRESENTANT = Paths.get("src/test/resources").resolve(CORRUPTED_FILE_NAME);
    private static final Path SIP_FOLDER = Paths.get("src/test/resources/testFolder");
    public static final String INGEST_WORKFLOW_ID = "uuid1";
    public static final String EXTERNAL_ID = "ARCLIB_000000001";
    public static final Path WS = Paths.get("testWorkspace");
    public static final String SIP_ID = "22e70aab-bd3a-4faf-9972-2db4d254e4d6";
    private static final Path QUARANTINE_PATH = WS.resolve("quarantine");
    private SequenceStore sequenceStore;

    @Before
    public void before() throws IOException {
        Files.createDirectories(WS);

        ingestWorkflowStore = new IngestWorkflowStore();
        sequenceStore = new SequenceStore();
        formatDefinitionStore = new IndexedFormatDefinitionStore();
        ingestIssueDefinitionStore = new IngestIssueDefinitionStore();
        ingestIssueStore = new IngestIssueStore();
        //ingestIssueStore.setTemplate(getTemplate());
        IngestIssueService ingestIssueService = new IngestIssueService();
        ingestIssueService.setIngestIssueStore(ingestIssueStore);
        FormatDefinitionService formatDefinitionService = new FormatDefinitionService();
        formatDefinitionService.setStore(formatDefinitionStore);
        ToolStore toolStore = new ToolStore();


        sipStore = new SipStore();

        initializeStores(ingestIssueStore, toolStore, ingestWorkflowStore, sequenceStore, sipStore, formatDefinitionStore, ingestIssueDefinitionStore);

        generator = new Generator();
        generator.setStore(sequenceStore);
        ingestWorkflowStore.setGenerator(generator);

        Sequence sequence = new Sequence();
        sequence.setCounter(2L);
        sequence.setFormat("'#'#");
        sequence.setId(ingestWorkflowStore.getSEQUENCE_ID());
        sequenceStore.save(sequence);

        sip = new Sip();
        sip.setId(SIP_ID);
        sipStore.save(sip);

        ingestWorkflow = new IngestWorkflow(INGEST_WORKFLOW_ID);
        ingestWorkflow.setExternalId(EXTERNAL_ID);
        ingestWorkflow.setSip(sip);
        ingestWorkflowStore.save(ingestWorkflow);

        TreeMap<String, Pair<String, String>> droidResult = new TreeMap<>();
        droidResult.put("/" + CORRUPTED_FILE_NAME, Pair.of("com", "mock"));

        Tool t = new Tool();
        t.setName(AntivirusType.CLAMAV.toString());
        t.setVersion("v1");
        t.setToolFunction(IngestToolFunction.virus_check);
        toolStore.save(t);

        IngestIssueDefinition def = new IngestIssueDefinition();
        def.setNumber("500");
        def.setCode(IngestIssueDefinitionCode.FILE_VIRUS_FOUND);
        def.setName("virus");
        ingestIssueDefinitionStore.save(def);

        ExternalProcessRunner r = new ExternalProcessRunner();
        r.setTimeoutSigkill(60);
        r.setTimeoutSigkill(60);
        antivirus = new ClamAntivirus(r, CMD);
        antivirus.inject(formatDefinitionService, ingestIssueService, t, ingestIssueDefinitionStore, null, InfectedSipAction.IGNORE, droidResult);

        mapper = new ObjectMapper();
        configRoot = mapper.readTree(INGEST_CONFIG);
    }

    @After
    public void after() throws IOException {
        FileUtils.cleanDirectory(WS.toFile());
        Files.deleteIfExists(WS);
        Files.deleteIfExists(SIP_FOLDER.resolve(CORRUPTED_FILE_NAME));
    }

    @Test
    public void nullFilePathTest() {
        assertThrown(() -> antivirus.scan(null, ingestWorkflow)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void fileNotFoundPathTest() {
        assertThrown(() -> antivirus.scan(Paths.get("invalidpath"), ingestWorkflow)).isInstanceOf(FileNotFoundException.class);
    }

    /**
     * Tests that AV scan process recognizes corrupted file inside folder.
     */
    @Test()
    public void corruptedFolderTest() throws Exception {
        Files.copy(CORRUPTED_FILE_REPRESENTANT, SIP_FOLDER.resolve(CORRUPTED_FILE_NAME), REPLACE_EXISTING);

        antivirus.scan(SIP_FOLDER.toAbsolutePath(), ingestWorkflow);

        List<IngestIssue> issues = ingestIssueStore.findByToolFunctionAndExternalId(IngestToolFunction.virus_check,
                EXTERNAL_ID);
        assertThat(issues.size(), is(1));
        IngestIssue ingestIssue = issues.get(0);
        assertThat(ingestIssue.isSuccess(), is(true));
        assertThat(ingestIssue.getDescription(), containsString(SIP_ID));
        assertThat(ingestIssue.getDescription(), containsString(CORRUPTED_FILE_NAME));
    }

    /**
     * Tests that AV scan process does not evaluate clean file as corrupted and therefore does not move it to quarantine folder. Called on single file.
     */
    @Test()
    public void okFileTest() throws Exception {
        antivirus.scan(SIP_FOLDER.resolve("clean.txt").toAbsolutePath(), ingestWorkflow);

        List<IngestIssue> issues = ingestIssueStore.findByToolFunctionAndExternalId(IngestToolFunction.virus_check,
                EXTERNAL_ID);
        assertThat(issues.size(), is(0));
    }
}
