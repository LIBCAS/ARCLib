package cz.cas.lib.arclib.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.domain.Hash;
import cz.cas.lib.arclib.domain.HashType;
import cz.cas.lib.arclib.domain.ingestWorkflow.WorkflowDefinition;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.store.ProducerProfileStore;
import cz.cas.lib.arclib.utils.ZipUtils;
import helper.ApiTest;
import helper.TransformerFactoryWorkaroundTest;
import helper.auth.WithMockCustomUser;
import org.apache.commons.io.FileUtils;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
public class IngestIntegrationTest extends TransformerFactoryWorkaroundTest implements ApiTest {

    private static final String SIP_MD5 = "6f1ed002ab5595859014ebf0951522d9";
    private static final Path BPMN_TEST_PATH = Paths.get("src/main/resources/bpmn/ingestWorkflow.bpmn");
    private static final String PRODUCER_CONFIG = "{}";
    private static final String CORRUPTED_FILE_NAME = "eicar.com";
    private static final Path CORRUPTED_FILE_REPRESENTANT = Paths.get("src/test/resources").resolve(CORRUPTED_FILE_NAME);

    private static final Path WS = Paths.get("testWorkspace");
    private static final Path RS = Paths.get("src/test/resources");
    private static final Path QUARANTINE = WS.resolve("quarantine");
    private static final Path SIP_FOLDER = RS.resolve("testFolder");
    private static final String INGEST_CONFIG = "{\"antivirus\":{\"type\":\"clamav\",\"cmd\":[\"clamscan\",\"-r\"],\"quarantine\":\"quarantine\"}}";
    private static final String INGEST_CONFIG_WRONG = "{\"antivirus\":{\"type\":\"clamav\",\"cmd\":[\"clamscan\",\"-invalidstring\"],\"quarantine\":\"quarantine\"}}";

    private static final String SHA512 = "46f6ae30b1949e772117949bf8c7a2a13cb7f6f1207c359ad7f22399d2ec885e3c836b6dfd752a53d7f33d043da4069e1494cb0343cc40c52e09050efa6eba98";
    private static final String MD5 = "7c03df293574022b39adb90dfbc0a3d7";
    private static final long CRC32 = 3310271217L;
    private static final String eventId = "eventId";

    @Before
    public void before() throws IOException {
        Files.createDirectories(QUARANTINE);
    }

    @After
    public void after() throws IOException {
        FileUtils.cleanDirectory(WS.toFile());
        FileUtils.deleteDirectory(WS.toFile());
        Files.deleteIfExists(SIP_FOLDER.resolve(CORRUPTED_FILE_NAME));
    }

    @Inject
    private ProducerProfileStore producerProfileStore;

    @Inject
    private BatchApi api;

    @Inject
    private HistoryService historyService;

    @Inject
    private ObjectMapper objectMapper;

    @Test
    @WithMockCustomUser
    @Ignore
    public void workflowTest() throws Exception {
        byte[] bpmnDefinition = Files.readAllBytes(BPMN_TEST_PATH);

        Files.copy(CORRUPTED_FILE_REPRESENTANT, SIP_FOLDER.resolve(CORRUPTED_FILE_NAME), REPLACE_EXISTING);
        ProducerProfile p = new ProducerProfile();
        p.setWorkflowConfig(PRODUCER_CONFIG);
        WorkflowDefinition workflowDefinition = new WorkflowDefinition();
        workflowDefinition.setBpmnDefinition(new String(bpmnDefinition, StandardCharsets.UTF_8));

        p.setWorkflowDefinition(workflowDefinition);
        producerProfileStore.save(p);

        Hash sipHash = new Hash();
        sipHash.setHashValue(SIP_MD5);
        sipHash.setHashType(HashType.MD5);

        Files.createDirectories(QUARANTINE);
        MockMultipartFile sip = new MockMultipartFile("sipContent", "sipContent", "application/zip", ZipUtils.zipToByteArray(SIP_FOLDER));
        mvc(api).perform(
                fileUpload("/api/coordinator/one")
                        .file(sip)
                        .requestAttr("sipHash", sipHash)
                        .param("workflowConfig", INGEST_CONFIG_WRONG)
                        .param("producerProfileId", p.getId())
        )
                .andExpect(status().is2xxSuccessful());
        Thread.sleep(10000);

        assertThat(Files.exists(QUARANTINE.resolve(CORRUPTED_FILE_NAME)), equalTo(true));
        List<HistoricVariableInstance> globalConfigWrites = historyService.createHistoricVariableInstanceQuery().variableName(BpmConstants.ProcessVariables.latestConfig).list();
        assertThat(globalConfigWrites, hasSize(1));
        Map<String, Long> mapOfEventIdsToCrc32Calculations = (Map<String, Long>) historyService.createHistoricVariableInstanceQuery().variableName(BpmConstants.MessageDigestCalculation.mapOfEventIdsToCrc32Calculations).singleResult().getValue();
        long crc32 = mapOfEventIdsToCrc32Calculations.get(eventId);

        Map<String, Long> mapOfEventIdsToMd5Calculations = (Map<String, Long>) historyService.createHistoricVariableInstanceQuery().variableName(BpmConstants.MessageDigestCalculation.mapOfEventIdsToMd5Calculations).singleResult().getValue();
        long md5 = mapOfEventIdsToMd5Calculations.get(eventId);

        Map<String, Long> mapOfEventIdsToSha512Calculations = (Map<String, Long>) historyService.createHistoricVariableInstanceQuery().variableName(BpmConstants.MessageDigestCalculation.mapOfEventIdsToSha512Calculations).singleResult().getValue();
        long sha512 = mapOfEventIdsToSha512Calculations.get(eventId);

        assertThat(crc32, is(CRC32));
        assertThat(md5, is(MD5));
        assertThat(sha512, is(SHA512));
    }

    /**
     * empty test to show whether other tests can be initialized (dependency injection, before/after logic etc.)
     **/
    @Test
    public void smoke() {
        assertThat(producerProfileStore, notNullValue());
        assertThat(api, notNullValue());
        assertThat(historyService, notNullValue());
        assertThat(objectMapper, notNullValue());
    }
}
