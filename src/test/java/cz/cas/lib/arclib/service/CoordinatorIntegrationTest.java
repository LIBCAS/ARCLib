package cz.cas.lib.arclib.service;

import com.google.common.io.Resources;
import cz.cas.lib.arclib.api.BatchApi;
import cz.cas.lib.arclib.domain.Batch;
import cz.cas.lib.arclib.domain.BatchState;
import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflowState;
import cz.cas.lib.arclib.domain.ingestWorkflow.WorkflowDefinition;
import cz.cas.lib.arclib.domain.profiles.PathToSipId;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.domain.profiles.SipProfile;
import cz.cas.lib.arclib.domain.profiles.ValidationProfile;
import cz.cas.lib.arclib.store.*;
import helper.ApiTest;
import helper.SrDbTest;
import helper.auth.WithMockCustomUser;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.FileSystemUtils;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import static cz.cas.lib.core.util.Utils.asList;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CoordinatorIntegrationTest extends SrDbTest implements ApiTest {

    @Inject
    private BatchService batchService;

    @Inject
    private ProducerProfileStore producerProfileStore;

    @Inject
    private SipProfileStore sipProfileStore;

    @Inject
    private ValidationProfileStore validationProfileStore;

    @Inject
    private BatchApi api;

    @Inject
    private IngestWorkflowStore ingestWorkflowStore;

    @Inject
    private ProducerStore producerStore;

    @Inject
    private WorkflowDefinitionStore workflowDefinitionStore;

    @Inject
    private BatchStore batchStore;

    private static final Path BPMN_TEST_PATH = Paths.get("src/test/resources/bpmn/minimalWorkflow.bpmn");
    private static final String PRODUCER_CONFIG = "{}";
    private static final String INGEST_CONFIG = "{\"antivirus\":{\"type\":\"clamav\",\"cmd\":[\"clamscan\",\"-r\"],\"quarantine\":\"quarantine\"}}";

    private static final Path RS = Paths.get("src/test/resources");
    private static final Path FILE_STORAGE = Paths.get("fileStorage");

    private static final Path SIP_FOLDER = RS.resolve("testSip.zip");

    private static final Path FIXITY = RS.resolve("testSip.sums");
    private static final String PATH_TO_XML = "/testSip/testXml.xml";
    private static final String PATH_TO_AUTHORIAL_ID = "/mets/metsHdr/@ID";
    private static final String PATH_TO_METADATA = "/testSip/testXml.xml";

    private static final String TRANSFER_AREA_PATH_1 = "test_transfer_area_1";
    private static final String TRANSFER_AREA_PATH_2 = "test_transfer_area_2";
    private static final String TRANSFER_AREA_PATH_3 = "test_transfer_area_3";
    private static final String TRANSFER_AREA_PATH_4 = "test_transfer_area_4";

    private static final String SIP_PROFILE_XSLT_PATH = "/sipProfiles/xslts/minimalisticSipProfile.xsl";

    private String sipProfileXslt;
    private SipProfile sipProfile;
    private ValidationProfile validationProfile;
    private WorkflowDefinition workflowDefinition;

    @AfterClass
    public static void after() {
        List<String> paths = asList(
                TRANSFER_AREA_PATH_1, TRANSFER_AREA_PATH_2, TRANSFER_AREA_PATH_3, TRANSFER_AREA_PATH_4);
        paths.forEach(path -> FileSystemUtils.deleteRecursively(
                new File(FILE_STORAGE.resolve(path).toAbsolutePath().toString())));
    }

    @Before
    public void before() throws IOException {
        Files.createDirectories(FILE_STORAGE);

        sipProfile = new SipProfile();

        PathToSipId pathToSipId = new PathToSipId();
        pathToSipId.setPathToXmlGlobPattern(PATH_TO_XML);
        pathToSipId.setXPathToId(PATH_TO_AUTHORIAL_ID);
        sipProfile.setPathToSipId(pathToSipId);

        sipProfileXslt = Resources.toString(getClass().getResource(SIP_PROFILE_XSLT_PATH), StandardCharsets.UTF_8);
        sipProfile.setXsl(sipProfileXslt);

        sipProfile.setSipMetadataPathGlobPattern(PATH_TO_METADATA);
        sipProfileStore.save(sipProfile);

        validationProfile = new ValidationProfile("xml");
        validationProfileStore.save(validationProfile);

        byte[] bpmnDefinition = Files.readAllBytes(BPMN_TEST_PATH);
        workflowDefinition = new WorkflowDefinition(
                new String(bpmnDefinition, StandardCharsets.UTF_8));
        workflowDefinitionStore.save(workflowDefinition);
    }

    @After
    public void tearDown() {
        batchStore.findAll().forEach(entity -> batchStore.delete(entity));
        ingestWorkflowStore.findAll().forEach(entity -> ingestWorkflowStore.delete(entity));
    }

    /**
     * Test of ({@link CoordinatorService#processBatchOfSips(String, String, String, String)}) method.
     * <p>
     * 1. there is a new instance of batch created and its state is PROCESSED
     * 2. there is a new instance of SIP created its state is PROCESSED
     * 3. SIP ids that belong to the batch are the same as the ids of the SIP packages stored in database
     */
    @Test
    @WithMockCustomUser
    @Ignore
    public void processBatchOfSipsTest() throws Exception {
        Path transferAreaPath = FILE_STORAGE.resolve(TRANSFER_AREA_PATH_1);
        Files.createDirectories(transferAreaPath);

        Files.copy(SIP_FOLDER, transferAreaPath.resolve(SIP_FOLDER.getFileName()), REPLACE_EXISTING);
        Files.copy(FIXITY, transferAreaPath.resolve(FIXITY.getFileName()), REPLACE_EXISTING);

        Producer producer = new Producer();
        producer.setName("test producer name");
        producer.setTransferAreaPath(TRANSFER_AREA_PATH_1);
        producerStore.save(producer);

        ProducerProfile producerProfile = new ProducerProfile();
        producerProfile.setSipProfile(sipProfile);
        producerProfile.setWorkflowConfig(PRODUCER_CONFIG);
        producerProfile.setValidationProfile(validationProfile);
        producerProfile.setWorkflowDefinition(workflowDefinition);
        producerProfile.setProducer(producer);
        producerProfileStore.save(producerProfile);

        final String[] result = new String[1];
        mvc(api).perform(
                fileUpload("/api/coordinator/start")
                        .param("workflowConfig", INGEST_CONFIG)
                        .param("producerProfileId", producerProfile.getId())
        )
                .andExpect(status().is2xxSuccessful())
                .andDo(r -> result[0] = r.getResponse().getContentAsString());

        String batchId = result[0];

        /*
        wait until all the JMS communication is finished and the proper data is stored in DB
         */
        Thread.sleep(20000);

        Batch batch = batchService.get(batchId);
        assertThat(batch.getState(), is(BatchState.PROCESSED));

        Collection<IngestWorkflow> allIngestWorkflows = ingestWorkflowStore.findAll();
        assertThat(allIngestWorkflows, hasSize(1));
        allIngestWorkflows.forEach(sipPackage -> assertThat(sipPackage.getProcessingState(), is(IngestWorkflowState.PROCESSED)));

        List<IngestWorkflow> batchIngestWorkflows = batch.getIngestWorkflows();
        assertThat(batchIngestWorkflows.toArray(), is(allIngestWorkflows.toArray()));
    }

    /**
     * Test of ({@link CoordinatorService#cancelBatch(String)}) method. There are two methods called in a row:
     * 1. method ({@link CoordinatorService#processBatchOfSips(String, String, String, String})
     * 2. method ({@link CoordinatorService#cancelBatch(String)}) that cancels the batch
     * <p>
     * The test asserts that the state of the batch is CANCELED.
     */
    @Test
    @WithMockCustomUser
    @Ignore
    public void cancelTest() throws Exception {
        Path transferAreaPath = FILE_STORAGE.resolve(TRANSFER_AREA_PATH_2);
        Files.createDirectories(transferAreaPath);

        Files.copy(SIP_FOLDER, transferAreaPath.resolve(SIP_FOLDER.getFileName()), REPLACE_EXISTING);
        Files.copy(FIXITY, transferAreaPath.resolve(FIXITY.getFileName()), REPLACE_EXISTING);

        Producer producer = new Producer();
        producer.setName("test producer name");
        producer.setTransferAreaPath(TRANSFER_AREA_PATH_2);
        producerStore.save(producer);

        ProducerProfile producerProfile = new ProducerProfile();
        producerProfile.setSipProfile(sipProfile);
        producerProfile.setWorkflowConfig(PRODUCER_CONFIG);
        producerProfile.setValidationProfile(validationProfile);
        producerProfile.setWorkflowDefinition(workflowDefinition);
        producerProfile.setProducer(producer);
        producerProfileStore.save(producerProfile);

        final String[] result = new String[1];
        mvc(api).perform(
                fileUpload("/api/coordinator/start")
                        .param("workflowConfig", INGEST_CONFIG)
                        .param("producerProfileId", producerProfile.getId())
        )
                .andExpect(status().is2xxSuccessful())
                .andDo(r -> result[0] = r.getResponse().getContentAsString());


        String batchId = result[0];

        mvc(api).perform(post("/api/coordinator/" + batchId + "/cancel"))
                .andExpect(status().is2xxSuccessful());
        /*
        wait until all the JMS communication is finished and the proper data is stored in DB
         */
        Thread.sleep(12000);

        Batch batch = batchService.get(batchId);
        assertThat(batch.getState(), is(BatchState.CANCELED));
    }

    /**
     * Test of ({@link CoordinatorService#cancelBatch(String)}) method. There are two methods called in a row:
     * 1. method ({@link CoordinatorService#processBatchOfSips(String, String, String, String)})
     * 2. method ({@link CoordinatorService#suspendBatch(String)}) that suspends the batch
     * <p>
     * The test asserts that the state of the batch is SUSPENDED.
     */

    @Test
    @Ignore
    @WithMockCustomUser
    public void suspendTest() throws Exception {
        Path transferAreaPath = FILE_STORAGE.resolve(TRANSFER_AREA_PATH_3);
        Files.createDirectories(transferAreaPath);

        Files.copy(SIP_FOLDER, transferAreaPath.resolve(SIP_FOLDER.getFileName()), REPLACE_EXISTING);
        Files.copy(FIXITY, transferAreaPath.resolve(FIXITY.getFileName()), REPLACE_EXISTING);

        Producer producer = new Producer();
        producer.setName("test producer name");
        producer.setTransferAreaPath(TRANSFER_AREA_PATH_3);
        producerStore.save(producer);

        ProducerProfile producerProfile = new ProducerProfile();
        producerProfile.setSipProfile(sipProfile);
        producerProfile.setWorkflowConfig(PRODUCER_CONFIG);
        producerProfile.setValidationProfile(validationProfile);
        producerProfile.setProducer(producer);
        producerProfile.setWorkflowDefinition(workflowDefinition);
        producerProfileStore.save(producerProfile);

        final String[] result = new String[1];
        mvc(api).perform(
                fileUpload("/api/coordinator/start")
                        .param("workflowConfig", INGEST_CONFIG)
                        .param("producerProfileId", producerProfile.getId())
        )
                .andExpect(status().is2xxSuccessful())
                .andDo(r -> result[0] = r.getResponse().getContentAsString());

        String batchId = result[0];

        mvc(api).perform(post("/api/coordinator/" + batchId + "/suspend"))
                .andExpect(status().is2xxSuccessful());
        /*
        wait until all the JMS communication is finished and the proper data is stored in DB
         */
        Thread.sleep(12000);

        Batch batch = batchService.get(batchId);
        assertThat(batch.getState(), is(BatchState.SUSPENDED));
    }

    /**
     * Test of ({@link CoordinatorService#resumeBatch(String}) method. There are three methods called in a sequence:
     * 1. method ({@link CoordinatorService#start(String})
     * 2. method ({@link CoordinatorService#suspendBatch(String}) that suspends the batch
     * 3. method ({@link CoordinatorService#resumeBatch(String}) that resumes the batch
     * <p>
     * The test asserts that:
     * 1. the batch is in the state PROCESSING
     * 2. there is one SIP package created and its state is PROCESSED
     * 3. sip ids that belong to the batch are the same as the ids of the sip packages stored in database
     */
    @Test
    @Ignore
    public void resumeTest() throws Exception {
        Path transferAreaPath = FILE_STORAGE.resolve(TRANSFER_AREA_PATH_4);
        Files.createDirectories(transferAreaPath);

        Files.copy(SIP_FOLDER, transferAreaPath.resolve(SIP_FOLDER.getFileName()), REPLACE_EXISTING);
        Files.copy(FIXITY, transferAreaPath.resolve(FIXITY.getFileName()), REPLACE_EXISTING);

        Producer producer = new Producer();
        producer.setName("test producer name");
        producer.setTransferAreaPath(TRANSFER_AREA_PATH_4);
        producerStore.save(producer);

        ProducerProfile producerProfile = new ProducerProfile();
        producerProfile.setSipProfile(sipProfile);
        producerProfile.setWorkflowConfig(PRODUCER_CONFIG);
        producerProfile.setValidationProfile(validationProfile);
        producerProfile.setWorkflowDefinition(workflowDefinition);
        producerProfile.setProducer(producer);
        producerProfileStore.save(producerProfile);

        final String[] result = new String[1];
        mvc(api).perform(
                fileUpload("/api/coordinator/start")
                        .param("workflowConfig", INGEST_CONFIG)
                        .param("producerProfileId", producerProfile.getId())
        )
                .andExpect(status().is2xxSuccessful())
                .andDo(r -> result[0] = r.getResponse().getContentAsString());

        String batchId = result[0];

        mvc(api).perform(post("/api/coordinator/" + batchId + "/suspend"))
                .andExpect(status().is2xxSuccessful());
        Thread.sleep(6000);

        mvc(api).perform(post("/api/coordinator/" + batchId + "/resume"))
                .andExpect(status().is2xxSuccessful());

        /*
        wait until all the JMS communication is finished and the proper data is stored in DB
         */
        Thread.sleep(40000);

        Batch batch = batchService.get(batchId);
        assertThat(batch.getState(), is(BatchState.PROCESSED));

        Collection<IngestWorkflow> allSips = ingestWorkflowStore.findAll();
        assertThat(allSips, hasSize(1));
        allSips.forEach(sipPackage -> assertThat(sipPackage.getProcessingState(), is(IngestWorkflowState.PROCESSED)));

        List<IngestWorkflow> batchSips = batch.getIngestWorkflows();
        assertThat(batchSips.toArray(), is(allSips.toArray()));
    }
}
