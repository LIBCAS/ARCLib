package cz.cas.lib.arclib.service;

import cz.inqool.uas.exception.GeneralException;
import cz.inqool.uas.exception.MissingObject;
import helper.ApiTest;
import helper.ThrowableAssertion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;


@RunWith(SpringRunner.class)
@SpringBootTest
public class CoordinatorServiceTest implements ApiTest {
    @Inject
    private CoordinatorService service;

    /**
     * Test of ({@link CoordinatorService#start(String)}) method. The test asserts that a GeneralException is thrown when a non existent path
     * is provided.
     */
    @Test
    public void startTestNonExistentPath() {
        ThrowableAssertion.assertThrown(() -> service.start("/nonExistentPath"))
                .isInstanceOf(GeneralException.class);
    }

    /**
     * Test of ({@link CoordinatorService#suspend(String)}) method. It will assert that a MissingObject exception is thrown
     * when ID of a non existent batch is provided.
     */
    @Test
    public void suspendNonExistentBatchTest() {
        ThrowableAssertion.assertThrown(() -> service.suspend("#%#$")).isInstanceOf(MissingObject.class);
    }

    /**
     * Test of ({@link CoordinatorService#resume(String}) method. It will assert that a MissingObject exception is thrown
     * when ID of a non existent batch is provided.
     */
    @Test
    public void resumeTestNonExistentBatch() {
        ThrowableAssertion.assertThrown(() -> service.resume("#%#$")).isInstanceOf(MissingObject.class);
    }
}
