package cz.cas.lib.arclib.bpm.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import cz.cas.lib.arclib.bpm.ArchivalStorageDelegate;
import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.domain.*;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.packages.AuthorialPackage;
import cz.cas.lib.arclib.domain.packages.Sip;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.domain.profiles.SipProfile;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlDocument;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlStore;
import cz.cas.lib.arclib.security.user.UserDelegate;
import cz.cas.lib.arclib.service.BatchService;
import cz.cas.lib.arclib.service.IngestWorkflowService;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageService;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageServiceDebug;
import cz.cas.lib.arclib.store.*;
import cz.cas.lib.arclib.utils.ArclibUtils;
import cz.cas.lib.core.sequence.Generator;
import cz.cas.lib.core.sequence.Sequence;
import cz.cas.lib.core.sequence.SequenceStore;
import org.apache.commons.io.FileUtils;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static cz.cas.lib.core.util.Utils.asMap;
import static cz.cas.lib.core.util.Utils.fileExists;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@Deployment(resources = "bpmn/archivalStorage.bpmn")
public class ArchivalStorageDelegateTest extends DelegateTest {

    private static final String PROCESS_INSTANCE_KEY = "archivalStorageProcess";
    private static final String PATH_TO_METS = "mets_7033d800-0935-11e4-beed-5ef3fc9ae867.xml";
    private static final String SIP_FILE_NAME = SIP_ZIP.getFileName().toString();

    private static final String SIP_HASH = "50FDE99373B04363727473D00AE938A4F4DEBFD0AFB1D428337D81905F6863B3CC303BB3" +
            "31FFB3361085C3A6A2EF4589FF9CD2014C90CE90010CD3805FA5FBC6";
    private static Properties props = new Properties();
    private static String eventId = "eventId";

    private Generator generator;

    private IngestWorkflowStore ingestWorkflowStore;
    private SipProfileStore sipProfileStore;
    private SipStore sipStore;
    private ProducerProfileStore producerProfileStore;
    private ProducerStore producerStore;
    private AuthorialPackageStore authorialPackageStore;
    private BatchStore batchStore;
    private BatchService batchService;

    private UserStore userStore;
    private AipQueryStore aipQueryStore;
    private SequenceStore sequenceStore;
    private ArchivalStorageServiceDebug archivalStorageServiceDebug;
    private ArchivalStorageService archivalStorageService;
    private Producer producer;

    private ArchivalStorageDelegate archivalStorageDelegate;
    private SipProfile sipProfile;
    private IngestWorkflow ingestWorkflow;
    private Batch batch;
    private UserDelegate userDelegate;
    private User user;
    private Sip sip;
    private Sip previousVersionSip;

    @Mock
    private IndexedArclibXmlStore indexedArclibXmlStore;

