package cz.cas.lib.arclib.fixity;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;


/**
 * Ensure the fixity.bpmn Process is working correctly
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class FixityProcessTest {

    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private RepositoryService repositoryService;

    private static final Path PATH_TO_SIP_META_XML = Paths.get("SIP_packages/KPW01169310/METS_KPW01169310.xml");
    private String INVALID_CHECKSUM_FILE_1 = PATH_TO_SIP_META_XML.getParent().resolve("./TXT/TXT_KPW01169310_0002.TXT").normalize().toAbsolutePath().toString();
    private String INVALID_CHECKSUM_FILE_2 = PATH_TO_SIP_META_XML.getParent().resolve("./amdSec/AMD_METS_KPW01169310_0004.xml").normalize().toAbsolutePath().toString();
    private String processInstanceId = null;

    @Before
    public void before() {
        repositoryService.createDeployment()
                .addClasspathResource("bpmn/fixity.bpmn")
                .deploy();
    }

    @After
    public void after() {
        if (processInstanceId != null)
            historyService.deleteHistoricProcessInstance(processInstanceId);
    }

    /**
     * Tests that:
     * <ul>
     * <li>computed digests matches the checksum of a file and that comparison is case-insensitive</li>
     * <li>BPM process starts verification of all SIP files based on given process variables (path to SIP META file)</li>
     * <li>after comparison BPM process sets correct process variable based on verification result (contains paths to files with invalid checksum)</li>
     * </ul>
     */
    @Test
    public void testSIP() {
        Map variables = new HashMap();
        variables.put("sipMetaPath", PATH_TO_SIP_META_XML.toString());
        processInstanceId = runtimeService.startProcessInstanceByKey("fixity", variables).getId();
        List<String> invalidChecksumFiles = (ArrayList<String>) historyService.createHistoricVariableInstanceQuery().variableName("invalidChecksumFiles").singleResult().getValue();
        assertThat(invalidChecksumFiles, hasSize(2));
        assertThat(invalidChecksumFiles, containsInAnyOrder(INVALID_CHECKSUM_FILE_1, INVALID_CHECKSUM_FILE_2));
    }

}