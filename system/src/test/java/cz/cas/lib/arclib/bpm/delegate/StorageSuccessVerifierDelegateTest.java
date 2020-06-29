package cz.cas.lib.arclib.bpm.delegate;

import cz.cas.lib.arclib.bpm.StorageSuccessVerifierDelegate;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.junit.Before;

import java.io.IOException;
import java.util.Properties;

@Deployment(resources = "bpmn/storageSuccessVerifier.bpmn")
public class StorageSuccessVerifierDelegateTest extends DelegateTest {

    private static final String PROCESS_INSTANCE_KEY = "storageSuccessVerifierProcess";
    private static Properties props = new Properties();

    private StorageSuccessVerifierDelegate storageSuccessVerifierDelegate;

    @Before
    public void before() throws IOException {
        props.load(ClassLoader.getSystemResourceAsStream("application.properties"));
        storageSuccessVerifierDelegate = new StorageSuccessVerifierDelegate();
        Mocks.register("storageSuccessVerifierDelegate", storageSuccessVerifierDelegate);
    }
}
