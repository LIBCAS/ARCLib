package cz.cas.lib.arclib.formatidentifier;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.exists;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Ensure the droid.bpmn Process is working correctly
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class FormatIdentifierBpmDelegateTest {
    private static final String SIP_ID = "KPW01169310";
    private static final String WORKSPACE = "workspace";
    private static final Path SIP_SOURCES_FOLDER = Paths.get("SIP_packages/");

    @Autowired
    private HistoryService historyService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private RepositoryService repositoryService;

    private String processInstanceId = null;

    @BeforeClass
    public static void setUp() throws IOException {
        copySipToWorkspace(SIP_SOURCES_FOLDER, SIP_ID);
    }

    @AfterClass
    public static void tearDown() throws IOException {
//        deleteWorkspace();
    }

    @Before
    public void before() {
        repositoryService.createDeployment()
                .addClasspathResource("bpmn/droid.bpmn")
                .deploy();
    }

    @After
    public void after() {
        if (processInstanceId != null)
            historyService.deleteHistoricProcessInstance(processInstanceId);
    }

    /**
     * Test of the BPM process running DROID format identification for a specified SIP. Tests that:
     * <ul>
     * <li>the process variable with result of format identification is not null</li>
     * <li>all the files in SIP have been analyzed</li>
     * </ul>
     */
    @Test
    public void testOK() throws IOException, InterruptedException {
        Map<String, Object> variables = new HashMap<>();
        variables.put("sipId", SIP_ID);
        runtimeService.startProcessInstanceByKey("Droid", variables).getId();

        Map mapOfFilesToFormats = (Map) (historyService.createHistoricVariableInstanceQuery()
                .variableName("mapOfFilesToFormats")
                .singleResult().getValue());

        assertThat(mapOfFilesToFormats, is(notNullValue()));
        assertThat(mapOfFilesToFormats.size(), is(55));
    }

    private static void copySipToWorkspace(Path path, String sipId) throws IOException {
        if (!exists(Paths.get(WORKSPACE))) {
            createDirectories(Paths.get(WORKSPACE));
        }

        FileSystemUtils.copyRecursively(new File(path.resolve(sipId).toAbsolutePath().toString()),
                new File(Paths.get(WORKSPACE).resolve(sipId).toAbsolutePath().toString()));
    }

    private static void deleteWorkspace() {
        if (exists(Paths.get(WORKSPACE))) {
            FileSystemUtils.deleteRecursively(new File(Paths.get(WORKSPACE).toAbsolutePath().toString()));
        }
    }
}
