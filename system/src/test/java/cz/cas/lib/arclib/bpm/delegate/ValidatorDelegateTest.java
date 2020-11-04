package cz.cas.lib.arclib.bpm.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.bpm.ValidatorDelegate;
import cz.cas.lib.arclib.domain.IngestToolFunction;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.preservationPlanning.Tool;
import cz.cas.lib.arclib.domain.profiles.ValidationProfile;
import cz.cas.lib.arclib.exception.validation.MissingFile;
import cz.cas.lib.arclib.service.IngestWorkflowService;
import cz.cas.lib.arclib.service.preservationPlanning.ToolService;
import cz.cas.lib.arclib.service.validator.Validator;
import cz.cas.lib.arclib.store.IngestEventStore;
import cz.cas.lib.arclib.store.IngestWorkflowStore;
import cz.cas.lib.arclib.store.ToolStore;
import cz.cas.lib.arclib.store.ValidationProfileStore;
import cz.cas.lib.core.sequence.Generator;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static helper.ThrowableAssertion.assertThrown;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Ensure the validator.bpmn process is working correctly
 */
@Deployment(resources = "bpmn/validator.bpmn")
public class ValidatorDelegateTest extends DelegateTest {

    private static final String PROCESS_INSTANCE_KEY = "validatorProcess";
    public static final String SIP_ID = "d523cb06-6e3e-407f-9054-0ee9b191b927";

    private static ValidatorDelegate validatorDelegate;
    private static ValidationProfileStore store;
    private IngestEventStore ingestEventStore = new IngestEventStore();
    private ToolStore toolStore = new ToolStore();
    private IngestWorkflowStore ingestWorkflowStore = new IngestWorkflowStore();
    private ToolService toolService = new ToolService();
    @Mock
    private Generator generator;

    @BeforeClass
    public static void beforeClass() {
        validatorDelegate = new ValidatorDelegate();
        validatorDelegate.setObjectMapper(new ObjectMapper());
        validatorDelegate.setWorkspace(WS.toString());
        Mocks.register("validatorDelegate", validatorDelegate);
    }

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        when(generator.generate(any())).thenReturn(String.valueOf(new Random().nextInt()));
        Validator validator = new Validator();
        store = new ValidationProfileStore();
        store.setGenerator(generator);

        initializeStores(store, ingestEventStore, ingestWorkflowStore, toolStore);
        validatorDelegate.setService(validator);
        validator.setValidationProfileStore(store);

        toolService.setToolStore(toolStore);
        validatorDelegate.setIngestEventStore(ingestEventStore);
        IngestWorkflowService ingestWorkflowService = new IngestWorkflowService();
        ingestWorkflowService.setStore(ingestWorkflowStore);
        validatorDelegate.setIngestWorkflowService(ingestWorkflowService);
        validatorDelegate.setToolService(toolService);

        Tool t = new Tool();
        t.setVersion("1.0");
        t.setName("ARCLib_" + IngestToolFunction.validation);
        toolService.save(t);

        IngestWorkflow iw = new IngestWorkflow(INGEST_WORKFLOW_ID);
        iw.setExternalId(EXTERNAL_ID);
        ingestWorkflowStore.save(iw);
    }

    @Test
    public void testOK() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/validation/validationProfileMixedChecks.xml");
        String xml = readFromInputStream(inputStream);

        ValidationProfile validationProfile = new ValidationProfile();
        validationProfile.setXml(xml);

        store.save(validationProfile);
        flushCache();

        Map variables = new HashMap();
        variables.put(BpmConstants.ProcessVariables.ingestWorkflowId, INGEST_WORKFLOW_ID);
        variables.put(BpmConstants.ProcessVariables.ingestWorkflowExternalId, EXTERNAL_ID);
        variables.put(BpmConstants.ProcessVariables.sipId, SIP_ID);
        variables.put(BpmConstants.ProcessVariables.latestConfig, String.format("{\"%s\":\"%s\"}", ValidatorDelegate.VALIDATION_PROFILE_CONFIG_ENTRY, validationProfile.getExternalId()));
        variables.put(BpmConstants.Ingestion.sipFileName, ORIGINAL_SIP_FILE_NAME);
        variables.put(BpmConstants.ProcessVariables.responsiblePerson, "user");
        variables.put(BpmConstants.ProcessVariables.sipFolderWorkspacePath, SIP.toAbsolutePath().toString());

        startJob(PROCESS_INSTANCE_KEY, variables);
    }

    @Test
    public void testFailure() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/validation/validationProfileMissingFile.xml");
        String xml = readFromInputStream(inputStream);

        ValidationProfile validationProfile = new ValidationProfile();
        validationProfile.setXml(xml);

        store.save(validationProfile);
        flushCache();

        Map variables = new HashMap();
        variables.put(BpmConstants.ProcessVariables.ingestWorkflowId, INGEST_WORKFLOW_ID);
        variables.put(BpmConstants.ProcessVariables.ingestWorkflowExternalId, EXTERNAL_ID);
        variables.put(BpmConstants.ProcessVariables.latestConfig, String.format("{\"%s\":\"%s\"}", ValidatorDelegate.VALIDATION_PROFILE_CONFIG_ENTRY, validationProfile.getExternalId()));
        variables.put(BpmConstants.ProcessVariables.sipId, SIP_ID);
        variables.put(BpmConstants.ProcessVariables.responsiblePerson, "user");
        variables.put(BpmConstants.Ingestion.sipFileName, ORIGINAL_SIP_FILE_NAME);
        variables.put(BpmConstants.ProcessVariables.sipFolderWorkspacePath, SIP.toString());

        assertThrown(() -> startJob(PROCESS_INSTANCE_KEY, variables)).isInstanceOf(MissingFile.class);
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
