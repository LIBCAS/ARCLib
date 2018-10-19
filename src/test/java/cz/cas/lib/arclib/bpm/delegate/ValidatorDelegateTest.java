package cz.cas.lib.arclib.bpm.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.bpm.ValidatorDelegate;
import cz.cas.lib.arclib.domain.profiles.ValidationProfile;
import cz.cas.lib.arclib.exception.validation.MissingFile;
import cz.cas.lib.arclib.service.validator.Validator;
import cz.cas.lib.arclib.store.ValidationProfileStore;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import static helper.ThrowableAssertion.assertThrown;

/**
 * Ensure the validator.bpmn process is working correctly
 */
@Deployment(resources = "bpmn/validator.bpmn")
public class ValidatorDelegateTest extends DelegateTest {

    private static final String INGEST_CONFIG = "{}";
    private static final String PROCESS_INSTANCE_KEY = "validatorProcess";
    public static final String SIP_ID = "d523cb06-6e3e-407f-9054-0ee9b191b927";

    private static ValidatorDelegate validatorDelegate;
    private static ValidationProfileStore store;

    @BeforeClass
    public static void beforeClass() {
        validatorDelegate = new ValidatorDelegate();
        validatorDelegate.setObjectMapper(new ObjectMapper());
        validatorDelegate.setWorkspace(WS.toString());
        Mocks.register("validatorDelegate", validatorDelegate);
    }

    @Before
    public void before() {
        Validator validator = new Validator();
        store = new ValidationProfileStore();
        store.setEntityManager(getEm());
        store.setQueryFactory(new JPAQueryFactory(getEm()));

        validator.setValidationProfileStore(store);

        validatorDelegate.setService(validator);
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
        variables.put(BpmConstants.ProcessVariables.latestConfig, INGEST_CONFIG);
        variables.put(BpmConstants.Ingestion.originalSipFileName, ORIGINAL_SIP_FILE_NAME);
        variables.put(BpmConstants.ProcessVariables.assignee, "user");
        variables.put(BpmConstants.Validation.validationProfileId, validationProfile.getId());

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
        variables.put(BpmConstants.ProcessVariables.latestConfig, INGEST_CONFIG);
        variables.put(BpmConstants.ProcessVariables.sipId, SIP_ID);
        variables.put(BpmConstants.ProcessVariables.assignee, "user");
        variables.put(BpmConstants.Ingestion.originalSipFileName, ORIGINAL_SIP_FILE_NAME);
        variables.put(BpmConstants.Validation.validationProfileId, validationProfile.getId());

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
