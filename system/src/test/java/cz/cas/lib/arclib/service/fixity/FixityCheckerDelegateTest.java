package cz.cas.lib.arclib.service.fixity;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.bpm.BpmTestConfig;
import cz.cas.lib.arclib.bpm.FixityCheckerDelegate;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestIssue;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.packages.PackageType;
import cz.cas.lib.arclib.domain.profiles.SipProfile;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import cz.cas.lib.arclib.formatlibrary.service.FormatDefinitionService;
import cz.cas.lib.arclib.service.IngestIssueService;
import cz.cas.lib.arclib.service.SipProfileService;
import cz.cas.lib.arclib.service.preservationPlanning.ToolService;
import cz.cas.lib.arclib.store.IngestIssueDefinitionStore;
import cz.cas.lib.arclib.store.IngestIssueStore;
import cz.cas.lib.arclib.store.IngestWorkflowStore;
import cz.cas.lib.arclib.store.SipProfileStore;
import cz.cas.lib.core.store.Transactional;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static helper.ThrowableAssertion.assertThrown;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Ensure the fixity.bpmn Process is working correctly
 */
@RunWith(SpringRunner.class)
@Deployment(resources = "bpmn/fixityChecker.bpmn")
public class FixityCheckerDelegateTest {

    private static final Path WS = Paths.get("testWorkspace");
    private static final String INGEST_WORKFLOW_ID = "uuid2";
    private static final String EXTERNAL_ID = "ARCLIB_000000002";

    private static final String ORIGINAL_SIP_FILE_NAME = "KPW01169310";
    private static final String ORIGINAL_SIP_BAG_FILE_NAME = "BAG";

    private static final Path SIP = Paths.get("src/test/resources/SIP_PACKAGE/KPW01169310");
    private static final Path WS_SIP_LOCATION = Paths.get(WS.toString(), EXTERNAL_ID, SIP.getFileName().toString());

    private static final Path SIP_BAG = Paths.get("src/test/resources/SIP_PACKAGE/BAG");
    private static final Path WS_SIP_BAG_LOCATION = Paths.get(WS.toString(), EXTERNAL_ID, ORIGINAL_SIP_BAG_FILE_NAME);

    private static final String CONFIG1 = "{\"fixityCheck\":{\"0\":{" +
            "\"continueOnUnsupportedChecksumType\":true}}}";
    private static final String CONFIG2 = "{\"fixityCheck\":{\"0\":{" +
            "\"continueOnMissingFiles\":false," +
            "\"continueOnUnsupportedChecksumType\":true}}}";

    private static final String PROCESS_INSTANCE_KEY = "fixity";
    private static final String INVALID_CHECKSUM_FILE_1 = "TXT_KPW01169310_0002.TXT";
    private static final String INVALID_CHECKSUM_FILE_2 = "AMD_METS_KPW01169310_0004.xml";
    private static final String UNSUPPORTED_CHECKSUM_FILE = "AMD_METS_KPW01169310_0008.xml";
    private static final String MISSING_FILE = "ALTO_KPW01169310_0007.XML";
    private static final String eventId = "eventId";

    @Rule
    public ProcessEngineRule rule = new ProcessEngineRule(new BpmTestConfig().buildProcessEngine());

    @Mock
    private SipProfileStore sipProfileStore;

    @Mock
    private IngestWorkflowStore ingestWorkflowStore;
    @Mock
    private FormatDefinitionService formatDefinitionService;
    @Mock
    private ToolService toolService;
    @Mock
    private IngestIssueDefinitionStore ingestIssueDefinitionStore;
    @Spy
    private MetsFixityChecker metsFixityVerifier = new MetsFixityChecker();
    @Spy
    private BagitFixityChecker bagitPackageFixityVerifier = new BagitFixityChecker();
    private SipProfile bagProfile = new SipProfile();
    private SipProfile metsProfile = new SipProfile();
    private TreeMap<String, Pair<String, String>> droidResult = new TreeMap<>();
    @Mock
    private IngestIssueStore ingestIssueStore;
    @Captor
    ArgumentCaptor<List<IngestIssue>> captor;

    private SipProfileService sipProfileService;

    /**
     * used for asynchronous process instances, start process instance and executes it job right afterwards
     *
     * @param processInstanceKey
     * @param variables
     * @return processInstanceId
     */
    public String startJob(String processInstanceKey, Map<String, Object> variables) {
        String id = rule.getRuntimeService().startProcessInstanceByKey(processInstanceKey, variables).getId();
        Job job = rule.getManagementService().createJobQuery().processInstanceId(id).singleResult();
        rule.getManagementService().executeJob(job.getId());
        return id;
    }

