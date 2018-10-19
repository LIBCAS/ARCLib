package cz.cas.lib.arclib.bpm.delegate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.arclib.bpm.AntivirusDelegate;
import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.packages.Sip;
import cz.cas.lib.arclib.exception.bpm.ConfigParserException;
import cz.cas.lib.arclib.service.antivirus.Antivirus;
import cz.cas.lib.arclib.service.antivirus.ClamAntivirus;
import cz.cas.lib.arclib.store.IngestIssueStore;
import cz.cas.lib.arclib.store.IngestWorkflowStore;
import cz.cas.lib.arclib.store.SipStore;
import cz.cas.lib.core.sequence.Generator;
import cz.cas.lib.core.sequence.Sequence;
import cz.cas.lib.core.sequence.SequenceStore;
import org.apache.commons.io.FileUtils;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static cz.cas.lib.core.util.Utils.fileExists;
import static helper.ThrowableAssertion.assertThrown;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Ensure the antivirus.bpmn process is working correctly
 */
@Deployment(resources = "bpmn/antivirus.bpmn")
public class AntivirusDelegateTest extends DelegateTest {

    private static final String INGEST_CONFIG = "{\"antivirus\":{\"type\":\"clamav\",\"cmd\":[\"clamscan\",\"-r\"],\"infectedSipAction\":\"QUARANTINE\"}}";
    private static final String PROCESS_INSTANCE_KEY = "antivirusProcess";
    private static final String CORRUPTED_FILE_NAME = "eicar.com";
    private static final Path QUARANTINE_PATH = WS.resolve("quarantine");
    private static final Path CORRUPTED_FILE_REPRESENTANT = Paths.get("src/test/resources").resolve(CORRUPTED_FILE_NAME);
    private static final String[] CMD = {"clamscan", "-r"};

    private AntivirusDelegate antivirusDelegate;
    private Generator generator;

    @Before
    public void before() {
        antivirusDelegate = new AntivirusDelegate();

        IngestWorkflowStore ingestWorkflowStore = new IngestWorkflowStore();

        SequenceStore sequenceStore;
        sequenceStore = new SequenceStore();

        SipStore sipStore = new SipStore();

        IngestIssueStore ingestIssueStore = new IngestIssueStore();
        ingestIssueStore.setTemplate(getTemplate());
        initializeStores(ingestIssueStore, ingestWorkflowStore, sequenceStore, sipStore);

        generator = new Generator();
        generator.setStore(sequenceStore);

        Sequence sequence = new Sequence();
        sequence.setCounter(2L);
        sequence.setFormat("'#'#");
        sequence.setId(ingestWorkflowStore.getSEQUENCE_ID());
        sequenceStore.save(sequence);

        ingestWorkflowStore.setGenerator(generator);

        antivirusDelegate.setObjectMapper(new ObjectMapper());
        antivirusDelegate.setWorkspace(WS.toString());
        antivirusDelegate.setIngestIssueStore(ingestIssueStore);
        antivirusDelegate.setIngestWorkflowStore(ingestWorkflowStore);
        antivirusDelegate.setQuarantinePath(QUARANTINE_PATH.toString());

        Sip sip = new Sip();
        sipStore.save(sip);

        IngestWorkflow ingestWorkflow = new IngestWorkflow(INGEST_WORKFLOW_ID);
        ingestWorkflow.setExternalId(EXTERNAL_ID);
        ingestWorkflow.setSip(sip);
        ingestWorkflowStore.save(ingestWorkflow);

        Mocks.register("antivirusDelegate", antivirusDelegate);
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
        variables.put(BpmConstants.ProcessVariables.assignee, "user");
        startJob(PROCESS_INSTANCE_KEY, variables);
        assertThat(fileExists(QUARANTINE_PATH.resolve(EXTERNAL_ID)), is(true));
    }

    @Test
    public void testClamConfigParser() throws IOException, ConfigParserException {
        String clamConfig = "{\"antivirus\":{\"type\":\"clamav\",\"cmd\":[\"clamscan\",\"-r\"]}}";
        JsonNode jsonRoot = new ObjectMapper().readTree(clamConfig);
        Antivirus a = antivirusDelegate.initialize(jsonRoot);
        assertThat(a, instanceOf(ClamAntivirus.class));
        ClamAntivirus ca = (ClamAntivirus) a;
        assertThat(ca.getCmd(), equalTo(CMD));
    }

    @Test
    public void testClamConfigParserException() throws IOException {
        String clamConfig = "{\"antivirus\":{\"type\":\"blah\",\"cmd\":[\"clamscan\",\"-r\"]}}";
        JsonNode jsonRoot = new ObjectMapper().readTree(clamConfig);
        assertThrown(() -> antivirusDelegate.initialize(jsonRoot)).isInstanceOf(ConfigParserException.class);
    }
}