    @Before
    public void before() throws IOException {
        props.load(ClassLoader.getSystemResourceAsStream("application.properties"));
        MockitoAnnotations.initMocks(this);

        aipQueryStore = new AipQueryStore();
        sequenceStore = new SequenceStore();

        userStore = new UserStore();
        userStore.setTemplate(getTemplate());

        ingestWorkflowStore = new IngestWorkflowStore();
        sipProfileStore = new SipProfileStore();
        sipStore = new SipStore();
        authorialPackageStore = new AuthorialPackageStore();
        producerStore = new ProducerStore();

        batchStore = new BatchStore();
        batchStore.setTemplate(getTemplate());

        ingestWorkflowStore.setGenerator(generator);

        producerProfileStore = new ProducerProfileStore();
        producerProfileStore.setTemplate(getTemplate());

        initializeStores(sequenceStore, aipQueryStore, ingestWorkflowStore, sipProfileStore,
                sipStore, batchStore, producerProfileStore, producerStore, authorialPackageStore, userStore);

        generator = new Generator();
        generator.setStore(sequenceStore);

        Sequence sequence = new Sequence();
        sequence.setCounter(2L);
        sequence.setFormat("'#'#");
        sequence.setId(ingestWorkflowStore.getSEQUENCE_ID());
        sequenceStore.save(sequence);

        Sequence sequence2 = new Sequence();
        sequence2.setCounter(2L);
        sequence2.setFormat("'#'#");
        sequence2.setId(producerProfileStore.getSEQUENCE_ID());
        sequenceStore.save(sequence2);

        archivalStorageService = new ArchivalStorageService();
        archivalStorageService.setBaseEndpoint("");

        archivalStorageServiceDebug = new ArchivalStorageServiceDebug();
        archivalStorageServiceDebug.setWorkspace(WS.toString(), props.getProperty("archivalStorage.debugLocation"));

        producerProfileStore.setGenerator(generator);
        ingestWorkflowStore.setGenerator(generator);

        batchService = new BatchService();
        batchService.setDelegate(batchStore);

        user = new User();
        userDelegate = new UserDelegate(user);

        indexedArclibXmlStore.setUserDetails(userDelegate);

        archivalStorageDelegate = new ArchivalStorageDelegate();
        IngestWorkflowService ingestWorkflowService = new IngestWorkflowService();
        ingestWorkflowService.setStore(ingestWorkflowStore);
        archivalStorageDelegate.setIngestWorkflowService(ingestWorkflowService);
        archivalStorageDelegate.setObjectMapper(new ObjectMapper());
        archivalStorageDelegate.setWorkspace(WS.toString());
        archivalStorageDelegate.setArchivalStorageService(archivalStorageService);
        archivalStorageDelegate.setArchivalStorageServiceDebug(archivalStorageServiceDebug);

        sipProfile = new SipProfile();
        String sipProfileXml = Resources.toString(this.getClass().getResource(
                "/sipProfiles/comprehensiveSipProfile.xsl"), StandardCharsets.UTF_8);
        sipProfile.setXsl(sipProfileXml);
        sipProfile.setSipMetadataPathGlobPattern(PATH_TO_METS);
        sipProfileStore.save(sipProfile);

        producer = new Producer();
        producer.setName("digital library of organization XYZ a.s.");
        producerStore.save(producer);

        ProducerProfile producerProfile = new ProducerProfile();
        producerProfile.setProducer(producer);
        producerProfileStore.save(producerProfile);

        batch = new Batch();
        batch.setProducerProfile(producerProfile);
        batch.setState(BatchState.PROCESSING);
        batchStore.save(batch);

        flushCache();

        AuthorialPackage authorialPackage = new AuthorialPackage();
        authorialPackage.setAuthorialId("this is an authorial id Y3DF3FDG");
        authorialPackageStore.save(authorialPackage);

        previousVersionSip = new Sip();
        sipStore.save(previousVersionSip);

        flushCache();

        sip = new Sip();
        sip.setAuthorialPackage(authorialPackage);
        sip.setPreviousVersionSip(previousVersionSip);
        sip.setVersionNumber(1);
        sipStore.save(sip);

        ingestWorkflow = new IngestWorkflow();
        ingestWorkflow.setSip(sip);
        ingestWorkflow.setId(INGEST_WORKFLOW_ID);
        ingestWorkflow.setFileName(SIP_FILE_NAME);
        ingestWorkflow.setBatch(batch);
        ingestWorkflow.setVersioningLevel(VersioningLevel.NO_VERSIONING);
        ingestWorkflow.setXmlVersionNumber(1);
        ingestWorkflow.setExternalId(EXTERNAL_ID);
        ingestWorkflowStore.save(ingestWorkflow);

        ArrayList<Object> documents = new ArrayList<>();
        documents.add("<xml/>");
        Map<String, Object> indexedFields = new HashMap<>();
        indexedFields.put(IndexedArclibXmlDocument.CONTENT, documents);
        when(indexedArclibXmlStore.findArclibXmlIndexDocument(anyString())).thenReturn(indexedFields);

        Mocks.register("archivalStorageDelegate", archivalStorageDelegate);

        Path testXmlPath = Paths.get("src/test/resources/arclibXmls/arclibXml.xml");
        FileUtils.copyToFile(new FileInputStream(testXmlPath.toFile()), ArclibUtils.getAipXmlWorkspacePath(EXTERNAL_ID, WS.toString()).toFile());
    }

    @Test
    public void testArchivalStorageDelegate() throws InterruptedException, IOException {
        Map<String, String> mapOfEventIdsToSha512Calculations = new HashMap<>();
        mapOfEventIdsToSha512Calculations.put(eventId, SIP_HASH);

        Map<String, Object> variables = asMap(BpmConstants.ProcessVariables.batchId, batch.getId());
        variables.put(BpmConstants.ProcessVariables.ingestWorkflowExternalId, EXTERNAL_ID);
        variables.put(BpmConstants.ProcessVariables.ingestWorkflowId, INGEST_WORKFLOW_ID);
        variables.put(BpmConstants.ProcessVariables.debuggingModeActive, true);
        variables.put(BpmConstants.FixityGeneration.preferredFixityGenerationEventId, eventId);
        variables.put(BpmConstants.FixityGeneration.mapOfEventIdsToSipSha512, mapOfEventIdsToSha512Calculations);
        variables.put(BpmConstants.Ingestion.sipFileName, SIP_FILE_NAME);
        variables.put(BpmConstants.ProcessVariables.sipId, sip.getId());

        variables.put(BpmConstants.ArchivalStorage.aipStoreRetries, 3);
        flushCache();

        startJob(PROCESS_INSTANCE_KEY, variables);

        Thread.sleep(3000);

        ingestWorkflow = ingestWorkflowStore.find(INGEST_WORKFLOW_ID);
        assertThat(fileExists(WS.resolve(props.getProperty("archivalStorage.debugLocation")).resolve(sip.getId())), is(true));
        assertThat(fileExists(WS.resolve(props.getProperty("archivalStorage.debugLocation")).resolve(sip.getId() + 1)), is(true));
    }
}
