package cz.cas.lib.arclib.bpm.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.bpm.BpmErrorHandlerDelegate;
import cz.cas.lib.arclib.bpm.BpmTestConfig;
import cz.cas.lib.arclib.domain.Batch;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflowFailureInfo;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflowFailureType;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflowState;
import cz.cas.lib.arclib.domain.packages.AuthorialPackage;
import cz.cas.lib.arclib.domain.packages.Sip;
import cz.cas.lib.arclib.service.AipService;
import cz.cas.lib.arclib.service.BatchService;
import cz.cas.lib.arclib.service.IngestErrorHandler;
import cz.cas.lib.arclib.service.IngestWorkflowService;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageException;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageService;
import cz.cas.lib.arclib.service.incident.CustomIncidentHandler;
import cz.cas.lib.arclib.utils.ArclibUtils;
import cz.cas.lib.core.store.Transactional;
import helper.DbTest;
import org.apache.commons.io.FileUtils;
import org.camunda.bpm.engine.history.HistoricJobLog;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static helper.ThrowableAssertion.assertThrown;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Deployment(resources = "bpmn/errorTest.bpmn")
public class ErrorHandlingTest extends DbTest {
    public static final Path WS = Paths.get("testWorkspace");

    private static final String CONFIG_EX_INCIDENT = "{\"throw\":\"incident\"}";
    private static final String CONFIG_EX_RUNTIME = "{\"throw\":\"runtime\"}";
    private static final String CONFIG_EX_BPM = "{\"throw\":\"bpm\"}";
    private static final String BATCH_ID = "batchId";
    private static final String BPMN_KEY = "errorTest";
    private static final String EXTERNAL_ID = "ARCLIB_000000001";

    private IngestWorkflow ingestWorkflow;
    private IngestErrorHandler ingestErrorHandler = new IngestErrorHandler();
    private TransactionTemplate transactionTemplate = new TransactionTemplate();
    private BpmTestConfig bpmTestConfig = new BpmTestConfig(new CustomIncidentHandler());

    @Mock
    private IngestWorkflowService ingestWorkflowService;

    @Mock
    private AipService aipService;

    @Mock
    private BatchService batchService;

    @Mock
    private JmsTemplate template;
    @Rule
    public ProcessEngineRule rule = new ProcessEngineRule(bpmTestConfig.buildProcessEngine());

    @Mock
    private ArchivalStorageService archivalStorageService;

    @Mock
    private PlatformTransactionManager transactionManager;


    @Before
    @Transactional
    public void testSetUp() {
        MockitoAnnotations.initMocks(this);

        CustomIncidentHandler customIncidentHandler = (CustomIncidentHandler) bpmTestConfig.getCustomIncidentHandlers().get(0);
        customIncidentHandler.setManagementService(rule.getManagementService());
        customIncidentHandler.setRuntimeService(rule.getRuntimeService());
        customIncidentHandler.setIngestErrorHandler(ingestErrorHandler);
        customIncidentHandler.setBatchService(batchService);
        ingestErrorHandler.setTransactionTemplate(transactionTemplate);
        transactionTemplate.setTransactionManager(transactionManager);

        Sip sip = new Sip();
        AuthorialPackage authorialPackage = new AuthorialPackage();
        sip.setAuthorialPackage(authorialPackage);

        ingestWorkflow = new IngestWorkflow();
        ingestWorkflow.setXmlVersionNumber(1);
        ingestWorkflow.setProcessingState(IngestWorkflowState.PROCESSING);
        ingestWorkflow.setExternalId(EXTERNAL_ID);
        ingestWorkflow.setSip(sip);

        Batch b = new Batch();
        b.setId("batchId");
        ingestWorkflow.setBatch(b);
        when(ingestWorkflowService.find(eq(ingestWorkflow.getExternalId()))).thenReturn(ingestWorkflow);
        when(ingestWorkflowService.findByExternalId(eq(EXTERNAL_ID))).thenReturn(ingestWorkflow);

        BpmErrorHandlerDelegate bpmErrorHandlerDelegate = new BpmErrorHandlerDelegate();
        Mocks.register("bpmErrorHandlerDelegate", bpmErrorHandlerDelegate);
        bpmErrorHandlerDelegate.setIngestErrorHandler(ingestErrorHandler);

        ErrorThrowingDelegate errorThrowingDelegate = new ErrorThrowingDelegate();
        Mocks.register("errorThrowingDelegate", errorThrowingDelegate);
        errorThrowingDelegate.setObjectMapper(new ObjectMapper());

        ingestErrorHandler.setRuntimeService(rule.getRuntimeService());
        ingestErrorHandler.setTemplate(template);
        ingestErrorHandler.setIngestWorkflowService(ingestWorkflowService);
        ingestErrorHandler.setAipService(aipService);
        ingestErrorHandler.setArchivalStorageService(archivalStorageService);
        ingestErrorHandler.setWorkspace(WS.toString());
    }

