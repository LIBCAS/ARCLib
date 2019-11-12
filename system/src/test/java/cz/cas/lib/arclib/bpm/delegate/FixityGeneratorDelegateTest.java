package cz.cas.lib.arclib.bpm.delegate;

import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.bpm.FixityGeneratorDelegate;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestEvent;
import cz.cas.lib.arclib.service.fixity.Crc32Counter;
import cz.cas.lib.arclib.service.fixity.Md5Counter;
import cz.cas.lib.arclib.service.fixity.Sha512Counter;
import cz.cas.lib.arclib.service.preservationPlanning.ToolService;
import cz.cas.lib.arclib.store.IngestEventStore;
import cz.cas.lib.arclib.store.IngestWorkflowStore;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

/**
 * Ensure the fixityGenerator.bpmn process is working correctly
 */
@Deployment(resources = "bpmn/fixityGenerator.bpmn")
public class FixityGeneratorDelegateTest extends DelegateTest {

    private static final String PROCESS_INSTANCE_KEY = "fixityGeneratorProcess";
    private static final String CRC32 = "b7e55887";
    private static final String MD5 = "36672c6a4d5e5aec788d9db462dd23c6";
    private static final String SHA512 = "bcde218aa914ba993c4a76b9ffa528098fbbeba9113e59c935ecf6db13283058e4a1cecc023b5e27f0780df292372a550f5647b7a22b6fc25fb15adf2aa341a4";
    @Mock
    private IngestEventStore ingestEventStore;
    @Mock
    private IngestWorkflowStore ingestWorkflowStore;
    @Mock
    private ToolService toolService;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        FixityGeneratorDelegate fixityGeneratorDelegate = new FixityGeneratorDelegate();
        fixityGeneratorDelegate.setCrc32Counter(new Crc32Counter());
        fixityGeneratorDelegate.setMd5Counter(new Md5Counter());
        fixityGeneratorDelegate.setSha512Counter(new Sha512Counter());
        fixityGeneratorDelegate.setWorkspace(WS.toString());
        fixityGeneratorDelegate.setIngestEventStore(ingestEventStore);
        fixityGeneratorDelegate.setIngestWorkflowStore(ingestWorkflowStore);
        fixityGeneratorDelegate.setToolService(toolService);
        Mocks.register("fixityGeneratorDelegate", fixityGeneratorDelegate);
    }

    /**
     * Runs fixity generator bpm process and ensures that correct checksums are computed and stored into process variables
     */
    @Test
    public void testFixityGeneratorOnSIP() {
        Map variables = new HashMap();
        variables.put(BpmConstants.ProcessVariables.ingestWorkflowId, INGEST_WORKFLOW_ID);
        variables.put(BpmConstants.ProcessVariables.ingestWorkflowExternalId, EXTERNAL_ID);
        variables.put(BpmConstants.Ingestion.sipFileName, SIP_ZIP.getFileName().toString());
        variables.put(BpmConstants.MessageDigestCalculation.mapOfEventIdsToMd5Calculations, new HashMap<>());
        variables.put(BpmConstants.MessageDigestCalculation.mapOfEventIdsToCrc32Calculations, new HashMap<>());
        variables.put(BpmConstants.MessageDigestCalculation.mapOfEventIdsToSha512Calculations, new HashMap<>());
        startJob(PROCESS_INSTANCE_KEY, variables);

        Map<String, String> md5Calculations = (Map<String, String>) rule.getHistoryService().createHistoricVariableInstanceQuery().variableName(BpmConstants.MessageDigestCalculation.mapOfEventIdsToMd5Calculations).singleResult().getValue();
        Map<String, String> crc32Calculations =  (Map<String, String>) rule.getHistoryService().createHistoricVariableInstanceQuery().variableName(BpmConstants.MessageDigestCalculation.mapOfEventIdsToCrc32Calculations).singleResult().getValue();
        Map<String, String> sha512Calculations =  (Map<String, String>) rule.getHistoryService().createHistoricVariableInstanceQuery().variableName(BpmConstants.MessageDigestCalculation.mapOfEventIdsToSha512Calculations).singleResult().getValue();
        assertThat(crc32Calculations.entrySet().iterator().next().getValue(), is(CRC32));
        assertThat(md5Calculations.entrySet().iterator().next().getValue(), is(MD5));
        assertThat(sha512Calculations.entrySet().iterator().next().getValue(), is(SHA512));
        verify(ingestEventStore).save(any(IngestEvent.class));
    }
}
