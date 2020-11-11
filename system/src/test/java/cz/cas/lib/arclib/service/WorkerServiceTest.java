package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.Batch;
import cz.cas.lib.arclib.domain.BatchState;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflowState;
import cz.cas.lib.arclib.dto.JmsDto;
import cz.cas.lib.arclib.store.BatchStore;
import cz.cas.lib.arclib.store.IngestWorkflowStore;
import cz.cas.lib.arclib.utils.ArclibUtils;
import cz.cas.lib.core.store.Transactional;
import helper.TransformerFactoryWorkaroundTest;
import org.apache.commons.io.IOUtils;
import org.camunda.bpm.engine.RepositoryService;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.nio.file.Paths;
import java.sql.SQLException;

import static cz.cas.lib.core.util.Utils.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class WorkerServiceTest extends TransformerFactoryWorkaroundTest {

    @Inject
    private BatchStore batchStore;

    @Inject
    private WorkerService service;

    @Inject
    private IngestWorkflowStore ingestWorkflowStore;

    @Inject
    private JmsTemplate template;

    @Inject
    private RepositoryService repositoryService;

    @Before
    public void before() {
        System.setProperty("javax.xml.transform.TransformerFactory", "cz.cas.lib.arclib.config.ArclibTransformerFactory");
    }

    @After
    public void testTearDown() throws SQLException {
        ingestWorkflowStore.findAll().forEach(sip -> ingestWorkflowStore.delete(sip));
        batchStore.findAll().forEach(batch -> batchStore.delete(batch));
    }

    /**
     * Test of ({@link WorkerService#startProcessingOfIngestWorkflow(JmsDto)}) method. First, there is a batch created that:
     * 1. is in the processingState PROCESSING
     * 2. has one SIP package in processingState NEW
     * Then the startProcessingOfIngestWorkflow method is called on the given SIP.
     * <p>
     * The test asserts that in the end the SIP package is in the processingState PROCESSED.
     */
    //TODO prerobit
    @Ignore
    @Test
    public void processSipTest() throws Exception {
        IngestWorkflow ingestWorkflow = new IngestWorkflow();
        ingestWorkflow.setProcessingState(IngestWorkflowState.NEW);
        ingestWorkflowStore.save(ingestWorkflow);

        Batch batch = new Batch();
        batch.setState(BatchState.PROCESSING);
        batch.setIngestWorkflows(asList(ingestWorkflow));
        prepareDeployment(batch);
        batchStore.save(batch);

        service.startProcessingOfIngestWorkflow(new JmsDto(ingestWorkflow.getExternalId(), "user"));

        /*
        wait until all the JMS communication is finished and the proper data is stored in DB
        */
        Thread.sleep(6000);

        ingestWorkflow = ingestWorkflowStore.find(ingestWorkflow.getExternalId());

        assertThat(ingestWorkflow.getProcessingState(), is(IngestWorkflowState.PROCESSED));
    }

    /**
     * Test of ({@link WorkerService#startProcessingOfIngestWorkflow(JmsDto)}) method. First, there is a batch created that:
     * 1. is in the processingState SUSPENDED
     * 2. has one SIP package in processingState NEW
     * Then the startProcessingOfIngestWorkflow method is called on the given SIP.
     * <p>
     * The test asserts that in the end the SIP package is still in the processingState NEW. The SIP remained unprocessed because the respective batch
     * was in the state SUSPENDED.
     */
    //TODO prerobit
    @Ignore
    @Transactional
    @Test
    public void processSipTestBatchStateSuspended() throws Exception {
        IngestWorkflow ingestWorkflow = new IngestWorkflow();
        ingestWorkflow.setProcessingState(IngestWorkflowState.NEW);
        ingestWorkflowStore.save(ingestWorkflow);

        Batch batch = new Batch();
        batch.setState(BatchState.SUSPENDED);
        batch.setIngestWorkflows(asList(ingestWorkflow));
        prepareDeployment(batch);
        batchStore.save(batch);

        service.startProcessingOfIngestWorkflow(new JmsDto(ingestWorkflow.getExternalId(), "user"));

        /*
        wait until all the JMS communication is finished and the proper data is stored in DB
        */
        Thread.sleep(6000);

        ingestWorkflow = ingestWorkflowStore.findByExternalId(ingestWorkflow.getExternalId());

        assertThat(ingestWorkflow.getProcessingState(), is(IngestWorkflowState.NEW));
    }

    /**
     * Test of ({@link WorkerService#startProcessingOfIngestWorkflow(JmsDto)}) method. First, there is a batch created that:
     * 1. is in the state CANCELED
     * 2. has one SIP package in processingState NEW
     * Then the startProcessingOfIngestWorkflow method is called on the given SIP.
     * <p>
     * The test asserts that in the end the SIP package is still in the processingState NEW. The SIP remained unprocessed because the respective batch
     * was in the state CANCELED.
     */
    //TODO prerobit
    @Ignore
    @Test
    public void processSipTestBatchStateCanceled() throws Exception {
        IngestWorkflow ingestWorkflow = new IngestWorkflow();
        ingestWorkflow.setProcessingState(IngestWorkflowState.NEW);
        ingestWorkflowStore.save(ingestWorkflow);

        Batch batch = new Batch();
        batch.setState(BatchState.CANCELED);
        batch.setIngestWorkflows(asList(ingestWorkflow));
        prepareDeployment(batch);
        batchStore.save(batch);

        service.startProcessingOfIngestWorkflow(new JmsDto(ingestWorkflow.getExternalId(), "user"));

        /*
        wait until all the JMS communication is finished and the proper data is stored in DB
        */
        Thread.sleep(6000);

        ingestWorkflow = ingestWorkflowStore.findByExternalId(ingestWorkflow.getExternalId());

        assertThat(ingestWorkflow.getProcessingState(), is(IngestWorkflowState.NEW));
    }

    /**
     * Test of ({@link WorkerService#startProcessingOfIngestWorkflow(JmsDto)} method. The test asserts that on the processing of SIP package the
     * respective batch is not canceled when only half of the batch SIP packages have processingState FAILED.
     */
    //TODO prerobit
    @Ignore
    @Test
    @Transactional
    public void stopAtMultipleFailuresTestHalfPackagesFailed() throws Exception {
        IngestWorkflow ingestWorkflow = new IngestWorkflow();
        ingestWorkflow.setProcessingState(IngestWorkflowState.FAILED);
        ingestWorkflowStore.save(ingestWorkflow);

        IngestWorkflow ingestWorkflow2 = new IngestWorkflow();
        ingestWorkflow2.setProcessingState(IngestWorkflowState.NEW);
        ingestWorkflowStore.save(ingestWorkflow2);

        Batch batch = new Batch();
        batch.setState(BatchState.PROCESSING);
        batch.setIngestWorkflows(asList(ingestWorkflow, ingestWorkflow2));
        prepareDeployment(batch);
        batchStore.save(batch);

        service.startProcessingOfIngestWorkflow(new JmsDto(ingestWorkflow2.getId(), "user"));

        /*
        wait until all the JMS communication is finished and the proper data is stored in DB
        */
        Thread.sleep(2000);

        batch = batchStore.find(batch.getId());
        assertThat(batch.getState(), is(BatchState.PROCESSED));
    }

    /**
     * Test of ({@link WorkerService#startProcessingOfIngestWorkflow(JmsDto)} method. The test asserts that on the processing of SIP package the
     * respective batch is canceled when more than half of the batch SIP packages have processingState FAILED.
     */
    //TODO prerobit
    @Ignore
    @Test
    public void stopAtMultipleFailuresTestMoreThanHalfPackagesFailed() throws Exception {
        IngestWorkflow ingestWorkflow = new IngestWorkflow();
        ingestWorkflow.setProcessingState(IngestWorkflowState.PROCESSING);
        ingestWorkflowStore.save(ingestWorkflow);

        IngestWorkflow ingestWorkflow2 = new IngestWorkflow();
        ingestWorkflow2.setProcessingState(IngestWorkflowState.FAILED);
        ingestWorkflowStore.save(ingestWorkflow2);

        IngestWorkflow ingestWorkflow3 = new IngestWorkflow();
        ingestWorkflow3.setProcessingState(IngestWorkflowState.FAILED);
        ingestWorkflowStore.save(ingestWorkflow3);

        Batch batch = new Batch();
        batch.setState(BatchState.PROCESSING);
        batch.setIngestWorkflows(asList(ingestWorkflow, ingestWorkflow2, ingestWorkflow3));
        prepareDeployment(batch);
        batchStore.save(batch);

        service.startProcessingOfIngestWorkflow(new JmsDto(ingestWorkflow3.getId(), "user"));

        /*
        wait until all the JMS communication is finished and the proper data is stored in DB
        */
        Thread.sleep(2000);

        batch = batchStore.find(batch.getId());
        assertThat(batch.getState(), is(BatchState.CANCELED));
    }

    private void prepareDeployment(Batch batch) throws Exception {
        try (FileInputStream fos = new FileInputStream(Paths.get("system/src/main/resources/bpmn/ingest.bpmn").toFile())) {
            String bpmnString = ArclibUtils.prepareBpmnDefinitionForDeployment(IOUtils.toString(fos), batch.getId());
            repositoryService.createDeployment().addInputStream(batch.getId() + ".bpmn",
                    new ByteArrayInputStream(bpmnString.getBytes())).name(batch.getId()).deploy();
        }
    }
}