    @Test
    public void testRuntimeErrorHandling() {
        Map<String, Object> vars = new HashMap<>();
        vars.put(BpmConstants.ProcessVariables.ingestWorkflowExternalId, EXTERNAL_ID);
        vars.put(BpmConstants.ProcessVariables.latestConfig, CONFIG_EX_RUNTIME);
        vars.put(BpmConstants.ProcessVariables.batchId, BATCH_ID);
        String processInstanceId = rule.getRuntimeService().startProcessInstanceByKey(BPMN_KEY, vars).getId();
        Job job = rule.getManagementService().createJobQuery().singleResult();
        assertThrown(() -> rule.getManagementService().executeJob(job.getId())).isInstanceOf(RuntimeException.class);

        assertThat(ingestWorkflow.getProcessingState(), is(IngestWorkflowState.FAILED));
        IngestWorkflowFailureInfo failureInfo = ingestWorkflow.getFailureInfo();
        assertThat(failureInfo, notNullValue());
        assertThat(failureInfo.getIngestWorkflowFailureType(), is(IngestWorkflowFailureType.INTERNAL_ERROR));
        assertThat(failureInfo.getStackTrace(), notNullValue());
        assertThat(failureInfo.getMsg(), containsString("internalError"));
        verify(ingestWorkflowService).save(eq(ingestWorkflow));

        assertThat(rule.getHistoryService().createHistoricIncidentQuery().processInstanceId(processInstanceId).list(), empty());
        HistoricProcessInstance historicProcessInstance = rule.getHistoryService().createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        assertThat(historicProcessInstance.getState(), is("INTERNALLY_TERMINATED"));
        assertThat(historicProcessInstance.getDeleteReason(), containsString("internalError"));
        List<HistoricJobLog> jobLogs = rule.getHistoryService().createHistoricJobLogQuery().failureLog().list();
        assertThat(jobLogs, hasSize(1));
    }

    @Test
    public void testBpmErrorHandling() throws Exception {
        when(archivalStorageService.rollbackAip(anyString())).thenThrow(new ArchivalStorageException("Connection refused: connect"));
        File iwWorkspaceFolder = ArclibUtils.getIngestWorkflowWorkspacePath(ingestWorkflow.getExternalId(), WS.toString()).toFile();
        FileUtils.copyToFile(new ByteArrayInputStream(new byte[0]), ArclibUtils.getAipXmlWorkspacePath(ingestWorkflow.getExternalId(), WS.toString()).toFile());
        FileUtils.copyToFile(new ByteArrayInputStream(new byte[0]), ArclibUtils.getSipZipWorkspacePath(ingestWorkflow.getExternalId(), WS.toString(), "aipdata").toFile());
        Map<String, Object> vars = new HashMap<>();
        vars.put(BpmConstants.ProcessVariables.ingestWorkflowExternalId, EXTERNAL_ID);
        vars.put(BpmConstants.ProcessVariables.latestConfig, CONFIG_EX_BPM);
        vars.put(BpmConstants.ProcessVariables.batchId, BATCH_ID);
        String processInstanceId = rule.getRuntimeService().startProcessInstanceByKey(BPMN_KEY, vars).getId();
        Job job = rule.getManagementService().createJobQuery().singleResult();
        assertThat(iwWorkspaceFolder.exists(), is(true));
        rule.getManagementService().executeJob(job.getId());

        assertThat(ingestWorkflow.getProcessingState(), is(IngestWorkflowState.FAILED));
        IngestWorkflowFailureInfo failureInfo = ingestWorkflow.getFailureInfo();
        assertThat(failureInfo, notNullValue());
        assertThat(failureInfo.getIngestWorkflowFailureType(), is(IngestWorkflowFailureType.BPM_ERROR));
        assertThat(failureInfo.getStackTrace(), nullValue());
        assertThat(failureInfo.getMsg(), containsString("processFailure"));
        verify(ingestWorkflowService).save(eq(ingestWorkflow));
        verify(archivalStorageService).rollbackAip(ingestWorkflow.getSip().getId());
        verify(aipService).removeAipXmlIndex(ingestWorkflow.getExternalId());
        assertThat(iwWorkspaceFolder.exists(), is(false));

        assertThat(rule.getHistoryService().createHistoricIncidentQuery().processInstanceId(processInstanceId).list(), empty());
        HistoricProcessInstance historicProcessInstance = rule.getHistoryService().createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        assertThat(historicProcessInstance.getState(), is("INTERNALLY_TERMINATED"));
        assertThat(historicProcessInstance.getDeleteReason(), containsString("processFailure"));
        List<HistoricJobLog> jobLogs = rule.getHistoryService().createHistoricJobLogQuery().failureLog().list();
        assertThat(jobLogs, empty());
    }

    @Test
    public void testIncidentErrorHandling() {
        Map<String, Object> vars = new HashMap<>();
        vars.put(BpmConstants.ProcessVariables.ingestWorkflowExternalId, EXTERNAL_ID);
        vars.put(BpmConstants.ProcessVariables.latestConfig, CONFIG_EX_INCIDENT);
        vars.put(BpmConstants.ProcessVariables.batchId, BATCH_ID);
        Batch b = new Batch();
        b.setId(BATCH_ID);
        when(batchService.find(BATCH_ID)).thenReturn(b);
        String processInstanceId = rule.getRuntimeService().startProcessInstanceByKey(BPMN_KEY, vars).getId();
        Job job = rule.getManagementService().createJobQuery().singleResult();

        assertThrown(() -> rule.getManagementService().executeJob(job.getId())).isInstanceOf(Exception.class);
        List<HistoricJobLog> jobLogs = rule.getHistoryService().createHistoricJobLogQuery().failureLog().list();
        assertThat(jobLogs, hasSize(1));
        assertThat(ingestWorkflow.getProcessingState(), is(IngestWorkflowState.PROCESSING));
        assertThat(rule.getHistoryService().createHistoricIncidentQuery().processInstanceId(processInstanceId).list(), hasSize(1));
        assertThat(b.isPendingIncidents(), is(true));
    }
}