    @Before
    @Transactional
    public void before() throws IOException {
        Files.createDirectories(WS);

        bagProfile.setPackageType(PackageType.BAGIT);
        bagProfile.setSipMetadataPathGlobPattern("bag-info.txt");
        when(sipProfileStore.find(eq(bagProfile.getId()))).thenReturn(bagProfile);

        IngestWorkflow ingestWorkflow = new IngestWorkflow();
        ingestWorkflow.setId(INGEST_WORKFLOW_ID);
        ingestWorkflow.setExternalId(EXTERNAL_ID);
        when(ingestWorkflowStore.findByExternalId(EXTERNAL_ID)).thenReturn(ingestWorkflow);

        metsProfile.setPackageType(PackageType.METS);
        metsProfile.setSipMetadataPathGlobPattern("METS_KPW01169310.xml");
        when(sipProfileStore.find(eq(metsProfile.getId()))).thenReturn(metsProfile);

        metsFixityVerifier.setMd5Counter(new Md5Counter());
        metsFixityVerifier.setIngestWorkflowStore(ingestWorkflowStore);

        bagitPackageFixityVerifier.setSha512Counter(new Sha512Counter());
        bagitPackageFixityVerifier.setIngestWorkflowStore(ingestWorkflowStore);

        IngestIssueService iiservice = new IngestIssueService();
        iiservice.setIngestIssueStore(ingestIssueStore);
        metsFixityVerifier.setIngestIssueService(iiservice);
        metsFixityVerifier.setIngestWorkflowStore(ingestWorkflowStore);
        metsFixityVerifier.setFormatDefinitionService(formatDefinitionService);
        metsFixityVerifier.setIngestIssueDefinitionStore(ingestIssueDefinitionStore);
        metsFixityVerifier.setToolService(toolService);

        bagitPackageFixityVerifier.setIngestIssueService(iiservice);
        bagitPackageFixityVerifier.setIngestWorkflowStore(ingestWorkflowStore);
        bagitPackageFixityVerifier.setFormatDefinitionService(formatDefinitionService);
        bagitPackageFixityVerifier.setIngestIssueDefinitionStore(ingestIssueDefinitionStore);
        bagitPackageFixityVerifier.setToolService(toolService);

        droidResult.put("data/TXT/TXT_KPW01169310_0002.TXT", Pair.of("txt", "mock"));
        droidResult.put("data/amdSec/AMD_METS_KPW01169310_0004.xml", Pair.of("xml", "mock"));
        droidResult.put("data/amdSec/AMD_METS_KPW01169310_0008.xml", Pair.of("xml", "mock"));
        droidResult.put("data/ALTO/ALTO_KPW01169310_0006.XML", Pair.of("xml", "mock"));

        droidResult.put("TXT/TXT_KPW01169310_0002.TXT", Pair.of("txt", "mock"));
        droidResult.put("amdSec/AMD_METS_KPW01169310_0004.xml", Pair.of("xml", "mock"));
        droidResult.put("amdSec/AMD_METS_KPW01169310_0008.xml", Pair.of("xml", "mock"));
        droidResult.put("ALTO/ALTO_KPW01169310_0006.XML", Pair.of("xml", "mock"));

        sipProfileService = new SipProfileService();
        sipProfileService.setStore(sipProfileStore);

        FixityCheckerDelegate fixityCheckerDelegate = new FixityCheckerDelegate();
        fixityCheckerDelegate.setObjectMapper(new ObjectMapper());
        fixityCheckerDelegate.setMetsFixityVerifier(metsFixityVerifier);
        fixityCheckerDelegate.setBagitFixityVerifier(bagitPackageFixityVerifier);
        fixityCheckerDelegate.setSipProfileService(sipProfileService);
        fixityCheckerDelegate.setWorkspace(WS.toString());
        Mocks.register("fixityCheckerDelegate", fixityCheckerDelegate);
    }

    @After
    public void after() throws IOException {
        FileUtils.cleanDirectory(WS.toFile());
        FileUtils.deleteDirectory(WS.toFile());
    }

