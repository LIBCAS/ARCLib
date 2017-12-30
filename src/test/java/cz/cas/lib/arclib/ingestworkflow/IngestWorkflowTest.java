package cz.cas.lib.arclib.ingestworkflow;

import cz.cas.lib.arclib.domain.ValidationProfile;
import cz.cas.lib.arclib.store.ValidationProfileStore;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Ensure the antivirus.bpmn Process is working correctly
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class IngestWorkflowTest {

    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private HistoryService historyService;
    @Inject
    private ValidationProfileStore validationProfileStore;

    private static final Path QUARANTINE_FOLDER = Paths.get(System.getenv("CLAMAV"), "quarantine");
    private static final String CORRUPTED_FILE_NAME = "eicar.com";
    private static final String CORRUPTED_FILE_NAME2 = "eicar2.com";
    private static final Path CORRUPTED_FILE_REPRESENTANT = Paths.get("src/test/resources").resolve(CORRUPTED_FILE_NAME);
    private static final Path CORRUPTED_FILE_REPRESENTANT2 = Paths.get("src/test/resources").resolve(CORRUPTED_FILE_NAME2);
    private static final Path SIP = Paths.get("SIP_packages/KPW01169310");


    private static final Path PATH_TO_SIP_META_XML = Paths.get("SIP_packages/KPW01169310/METS_KPW01169310.xml");
    private String processInstanceId = null;
    private ValidationProfile validationProfile;

    @BeforeClass
    public static void beforeClass() throws IOException {
        Files.copy(CORRUPTED_FILE_REPRESENTANT, SIP.resolve(CORRUPTED_FILE_NAME), REPLACE_EXISTING);
        Files.copy(CORRUPTED_FILE_REPRESENTANT2, SIP.resolve(CORRUPTED_FILE_NAME2), REPLACE_EXISTING);

    }

    @AfterClass
    public static void afterClass() throws IOException {
        Files.deleteIfExists(QUARANTINE_FOLDER.resolve(CORRUPTED_FILE_NAME));
        Files.deleteIfExists(QUARANTINE_FOLDER.resolve(CORRUPTED_FILE_NAME2));
    }

    @After
    public void after() {
        if (processInstanceId != null)
            historyService.deleteHistoricProcessInstance(processInstanceId);
    }

    @Before
    public void before() throws IOException {
        repositoryService.createDeployment()
                .addClasspathResource("bpmn/IngestWorkflow.bpmn")
                .deploy();

        validationProfile = new ValidationProfile();
        InputStream inputStream = getClass().getResourceAsStream("/validation/validationProfileMixedChecks.xml");
        String xml = readFromInputStream(inputStream);

        validationProfile.setXml(xml);
        validationProfileStore.save(validationProfile);
    }

    @Test
    public void testIngestWorkflowOnSip() {
        Map variables = new HashMap();
        variables.put("pathToSip", SIP.toString());
        variables.put("sipMetaPath", PATH_TO_SIP_META_XML.toString());
        variables.put("validationProfileId", validationProfile.getId());

        runtimeService.startProcessInstanceByKey("IngestWorkflow", variables).getId();
    }

    private String readFromInputStream(InputStream inputStream) throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }
}
