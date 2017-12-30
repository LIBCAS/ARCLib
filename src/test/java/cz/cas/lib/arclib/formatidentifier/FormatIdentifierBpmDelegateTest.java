package cz.cas.lib.arclib.formatidentifier;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Ensure the formatIdentification.bpmn Process is working correctly
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class FormatIdentifierBpmDelegateTest {
    private static final String SIP_ID = "KPW01169310";
    private static final String SIP_PATH = "SIP_packages/" + SIP_ID;

    @Autowired
    private HistoryService historyService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private RepositoryService repositoryService;

    private String processInstanceId = null;

    @Before
    public void before() {
        repositoryService.createDeployment()
                .addClasspathResource("processes/formatIdentification.bpmn")
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
    public void testOK() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("pathToSip", SIP_PATH);
        processInstanceId = runtimeService.startProcessInstanceByKey("FormatIdentification", variables).getId();

        Map mapOfFilesToFormats = (Map) (historyService.createHistoricVariableInstanceQuery()
                .variableName("mapOfFilesToFormats")
                .singleResult().getValue());

        assertThat(mapOfFilesToFormats, is(notNullValue()));
        assertThat(mapOfFilesToFormats.size(), is(55));
    }
}