    @Test
    public void testMetsSipUnsupportedTypeContinueInvalidChecksumsStopProcess() throws IncidentException, IOException {
        FileUtils.copyDirectory(SIP.toFile(), WS_SIP_LOCATION.toFile());

        Map variables = new HashMap();
        variables.put(BpmConstants.ProcessVariables.ingestWorkflowId, INGEST_WORKFLOW_ID);
        variables.put(BpmConstants.ProcessVariables.ingestWorkflowExternalId, EXTERNAL_ID);
        variables.put(BpmConstants.ProcessVariables.sipProfileId, metsProfile.getId());
        variables.put(BpmConstants.ProcessVariables.latestConfig, CONFIG1);
        variables.put(BpmConstants.Ingestion.sipFileName, ORIGINAL_SIP_FILE_NAME);
        Map<String, TreeMap<String, Pair<String, String>>> mapOfEventIdsToMapsOfFilesToFormats = new HashMap<>();
        mapOfEventIdsToMapsOfFilesToFormats.put(eventId, droidResult);
        variables.put(BpmConstants.FormatIdentification.preferredFormatIdentificationEventId, eventId);
        variables.put(BpmConstants.FormatIdentification.mapOfEventIdsToMapsOfFilesToFormats, mapOfEventIdsToMapsOfFilesToFormats);
        variables.put(BpmConstants.ProcessVariables.sipFolderWorkspacePath, WS_SIP_LOCATION.toAbsolutePath().toString());
        variables.put(BpmConstants.FixityCheck.fixityCheckToolCounter, 0);
        assertThrown(() -> startJob(PROCESS_INSTANCE_KEY, variables)).hasCauseInstanceOf(IncidentException.class);
        verify(ingestIssueStore, times(2)).save(captor.capture());

        assertThat(captor.getAllValues().get(0).get(0).isSuccess(), is(true));
        assertThat(captor.getAllValues().get(0).get(0).getDescription(), containsString(FixityCheckerDelegate.CONFIG_UNSUPPORTED_CHECKSUM_TYPE));
        assertThat(captor.getAllValues().get(0).get(0).getDescription(), containsString("CRC"));
        assertThat(captor.getAllValues().get(0).get(0).getDescription(), containsString(UNSUPPORTED_CHECKSUM_FILE));

        assertThat(captor.getAllValues().get(1).get(0).isSuccess(), is(false));
        assertThat(captor.getAllValues().get(1).get(0).getDescription(), containsString(FixityCheckerDelegate.CONFIG_INVALID_CHECKSUMS));
        assertThat(captor.getAllValues().get(1).get(0).getDescription(), containsString(INVALID_CHECKSUM_FILE_1));
        assertThat(captor.getAllValues().get(1).get(1).getDescription(), containsString(INVALID_CHECKSUM_FILE_2));

        verify(metsFixityVerifier, times(1)).invokeUnsupportedChecksumTypeIssue(any(), any(), any(), any(), any());
        verify(metsFixityVerifier, times(1)).invokeInvalidChecksumsIssue(any(), any(), any(), any(), any());
        verify(metsFixityVerifier, never()).invokeMissingFilesIssue(any(), any(), any(), any(), any());
        verify(formatDefinitionService, times(2)).findPreferredDefinitionsByPuid(eq("xml"));
        verify(formatDefinitionService).findPreferredDefinitionsByPuid(eq("txt"));
    }

