package cz.cas.lib.arclib.bpm.delegate;

import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.bpm.FixityGeneratorDelegate;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestEvent;
import cz.cas.lib.arclib.service.IngestWorkflowService;
import cz.cas.lib.arclib.service.fixity.MetsChecksumType;
import cz.cas.lib.arclib.service.preservationPlanning.ToolService;
import cz.cas.lib.arclib.store.IngestEventStore;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
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
    private IngestWorkflowService ingestWorkflowService;
    @Mock
    private ToolService toolService;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        FixityGeneratorDelegate fixityGeneratorDelegate = new FixityGeneratorDelegate();
        fixityGeneratorDelegate.setWorkspace(WS.toString());
        fixityGeneratorDelegate.setIngestEventStore(ingestEventStore);
        fixityGeneratorDelegate.setIngestWorkflowService(ingestWorkflowService);
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
        variables.put(BpmConstants.ProcessVariables.sipFileName, SIP_ZIP.getFileName().toString());
        variables.put(BpmConstants.FixityGeneration.mapOfEventIdsToSipMd5, new HashMap<>());
        variables.put(BpmConstants.FixityGeneration.mapOfEventIdsToSipCrc32, new HashMap<>());
        variables.put(BpmConstants.FixityGeneration.mapOfEventIdsToSipSha512, new HashMap<>());
        variables.put(BpmConstants.ProcessVariables.sipFolderWorkspacePath, SIP.toString());
        variables.put(BpmConstants.FixityGeneration.mapOfEventIdsToSipContentFixityData, new HashMap<String, Map<String, Triple<Long, String, String>>>());

        startJob(PROCESS_INSTANCE_KEY, variables);

        Map<String, String> md5Calculations = (Map<String, String>) rule.getHistoryService().createHistoricVariableInstanceQuery().variableName(BpmConstants.FixityGeneration.mapOfEventIdsToSipMd5).singleResult().getValue();
        Map<String, String> crc32Calculations = (Map<String, String>) rule.getHistoryService().createHistoricVariableInstanceQuery().variableName(BpmConstants.FixityGeneration.mapOfEventIdsToSipCrc32).singleResult().getValue();
        Map<String, String> sha512Calculations = (Map<String, String>) rule.getHistoryService().createHistoricVariableInstanceQuery().variableName(BpmConstants.FixityGeneration.mapOfEventIdsToSipSha512).singleResult().getValue();
        assertThat(crc32Calculations.entrySet().iterator().next().getValue(), is(CRC32));
        assertThat(md5Calculations.entrySet().iterator().next().getValue(), is(MD5));
        assertThat(sha512Calculations.entrySet().iterator().next().getValue(), is(SHA512));
        verify(ingestEventStore).save(any(IngestEvent.class));


        Map<String, Map<String, Triple<Long, String, String>>> fileFixites = (Map<String, Map<String, Triple<Long, String, String>>>) rule.getHistoryService().createHistoricVariableInstanceQuery().variableName(BpmConstants.FixityGeneration.mapOfEventIdsToSipContentFixityData).singleResult().getValue();
        assertThat(fileFixites.keySet(), hasSize(1));
        Map<String, Triple<Long, String, String>> fixities = fileFixites.entrySet().iterator().next().getValue();
        assertThat(fixities.keySet(), hasSize(33));
        Triple<Long, String, String> fileFixity = fixities.get("txt/txt_7033d800-0935-11e4-beed-5ef3fc9ae867_0006.txt");
        assertThat(fileFixity, notNullValue());
        assertThat(fileFixity.getLeft(), is(8657L));
        MetsChecksumType checksumType = EnumUtils.getEnum(MetsChecksumType.class, fileFixity.getMiddle());
        assertThat(checksumType, is(MetsChecksumType.SHA512));
        assertThat(fileFixity.getRight(), is("145f3bbf4f0457c49543eaaa73bac4cd8eb088173d41ae5c9f91ee513ac39801f70d0484b8506844357d0dfdd61f1ca82578bc8f9257b3d3bc2e046721090859"));
    }
}
