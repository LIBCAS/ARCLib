package cz.cas.lib.arclib.bpm.delegate;

import cz.cas.lib.arclib.bpm.BpmTestConfig;
import helper.SrDbTest;
import org.apache.commons.io.FileUtils;
import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;


public abstract class DelegateTest extends SrDbTest {

    public static final Path SIP = Paths.get("src/test/resources/SIP_PACKAGE/" +
            "7033d800-0935-11e4-beed-5ef3fc9ae867");
    public static final Path SIP_ZIP = Paths.get("src/test/resources/SIP_PACKAGE/zipped_with_hashes/7033d800-0935-11e4-beed-5ef3fc9ae867.zip");
    public static final Path WS = Paths.get("testWorkspace");
    public static final String INGEST_WORKFLOW_ID = "uuid1";
    public static final String EXTERNAL_ID = "ARCLIB_000000001";
    public static final String ORIGINAL_SIP_FILE_NAME = "7033d800-0935-11e4-beed-5ef3fc9ae867.zip";
    public static final Path WS_SIP_LOCATION = Paths.get(WS.toString(), EXTERNAL_ID, SIP.getFileName().toString());
    public static final Path WS_SIP_ZIP_LOCATION = Paths.get(WS.toString(), EXTERNAL_ID, SIP_ZIP.getFileName().toString());

    @Rule
    public ProcessEngineRule rule = new ProcessEngineRule(new BpmTestConfig().buildProcessEngine());

    @AfterClass
    public static void tearDown() throws IOException {
        FileUtils.cleanDirectory(WS.toFile());
        FileUtils.deleteDirectory(WS.toFile());
    }

    @BeforeClass
    public static void setUp() throws IOException {
        Files.createDirectories(WS_SIP_LOCATION);
        FileUtils.copyDirectory(SIP.toFile(), WS_SIP_LOCATION.toFile());
        FileUtils.copyFile(SIP_ZIP.toFile(), WS_SIP_ZIP_LOCATION.toFile());
    }

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
}