    @Test
    public void testMetsSipUnsupportedChecksumContinueMissingFilesKillProcess() throws IncidentException, IOException {
        FileUtils.copyDirectory(SIP.toFile(), WS_SIP_LOCATION.toFile());
        Path missingFile = WS_SIP_LOCATION.resolve("ALTO/" + MISSING_FILE);
        Files.delete(missingFile);

        Map variables = new HashMap();
        variables.put(BpmConstants.ProcessVariables.ingestWorkflowId, INGEST_WORKFLOW_ID);
        variables.put(BpmConstants.ProcessVariables.ingestWorkflowExternalId, EXTERNAL_ID);
        variables.put(BpmConstants.ProcessVariables.sipProfileId, metsProfile.getId());
        variables.put(BpmConstants.Ingestion.sipFileName, ORIGINAL_SIP_FILE_NAME);
        variables.put(BpmConstants.ProcessVariables.latestConfig, CONFIG2);
        Map<String, TreeMap<String, Pair<String, String>>> mapOfEventIdsToMapsOfFilesToFormats = new HashMap<>();
        mapOfEventIdsToMapsOfFilesToFormats.put(eventId, droidResult);
        variables.put(BpmConstants.FormatIdentification.preferredFormatIdentificationEventId, eventId);
        variables.put(BpmConstants.FormatIdentification.mapOfEventIdsToMapsOfFilesToFormats, mapOfEventIdsToMapsOfFilesToFormats);
        variables.put(BpmConstants.ProcessVariables.sipFolderWorkspacePath, WS_SIP_LOCATION.toAbsolutePath().toString());
        variables.put(BpmConstants.FixityCheck.fixityCheckToolCounter, 0);
        startJob(PROCESS_INSTANCE_KEY, variables);

        verify(ingestIssueStore, times(2)).save(captor.capture());

        assertThat(captor.getAllValues().get(0).get(0).isSuccess(), is(true));
        assertThat(captor.getAllValues().get(0).get(0).getDescription(), containsString(FixityCheckerDelegate.CONFIG_UNSUPPORTED_CHECKSUM_TYPE));
        assertThat(captor.getAllValues().get(0).get(0).getDescription(), containsString("CRC"));
        assertThat(captor.getAllValues().get(0).get(0).getDescription(), containsString(UNSUPPORTED_CHECKSUM_FILE));

        assertThat(captor.getAllValues().get(1).get(0).isSuccess(), is(true));
        assertThat(captor.getAllValues().get(1).get(0).getDescription(), containsString(FixityCheckerDelegate.CONFIG_MISSING_FILES));
        assertThat(captor.getAllValues().get(1).get(0).getDescription(), containsString(MISSING_FILE));

        verify(metsFixityVerifier, times(1)).invokeUnsupportedChecksumTypeIssue(any(), any(), any(), any(), any());
        verify(metsFixityVerifier, times(1)).invokeMissingFilesIssue(any(), any(), any(), any(), any());
        verify(metsFixityVerifier, never()).invokeInvalidChecksumsIssue(any(), any(), any(), any(), any());
        verify(formatDefinitionService).findPreferredDefinitionsByPuid(eq("xml"));
        verify(formatDefinitionService, times(0)).findPreferredDefinitionsByPuid(eq("txt"));
    }

    @Test
    public void testBagSipUnsupportedTypeContinueInvalidChecksumsStopProcess() throws IncidentException, IOException {
        FileUtils.copyDirectory(SIP_BAG.toFile(), WS_SIP_BAG_LOCATION.toFile());

        Map variables = new HashMap();
        variables.put(BpmConstants.ProcessVariables.ingestWorkflowId, INGEST_WORKFLOW_ID);
        variables.put(BpmConstants.ProcessVariables.ingestWorkflowExternalId, EXTERNAL_ID);
        variables.put(BpmConstants.ProcessVariables.sipProfileId, bagProfile.getId());
        variables.put(BpmConstants.Ingestion.sipFileName, ORIGINAL_SIP_BAG_FILE_NAME);
        variables.put(BpmConstants.ProcessVariables.latestConfig, CONFIG1);
        Map<String, TreeMap<String, Pair<String, String>>> mapOfEventIdsToMapsOfFilesToFormats = new HashMap<>();
        mapOfEventIdsToMapsOfFilesToFormats.put(eventId, droidResult);
        variables.put(BpmConstants.FormatIdentification.preferredFormatIdentificationEventId, eventId);
        variables.put(BpmConstants.FormatIdentification.mapOfEventIdsToMapsOfFilesToFormats, mapOfEventIdsToMapsOfFilesToFormats);
        variables.put(BpmConstants.ProcessVariables.sipFolderWorkspacePath, WS_SIP_BAG_LOCATION.toAbsolutePath().toString());
        variables.put(BpmConstants.FixityCheck.fixityCheckToolCounter, 0);
        assertThrown(() -> startJob(PROCESS_INSTANCE_KEY, variables)).hasCauseInstanceOf(IncidentException.class);
        verify(ingestIssueStore, times(2)).save(captor.capture());

        assertThat(captor.getAllValues().get(0).get(0).isSuccess(), is(true));
        assertThat(captor.getAllValues().get(0).get(0).getDescription(), containsString(FixityCheckerDelegate.CONFIG_UNSUPPORTED_CHECKSUM_TYPE));
        assertThat(captor.getAllValues().get(0).get(0).getDescription(), containsString("CRC"));
        assertThat(captor.getAllValues().get(0).get(0).getDescription(), containsString(UNSUPPORTED_CHECKSUM_FILE));

        assertThat(captor.getAllValues().get(1).get(0).isSuccess(), is(false));
        assertThat(captor.getAllValues().get(1).get(0).getDescription(), containsString(FixityCheckerDelegate.CONFIG_INVALID_CHECKSUMS));
        assertThat(captor.getAllValues().get(1).get(0).getDescription(), containsString(INVALID_CHECKSUM_FILE_2));
        assertThat(captor.getAllValues().get(1).get(1).getDescription(), containsString(INVALID_CHECKSUM_FILE_1));

        verify(bagitPackageFixityVerifier, times(1)).invokeUnsupportedChecksumTypeIssue(any(), any(), any(), any(), any());
        verify(bagitPackageFixityVerifier, times(1)).invokeInvalidChecksumsIssue(any(), any(), any(), any(), any());
        verify(bagitPackageFixityVerifier, never()).invokeMissingFilesIssue(any(), any(), any(), any(), any());
        verify(formatDefinitionService, times(2)).findPreferredDefinitionsByPuid(eq("xml"));
        verify(formatDefinitionService).findPreferredDefinitionsByPuid(eq("txt"));
    }

