package cz.cas.lib.arclib.bpm.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.bpm.FormatIdentificationDelegate;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestEvent;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.preservationPlanning.Tool;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.formatlibrary.domain.Format;
import cz.cas.lib.arclib.formatlibrary.domain.FormatDefinition;
import cz.cas.lib.arclib.formatlibrary.service.FormatDefinitionService;
import cz.cas.lib.arclib.service.IngestWorkflowService;
import cz.cas.lib.arclib.service.ProducerProfileService;
import cz.cas.lib.arclib.service.preservationPlanning.ToolService;
import cz.cas.lib.arclib.store.FormatOccurrenceStore;
import cz.cas.lib.arclib.store.IngestEventStore;
import cz.cas.lib.arclib.store.IngestWorkflowStore;
import org.apache.commons.lang3.tuple.Pair;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * Ensure the formatIdentification.bpmn process is working correctly
 */
@Deployment(resources = "bpmn/formatIdentification.bpmn")
public class FormatIdentificationToolDelegateTest extends DelegateTest {

    private static final String INGEST_CONFIG = "{\"formatIdentification\":{\"0\":{\"type\":\"DROID\",\"pathsAndFormats\":{\"0\":{\"filePath\":\"\", \"format\":\"fmt/101\"}, \"1\":{\"filePath\":\"this/is/another/filepath\", \"format\":\"fmt/993\"}}}}}";
    private static final String PROCESS_INSTANCE_KEY = "formatIdentificationProcess";
    private static final String eventId = "eventId";
    @Mock
    private IngestWorkflowStore ingestWorkflowStore;
    @Mock
    private ToolService toolService;
    @Mock
    private ProducerProfileService producerProfileService;
    @Mock
    private FormatDefinitionService formatDefinitionService;
    @Mock
    private FormatOccurrenceStore formatOccurrenceStore;
    @Mock
    private IngestEventStore ingestEventStore;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        FormatIdentificationDelegate formatIdentificationDelegate = new FormatIdentificationDelegate();
        formatIdentificationDelegate.setObjectMapper(new ObjectMapper());
        formatIdentificationDelegate.setWorkspace(WS.toString());
        IngestWorkflowService ingestWorkflowService = new IngestWorkflowService();
        ingestWorkflowService.setStore(ingestWorkflowStore);
        formatIdentificationDelegate.setIngestWorkflowService(ingestWorkflowService);
        formatIdentificationDelegate.setToolService(toolService);
        formatIdentificationDelegate.setFormatDefinitionService(formatDefinitionService);
        formatIdentificationDelegate.setFormatOccurrenceStore(formatOccurrenceStore);
        formatIdentificationDelegate.setProducerProfileService(producerProfileService);
        formatIdentificationDelegate.setIngestEventStore(ingestEventStore);
        Mocks.register("formatIdentificationDelegate", formatIdentificationDelegate);

        IngestWorkflow ingestWorkflow = new IngestWorkflow();
        ingestWorkflow.setId(INGEST_WORKFLOW_ID);
        ingestWorkflow.setExternalId(EXTERNAL_ID);
        when(ingestWorkflowStore.findByExternalId(EXTERNAL_ID)).thenReturn(ingestWorkflow);
        Tool tool = new Tool();
        tool.setName("name");
        tool.setVersion("version");
        when(toolService.createNewToolVersionIfNeeded(any(), any(), any())).thenReturn(tool);
        ProducerProfile producerProfile = new ProducerProfile();
        producerProfile.setExternalId("ppexid");
        when(producerProfileService.findByExternalId(any())).thenReturn(producerProfile);

        FormatDefinition formatDefinition = new FormatDefinition();
        formatDefinition.setPreferred(true);

        Format format = new Format();
        format.setPuid("puid");

        when(formatDefinitionService.findPreferredDefinitionsByPuid(any())).thenReturn(formatDefinition);
        IngestEvent ev = new IngestEvent();
        ev.setCreated(Instant.now());
        when(ingestEventStore.save(any(IngestEvent.class))).thenReturn(ev);
    }

    @Test
    public void testFormatIdentificationOnSIP() {
        Map variables = new HashMap();
        variables.put(BpmConstants.ProcessVariables.ingestWorkflowId, INGEST_WORKFLOW_ID);
        variables.put(BpmConstants.ProcessVariables.ingestWorkflowExternalId, EXTERNAL_ID);
        variables.put(BpmConstants.ProcessVariables.latestConfig, INGEST_CONFIG);
        variables.put(BpmConstants.ProcessVariables.responsiblePerson, "user");
        variables.put(BpmConstants.ProcessVariables.sipFileName, ORIGINAL_SIP_FILE_NAME);
        variables.put(BpmConstants.ProcessVariables.sipFolderWorkspacePath, SIP.toAbsolutePath().toString());
        variables.put(BpmConstants.FormatIdentification.preferredFormatIdentificationEventId, 0);
        variables.put(BpmConstants.FormatIdentification.mapOfEventIdsToMapsOfFilesToFormats, new HashMap<>());
        startJob(PROCESS_INSTANCE_KEY, variables);

        Map<String, Map<String, List<Pair<String, String>>>> mapOfEventIdsToMapsOfFilesToFormats = (Map<String, Map<String, List<Pair<String, String>>>>)
                rule.getHistoryService()
                        .createHistoricVariableInstanceQuery()
                        .variableName(BpmConstants.FormatIdentification.mapOfEventIdsToMapsOfFilesToFormats)
                        .singleResult()
                        .getValue();
        Map<String, List<Pair<String, String>>> mapOfFilesToFormats = mapOfEventIdsToMapsOfFilesToFormats.entrySet().iterator().next().getValue();
        assertThat(mapOfFilesToFormats.entrySet(), hasSize(33));
    }
}
