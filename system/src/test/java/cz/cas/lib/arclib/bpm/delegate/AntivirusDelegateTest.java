package cz.cas.lib.arclib.bpm.delegate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.arclib.bpm.AntivirusDelegate;
import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.domain.IngestToolFunction;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.packages.Sip;
import cz.cas.lib.arclib.domain.preservationPlanning.IngestIssueDefinition;
import cz.cas.lib.arclib.domain.preservationPlanning.IngestIssueDefinitionCode;
import cz.cas.lib.arclib.domain.preservationPlanning.Tool;
import cz.cas.lib.arclib.exception.bpm.ConfigParserException;
import cz.cas.lib.arclib.mail.ArclibMailCenter;
import cz.cas.lib.arclib.service.IngestIssueService;
import cz.cas.lib.arclib.service.IngestWorkflowService;
import cz.cas.lib.arclib.service.antivirus.Antivirus;
import cz.cas.lib.arclib.service.antivirus.ClamAntivirus;
import cz.cas.lib.arclib.service.preservationPlanning.ToolService;
import cz.cas.lib.arclib.store.*;
import cz.cas.lib.core.sequence.Generator;
import cz.cas.lib.core.sequence.Sequence;
import cz.cas.lib.core.sequence.SequenceStore;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static cz.cas.lib.core.util.Utils.fileExists;
import static helper.ThrowableAssertion.assertThrown;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ensure the antivirus.bpmn process is working correctly
 */
@Deployment(resources = "bpmn/antivirus.bpmn")
public class AntivirusDelegateTest extends DelegateTest {

    private static final String INGEST_CONFIG = "{\"antivirus\":{\"0\":{\"type\":\"clamav\",\"cmd\":{\"0\":\"clamscan\",\"1\":\"-r\"},\"infectedSipAction\":\"QUARANTINE\"}}}";
    private static final String PROCESS_INSTANCE_KEY = "antivirusProcess";
    private static final String CORRUPTED_FILE_NAME = "eicar.com";
    private static final Path QUARANTINE_PATH = WS.resolve("quarantine");
    private static final Path CORRUPTED_FILE_REPRESENTANT = Paths.get("src/test/resources").resolve(CORRUPTED_FILE_NAME);
    private static final String[] CMD = {"clamscan", "-r"};
    private static final String eventId = "eventId";

    private AntivirusDelegate antivirusDelegate = new AntivirusDelegate();
    private Generator generator = new Generator();
    @Mock
    protected ArclibMailCenter mailCenter;
    @Mock
    protected DelegateExecution delegateExecution;

    private IngestIssueDefinitionStore ingestIssueDefinitionStore = new IngestIssueDefinitionStore();
    private IngestWorkflowStore ingestWorkflowStore = new IngestWorkflowStore();
    private SequenceStore sequenceStore = new SequenceStore();
    private SipStore sipStore = new SipStore();
    private ToolStore toolStore = new ToolStore();
    private ToolService toolService = new ToolService();
    private IngestIssueStore ingestIssueStore = new IngestIssueStore();
    private IngestIssueService ingestIssueService = new IngestIssueService();
    private IngestWorkflow ingestWorkflow = new IngestWorkflow(INGEST_WORKFLOW_ID);

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        initializeStores(ingestIssueStore, toolStore, ingestWorkflowStore, sequenceStore, sipStore, ingestIssueDefinitionStore);
        toolService.setToolStore(toolStore);
        toolService.setArclibMailCenter(mailCenter);
        ingestIssueService.setIngestIssueStore(ingestIssueStore);
        generator.setStore(sequenceStore);
        ingestWorkflowStore.setGenerator(generator);
        ingestIssueDefinitionStore.setGenerator(generator);

        antivirusDelegate.setObjectMapper(new ObjectMapper());
        antivirusDelegate.setWorkspace(WS.toString());
        antivirusDelegate.setIngestIssueService(ingestIssueService);
        IngestWorkflowService ingestWorkflowService = new IngestWorkflowService();
        ingestWorkflowService.setStore(ingestWorkflowStore);
        antivirusDelegate.setIngestWorkflowService(ingestWorkflowService);
        antivirusDelegate.setQuarantinePath(QUARANTINE_PATH.toString());
        antivirusDelegate.setToolService(toolService);
        antivirusDelegate.setIngestIssueDefinitionStore(ingestIssueDefinitionStore);
        Mocks.register("antivirusDelegate", antivirusDelegate);

        Sequence sequence = new Sequence();
        sequence.setCounter(2L);
        sequence.setFormat("'#'#");
        sequence.setId(ingestWorkflowStore.getSEQUENCE_ID());
        sequenceStore.save(sequence);