    @Test
    public void testBagSipUnsupportedChecksumContinueMissingFilesKillProcess() throws IncidentException, IOException {
        FileUtils.copyDirectory(SIP_BAG.toFile(), WS_SIP_BAG_LOCATION.toFile());
        Path missingFile = WS_SIP_BAG_LOCATION.resolve("data/ALTO/" + MISSING_FILE);
        Files.delete(missingFile);

        Map variables = new HashMap();
        variables.put(BpmConstants.ProcessVariables.ingestWorkflowId, INGEST_WORKFLOW_ID);
        variables.put(BpmConstants.ProcessVariables.ingestWorkflowExternalId, EXTERNAL_ID);
        variables.put(BpmConstants.ProcessVariables.sipProfileId, bagProfile.getId());
        variables.put(BpmConstants.Ingestion.sipFileName, ORIGINAL_SIP_BAG_FILE_NAME);
        variables.put(BpmConstants.ProcessVariables.latestConfig, CONFIG2);
        Map<String, TreeMap<String, Pair<String, String>>> mapOfEventIdsToMapsOfFilesToFormats = new HashMap<>();
        mapOfEventIdsToMapsOfFilesToFormats.put(eventId, droidResult);
        variables.put(BpmConstants.FormatIdentification.preferredFormatIdentificationEventId, eventId);
        variables.put(BpmConstants.FormatIdentification.mapOfEventIdsToMapsOfFilesToFormats, mapOfEventIdsToMapsOfFilesToFormats);
        variables.put(BpmConstants.ProcessVariables.sipFolderWorkspacePath, WS_SIP_BAG_LOCATION.toAbsolutePath().toString());
        variables.put(BpmConstants.FixityCheck.fixityCheckToolCounter, 0);
        startJob(PROCESS_INSTANCE_KEY, variables);

        verify(ingestIssueStore, times(2)).save(captor.capture());

        assertThat(captor.getAllValues().get(0).get(0).isSuccess(), is(true));
        assertThat(captor.getAllValues().get(0).get(0).getDescription(), containsString(FixityCheckerDelegate.CONFIG_UNSUPPORTED_CHECKSUM_TYPE));
        assertThat(captor.getAllValues().get(0).get(0).getDescription(), containsString("CRC"));
        assertThat(captor.getAllValues().get(0).get(0).getDescription(), containsString(UNSUPPORTED_CHECKSUM_FILE));

        assertThat(captor.getAllValues().get(1).get(0).isSuccess(), is(true));
        assertThat(captor.getAllValues().get(1).get(0).getDescription(), containsString(FixityCheckerDelegate.CONFIG_MISSING_FILES));
        assertThat(captor.getAllValues().get(1).get(0).getDescription(), containsString(MISSING_FILE));

        verify(bagitPackageFixityVerifier, times(1)).invokeUnsupportedChecksumTypeIssue(any(), any(), any(), any(), any());
        verify(bagitPackageFixityVerifier, times(1)).invokeMissingFilesIssue(any(), any(), any(), any(), any());
        verify(bagitPackageFixityVerifier, never()).invokeInvalidChecksumsIssue(any(), any(), any(), any(), any());
        verify(formatDefinitionService).findPreferredDefinitionsByPuid(eq("xml"));
        verify(formatDefinitionService, times(0)).findPreferredDefinitionsByPuid(eq("txt"));
    }
}
