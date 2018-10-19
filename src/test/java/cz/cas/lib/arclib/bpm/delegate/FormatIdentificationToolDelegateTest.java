package cz.cas.lib.arclib.bpm.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.bpm.FormatIdentificationDelegate;
import cz.cas.lib.core.util.Utils;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

/**
 * Ensure the formatIdentification.bpmn process is working correctly
 */
@Deployment(resources = "bpmn/formatIdentification.bpmn")
public class FormatIdentificationToolDelegateTest extends DelegateTest {

    private static final String INGEST_CONFIG = "{\"formatIdentification\":{\"type\":\"DROID\",\"parsedColumn\": \"PUID\",\"pathsAndFormats\":[ {\"filePath\":\"file://\", \"format\":\"fmt/101\"}, {\"filePath\":\"this/is/another/filepath\", \"format\":\"fmt/993\"}]}}";
    private static final String PROCESS_INSTANCE_KEY = "formatIdentificationProcess";

    @BeforeClass
    public static void beforeClass() {
        FormatIdentificationDelegate formatIdentificationDelegate = new FormatIdentificationDelegate();
        formatIdentificationDelegate.setObjectMapper(new ObjectMapper());
        formatIdentificationDelegate.setWorkspace(WS.toString());
        Mocks.register("formatIdentificationDelegate", formatIdentificationDelegate);
    }

    @Test
    public void testFormatIdentificationOnSIP() {
        Map variables = new HashMap();
        variables.put(BpmConstants.ProcessVariables.ingestWorkflowId, INGEST_WORKFLOW_ID);
        variables.put(BpmConstants.ProcessVariables.ingestWorkflowExternalId, EXTERNAL_ID);
        variables.put(BpmConstants.ProcessVariables.latestConfig, INGEST_CONFIG);
        variables.put(BpmConstants.ProcessVariables.assignee, "user");
        variables.put(BpmConstants.Ingestion.originalSipFileName, ORIGINAL_SIP_FILE_NAME);
        startJob(PROCESS_INSTANCE_KEY, variables);

        Map<String, List<Utils.Pair<String, String>>> map = (Map<String, List<Utils.Pair<String, String>>>)
                rule.getHistoryService()
                        .createHistoricVariableInstanceQuery()
                        .variableName(BpmConstants.FormatIdentification.mapOfFilesToFormats)
                        .singleResult()
                        .getValue();
        assertThat(map.entrySet(), hasSize(33));
    }
}