        Sequence seq2 = new Sequence();
        seq2.setCounter(3L);
        seq2.setFormat("'#'#");
        seq2.setId(ingestIssueDefinitionStore.getSEQUENCE_ID());
        sequenceStore.save(seq2);

        Sip sip = new Sip();
        sipStore.save(sip);

        Tool t = new Tool();
        t.setName("ARCLib_" + IngestToolFunction.virus_check);
        toolService.save(t);

        ingestWorkflow.setExternalId(EXTERNAL_ID);
        ingestWorkflow.setSip(sip);
        ingestWorkflowStore.save(ingestWorkflow);

        IngestIssueDefinition def = new IngestIssueDefinition();
        def.setCode(IngestIssueDefinitionCode.CONFIG_PARSE_ERROR);
        ingestIssueDefinitionStore.save(def);

        IngestIssueDefinition de2 = new IngestIssueDefinition();
        de2.setCode(IngestIssueDefinitionCode.FILE_VIRUS_FOUND);
        ingestIssueDefinitionStore.save(de2);

        Map mapOfEventIdsToMapsOfFilesToFormats = new HashMap(new TreeMap<String, Pair<String, String>>());
        mapOfEventIdsToMapsOfFilesToFormats.put(eventId, new TreeMap<>());
        when(delegateExecution.getVariable(BpmConstants.FormatIdentification.mapOfEventIdsToMapsOfFilesToFormats))
                .thenReturn(mapOfEventIdsToMapsOfFilesToFormats);

        when(delegateExecution.getVariable(BpmConstants.FormatIdentification.preferredFormatIdentificationEventId))
                .thenReturn(eventId);
    }

    /**
     * Tests that:
     * <ul>
     * <li>AV scan process recognizes infected files inside SIP folder and moves whole SIP to quarantine</li>
     * <li>BPM process starts scan based on given process variable (sip id)</li>
     * <li>BPM task set process variable with paths to infected files</li>
     * <li>BPM task move whole SIP to quarantine</li>
     * </ul>
     */
    @Test
    public void testAntivirusOnSIP() throws IOException {
        FileUtils.copyFile(CORRUPTED_FILE_REPRESENTANT.toFile(), WS_SIP_LOCATION.resolve(CORRUPTED_FILE_NAME).toFile());
        Map variables = new HashMap();
        variables.put(BpmConstants.ProcessVariables.ingestWorkflowId, INGEST_WORKFLOW_ID);
        variables.put(BpmConstants.ProcessVariables.latestConfig, INGEST_CONFIG);
        variables.put(BpmConstants.ProcessVariables.ingestWorkflowExternalId, EXTERNAL_ID);
        variables.put(BpmConstants.ProcessVariables.responsiblePerson, "user");
        HashMap mapOfEventIdsToMapsOfFilesToFormats = new HashMap();
        mapOfEventIdsToMapsOfFilesToFormats.put(eventId, new TreeMap<>());
        variables.put(BpmConstants.FormatIdentification.mapOfEventIdsToMapsOfFilesToFormats, mapOfEventIdsToMapsOfFilesToFormats);
        variables.put(BpmConstants.FormatIdentification.preferredFormatIdentificationEventId, eventId);
        variables.put(BpmConstants.Antivirus.antivirusToolCounter, 0);
        startJob(PROCESS_INSTANCE_KEY, variables);
        assertThat(fileExists(QUARANTINE_PATH.resolve(EXTERNAL_ID)), is(true));
        verify(mailCenter).sendNewToolVersionNotification(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    public void testClamConfigParser() throws IOException, ConfigParserException {
        JsonNode jsonRoot = new ObjectMapper().readTree(INGEST_CONFIG);
        Antivirus a = antivirusDelegate.initialize(jsonRoot, delegateExecution, 0);
        assertThat(a, instanceOf(ClamAntivirus.class));
        ClamAntivirus ca = (ClamAntivirus) a;
        assertThat(ca.getCmd(), equalTo(CMD));
    }

    @Test
    public void testClamConfigParserException() throws Exception {
        String clamConfig = "{\"antivirus\":{\"type\":\"blah\",\"cmd\":[\"clamscan\",\"-r\"],\"infectedSipAction\":\"QUARANTINE\"}}";
        JsonNode jsonRoot = new ObjectMapper().readTree(clamConfig);
        assertThrown(() -> antivirusDelegate.initialize(jsonRoot, delegateExecution, 0)).isInstanceOf(ConfigParserException.class);
    }
}
