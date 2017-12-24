package cz.cas.lib.arclib.antivirus;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Ensure the antivirus.bpmn Process is working correctly
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class AntivirusProcessTest {

    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private RepositoryService repositoryService;

    private static final Path QUARANTINE_FOLDER = Paths.get(System.getenv("CLAMAV"), "quarantine");
    private static final String CORRUPTED_FILE_NAME = "eicar.com";
    private static final String CORRUPTED_FILE_NAME2 = "eicar2.com";
    private static final Path CORRUPTED_FILE_REPRESENTANT = Paths.get("src/test/resources").resolve(CORRUPTED_FILE_NAME);
    private static final Path CORRUPTED_FILE_REPRESENTANT2 = Paths.get("src/test/resources").resolve(CORRUPTED_FILE_NAME2);
    private static final Path SIP = Paths.get("SIP_packages/KPW01169310");

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

    @Before
    public void before() {
        repositoryService.createDeployment()
                .addClasspathResource("bpmn/antivirus.bpmn")
                .deploy();
    }

    /**
     * Tests that:
     * <ul>
     * <li>AV scan process recognizes infected files inside SIP folder and move it to quarantine folder</li>
     * <li>BPM process starts scan based on given process variable (path to file)</li>
     * <li>after scan BPM 'scan' tasks set process variable with paths to infected files</li>
     * <li>BPM 'quarantine' task moves infected files to quarantine based on the variable set by 'scan' task</li>
     * </ul>
     */
    @Test
    public void testAntivirusOnSIP() throws IOException {
        Map variables = new HashMap();
        variables.put("pathToSip", SIP.toString());
        runtimeService.startProcessInstanceByKey("antivirus", variables).getId();
        assertThat(Files.list(QUARANTINE_FOLDER).count(), equalTo(2L));
        assertThat(Files.exists(QUARANTINE_FOLDER.resolve(CORRUPTED_FILE_NAME)), equalTo(true));
        assertThat(Files.notExists(SIP.resolve(CORRUPTED_FILE_NAME)), equalTo(true));
        assertThat(Files.exists(QUARANTINE_FOLDER.resolve(CORRUPTED_FILE_NAME2)), equalTo(true));
        assertThat(Files.notExists(SIP.resolve(CORRUPTED_FILE_NAME2)), equalTo(true));
    }
}
