package cz.cas.lib.arclib.bpm.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import cz.cas.lib.arclib.bpm.StorageSuccessVerifierDelegate;
import cz.cas.lib.arclib.domain.*;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.packages.AuthorialPackage;
import cz.cas.lib.arclib.domain.packages.Sip;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.domain.profiles.SipProfile;
import cz.cas.lib.arclib.index.solr.arclibxml.SolrArclibXmlStore;
import cz.cas.lib.arclib.security.user.UserDelegate;
import cz.cas.lib.arclib.service.BatchService;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageService;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageServiceDebug;
import cz.cas.lib.arclib.store.*;
import cz.cas.lib.core.sequence.Generator;
import cz.cas.lib.core.sequence.Sequence;
import cz.cas.lib.core.sequence.SequenceStore;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.jms.core.JmsTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Deployment(resources = "bpmn/archivalStorage.bpmn")
public class StorageSuccessVerifierDelegateTest extends DelegateTest {

    private static final String PROCESS_INSTANCE_KEY = "storageSuccessVerifierProcess";

    private Generator generator;

    private IngestWorkflowStore ingestWorkflowStore;
    private SipProfileStore sipProfileStore;
    private SipStore sipStore;
    private ProducerProfileStore producerProfileStore;
    private ProducerStore producerStore;
    private AuthorialPackageStore authorialPackageStore;
    private BatchStore batchStore;
    private BatchService batchService;
    private SolrArclibXmlStore solrArclibXmlStore;
    private UserStore userStore;
    private AipQueryStore aipQueryStore;
    private SequenceStore sequenceStore;
    private ArchivalStorageServiceDebug archivalStorageServiceDebug;
    private ArchivalStorageService archivalStorageService;
    private Producer producer;

    private StorageSuccessVerifierDelegate storageSuccessVerifierDelegate;
    private SipProfile sipProfile;
    private IngestWorkflow ingestWorkflow;
    private Batch batch;
    private UserDelegate userDelegate;
    private User user;
    private Sip sip;
    private Sip previousVersionSip;

    @Before
    public void before() throws IOException {
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
        archivalStorageServiceDebug.setWorkspace(WS.toString());

        producerProfileStore.setGenerator(generator);
        ingestWorkflowStore.setGenerator(generator);

        batchService = new BatchService();
        batchService.setDelegate(batchStore);

        user = new User();
        userDelegate = new UserDelegate(user);

        solrArclibXmlStore.setUserDetails(userDelegate);

        storageSuccessVerifierDelegate = new StorageSuccessVerifierDelegate();
        storageSuccessVerifierDelegate.setIngestWorkflowStore(ingestWorkflowStore);
        storageSuccessVerifierDelegate.setObjectMapper(new ObjectMapper());
        storageSuccessVerifierDelegate.setWorkspace(WS.toString());
        storageSuccessVerifierDelegate.setArchivalStorageService(archivalStorageService);
        storageSuccessVerifierDelegate.setArchivalStorageServiceDebug(archivalStorageServiceDebug);
        storageSuccessVerifierDelegate.setTemplate(Mockito.mock(JmsTemplate.class));
        storageSuccessVerifierDelegate.setDeleteSipFromTransferArea(false);

        sipProfile = new SipProfile();
        String sipProfileXml = Resources.toString(this.getClass().getResource(
                "/sipProfiles/xslts/comprehensiveSipProfile.xsl"), StandardCharsets.UTF_8);
        sipProfile.setXsl(sipProfileXml);
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
        ingestWorkflow.setBatch(batch);
        ingestWorkflow.setVersioningLevel(VersioningLevel.NO_VERSIONING);
        ingestWorkflow.setXmlVersionNumber(1);
        ingestWorkflow.setExternalId(EXTERNAL_ID);
        ingestWorkflowStore.save(ingestWorkflow);

        Mocks.register("storageSuccessVerifierDelegate", storageSuccessVerifierDelegate);
    }
}
