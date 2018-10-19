package cz.cas.lib.arclib.bpm.error;

import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.domain.Batch;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.packages.AuthorialPackage;
import cz.cas.lib.arclib.domain.packages.Sip;
import cz.cas.lib.arclib.dto.IncidentInfoDto;
import cz.cas.lib.arclib.service.IngestWorkflowService;
import cz.cas.lib.arclib.service.incident.IncidentService;
import cz.cas.lib.arclib.service.incident.IncidentSortField;
import cz.cas.lib.arclib.store.AuthorialPackageUpdateLockStore;
import cz.cas.lib.arclib.store.IngestWorkflowStore;
import cz.cas.lib.core.exception.MissingObject;
import cz.cas.lib.core.index.dto.Order;
import cz.cas.lib.core.store.Transactional;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.repository.Deployment;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cz.cas.lib.core.util.Utils.asList;
import static cz.cas.lib.core.util.Utils.asSet;
import static helper.ThrowableAssertion.assertThrown;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
/**
 * Tests incidents service with bpmn.
 * <p>
 *     todo: speed up test by removing springboot and using camunda in-memory possibilities, find out how to properly set jobExecutorActive property
 * </p>
 */
public class IncidentServiceTest {

    private static final String CONFIG_OK = "{}";
    private static final String CONFIG_EX_INCIDENT = "{\"throw\":\"incident\"}";
    private static final String PROCESS_NAME = "process";
    private static final String BATCH_ID = "batchId";
    private static final String INGEST_WORKFLOW_ID = "ingestWorkflowId";
    private static final String EXTERNAL_ID = "ARCLIB_000000003";
    private static final String EXTERNAL_ID_2 = "ARCLIB_000000004";
    private static final String ACTIVITY_ID_FST = "process1";
    private static final String ACTIVITY_ID_SND = "process2";

    @Inject
    private RepositoryService repositoryService;

    @Inject
    private RuntimeService runtimeService;

    @Inject
    private IncidentService incidentService;

    @Inject
    private HistoryService historyService;

    @Inject
    private DataSource dataSource;

    @Mock
    private IngestWorkflowStore ingestWorkflowStore;

    @Mock
    private IngestWorkflowService ingestWorkflowService;

    @Mock
    private AuthorialPackageUpdateLockStore authorialPackageUpdateLockStore;

    @Mock
    private JmsTemplate template;

    @Before
    @Transactional
    public void testSetUp() throws Exception {
        incidentService.getIngestErrorHandler().setIngestWorkflowService(ingestWorkflowService);
        incidentService.getIngestErrorHandler().setTemplate(template);

        Sip sip = new Sip();
        AuthorialPackage authorialPackage = new AuthorialPackage();
        sip.setAuthorialPackage(authorialPackage);

        IngestWorkflow ingestWorkflow = new IngestWorkflow();
        Batch batch = new Batch();
        ingestWorkflow.setBatch(batch);
        ingestWorkflow.setSip(sip);

        IngestWorkflow ingestWorkflow2 = new IngestWorkflow();
        ingestWorkflow2.setBatch(batch);
        ingestWorkflow2.setSip(sip);

        when(ingestWorkflowService.findByExternalId(EXTERNAL_ID)).thenReturn(ingestWorkflow);
        when(ingestWorkflowService.findByExternalId(EXTERNAL_ID_2)).thenReturn(ingestWorkflow2);
        when(authorialPackageUpdateLockStore.findByAuthorialPackageId(anyString())).thenReturn(null);
        when(ingestWorkflowService.getDelegate()).thenReturn(ingestWorkflowStore);


        byte[] bpmn = Files.readAllBytes(Paths.get("src/test/resources/bpmn/incidentTest.bpmn"));
        repositoryService.createDeployment().addInputStream("incidentTest.bpmn", new ByteArrayInputStream(bpmn)).deploy();
    }

    @After
    public void testTearDown() {
        for (Deployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    public void noIncident() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        vars.put(BpmConstants.ProcessVariables.latestConfig, CONFIG_OK);
        vars.put(BpmConstants.ProcessVariables.batchId, BATCH_ID);
        runtimeService.startProcessInstanceByKey(PROCESS_NAME, vars).getId();
        snooze();

        List<IncidentInfoDto> incidents = incidentService.getIncidentsOfBatch(BATCH_ID, IncidentSortField.TIMESTAMP, Order.DESC);
        assertThat(incidents, empty());
    }

    @Test
    public void incidentDoesNotExist() {
        assertThrown(() -> incidentService.cancelIncidents(asList("blah"), CONFIG_OK))
                .isInstanceOf(MissingObject.class);
    }

    @Test
    public void incidentSolved() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        vars.put(BpmConstants.ProcessVariables.latestConfig, CONFIG_EX_INCIDENT);
        vars.put(BpmConstants.ProcessVariables.batchId, BATCH_ID);
        runtimeService.startProcessInstanceByKey(PROCESS_NAME, vars).getId();
        snooze();

        List<IncidentInfoDto> incidents = incidentService.getIncidentsOfBatch(BATCH_ID, IncidentSortField.TIMESTAMP, Order.DESC);
        assertThat(incidents, hasSize(1));
        incidentService.solveIncidents(asList(incidents.get(0).getId()), CONFIG_OK);
        sleep();

        List<IncidentInfoDto> incidents2 = incidentService.getIncidentsOfBatch(BATCH_ID, IncidentSortField.TIMESTAMP, Order.DESC);
        assertThat(incidents2, empty());
    }

    @Test
    public void incidentNotSolved() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        vars.put(BpmConstants.ProcessVariables.batchId, BATCH_ID);
        vars.put(BpmConstants.ProcessVariables.latestConfig, CONFIG_EX_INCIDENT);
        runtimeService.startProcessInstanceByKey(PROCESS_NAME, vars);
        snooze();

        List<IncidentInfoDto> incidents = incidentService.getIncidentsOfBatch(BATCH_ID, IncidentSortField.TIMESTAMP, Order.DESC);
        assertThat(incidents, hasSize(1));
        IncidentInfoDto fstIncident = incidents.get(0);
        incidentService.solveIncidents(asList(fstIncident.getId()), CONFIG_EX_INCIDENT);
        sleep();

        incidents = incidentService.getIncidentsOfBatch(BATCH_ID, IncidentSortField.TIMESTAMP, Order.DESC);
        assertThat(incidents, hasSize(1));
        assertThat(incidents.get(0), not(is(fstIncident)));
    }

    @Test
    public void incidentsByExternalId() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        vars.put(BpmConstants.ProcessVariables.batchId, BATCH_ID);
        vars.put(BpmConstants.ProcessVariables.ingestWorkflowId, INGEST_WORKFLOW_ID);
        vars.put(BpmConstants.ProcessVariables.ingestWorkflowExternalId, EXTERNAL_ID);
        vars.put(BpmConstants.ProcessVariables.latestConfig, CONFIG_EX_INCIDENT);
        runtimeService.startProcessInstanceByKey(PROCESS_NAME, vars);
        Map<String, Object> vars2 = new HashMap<>();
        vars2.put(BpmConstants.ProcessVariables.batchId, BATCH_ID);
        vars2.put(BpmConstants.ProcessVariables.ingestWorkflowId, "other");
        vars2.put(BpmConstants.ProcessVariables.latestConfig, CONFIG_EX_INCIDENT);
        runtimeService.startProcessInstanceByKey(PROCESS_NAME, vars2);
        snooze();

        List<IncidentInfoDto> incidents = incidentService.getIncidentsOfIngestWorkflow(EXTERNAL_ID);
        assertThat(incidents, hasSize(1));
        IncidentInfoDto fstIncident = incidents.get(0);
        incidentService.solveIncidents(asList(fstIncident.getId()), CONFIG_EX_INCIDENT);
        sleep();

        incidents = incidentService.getIncidentsOfIngestWorkflow(EXTERNAL_ID);
        assertThat(incidents, hasSize(2));
        IncidentInfoDto sndIncident = incidents.get(1);
        assertThat(sndIncident, not(is(fstIncident)));
        assertThat(incidents.get(0), is(fstIncident));
        assertThat(sndIncident.getExternalId(), is(EXTERNAL_ID));
    }

    @Test
    public void multipleIncidentsHalfSolved() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        vars.put(BpmConstants.ProcessVariables.latestConfig, CONFIG_EX_INCIDENT);
        vars.put(BpmConstants.ProcessVariables.batchId, BATCH_ID);
        runtimeService.startProcessInstanceByKey(PROCESS_NAME, vars);

        Map<String, Object> vars2 = new HashMap<>();
        vars2.put(BpmConstants.ProcessVariables.latestConfig, CONFIG_EX_INCIDENT);
        vars2.put(BpmConstants.ProcessVariables.batchId, BATCH_ID);
        vars2.put(ACTIVITY_ID_SND, "incident");
        runtimeService.startProcessInstanceByKey(PROCESS_NAME, vars2);
        snooze();

        List<IncidentInfoDto> incidents = incidentService.getIncidentsOfBatch(BATCH_ID, IncidentSortField.TIMESTAMP, Order.DESC);
        assertThat(incidents, hasSize(2));
        incidentService.solveIncidents(incidents.stream().map(IncidentInfoDto::getId).collect(Collectors.toList()), CONFIG_OK);
        sleep();

        incidents = incidentService.getIncidentsOfBatch(null, IncidentSortField.TIMESTAMP, Order.DESC);
        assertThat(incidents, hasSize(1));
    }

    @Test
    public void multipleIncidentsAdminSolvesAll() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        vars.put(BpmConstants.ProcessVariables.latestConfig, CONFIG_EX_INCIDENT);
        vars.put(BpmConstants.ProcessVariables.batchId, BATCH_ID);
        runtimeService.startProcessInstanceByKey(PROCESS_NAME, vars);

        Map<String, Object> vars2 = new HashMap<>();
        vars2.put(BpmConstants.ProcessVariables.latestConfig, CONFIG_EX_INCIDENT);
        vars2.put(BpmConstants.ProcessVariables.batchId, BATCH_ID);
        runtimeService.startProcessInstanceByKey(PROCESS_NAME, vars2);
        snooze();

        List<IncidentInfoDto> incidents = incidentService.getIncidentsOfBatch(null, IncidentSortField.TIMESTAMP, Order.DESC);
        assertThat(incidents, hasSize(2));

        incidentService.solveIncidents(incidents.stream().map(IncidentInfoDto::getId).collect(Collectors.toList()), CONFIG_OK);
        sleep();

        incidents = incidentService.getIncidentsOfBatch(BATCH_ID, IncidentSortField.TIMESTAMP, Order.DESC);
        assertThat(incidents, empty());
    }

    @Test
    public void multipleIncidentsAdminCancelsAll() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        vars.put(BpmConstants.ProcessVariables.latestConfig, CONFIG_EX_INCIDENT);
        vars.put(BpmConstants.ProcessVariables.batchId, BATCH_ID);
        vars.put(BpmConstants.ProcessVariables.ingestWorkflowExternalId, EXTERNAL_ID);

        String processInstanceId1 = runtimeService.startProcessInstanceByKey(PROCESS_NAME, vars).getId();

        Map<String, Object> vars2 = new HashMap<>();
        vars2.put(BpmConstants.ProcessVariables.latestConfig, CONFIG_EX_INCIDENT);
        vars2.put(BpmConstants.ProcessVariables.batchId, BATCH_ID);
        vars2.put(BpmConstants.ProcessVariables.ingestWorkflowExternalId, EXTERNAL_ID_2);
        String processInstanceId2 = runtimeService.startProcessInstanceByKey(PROCESS_NAME, vars2).getId();
        snooze();

        List<IncidentInfoDto> incidents = incidentService.getIncidentsOfBatch(BATCH_ID, IncidentSortField.TIMESTAMP, Order.DESC);
        assertThat(incidents, hasSize(2));

        incidentService.cancelIncidents(incidents.stream().map(IncidentInfoDto::getId).collect(Collectors.toList()), "cancelled by admin");
        sleep();

        incidents = incidentService.getIncidentsOfBatch(BATCH_ID, IncidentSortField.TIMESTAMP, Order.DESC);
        assertThat(incidents, empty());

        List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery().processInstanceIds(asSet(processInstanceId1, processInstanceId2)).list();
        assertThat(processInstances.get(0).getState(), is("EXTERNALLY_TERMINATED"));
        assertThat(processInstances.get(1).getState(), is("EXTERNALLY_TERMINATED"));
    }

    @Test
    public void incidentFullDto() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        vars.put(BpmConstants.ProcessVariables.assignee, "user");
        vars.put(BpmConstants.ProcessVariables.batchId, BATCH_ID);
        vars.put(BpmConstants.ProcessVariables.ingestWorkflowId, "ingestWorkflowId");
        vars.put(BpmConstants.ProcessVariables.ingestWorkflowExternalId, EXTERNAL_ID);
        vars.put(BpmConstants.ProcessVariables.latestConfig, CONFIG_EX_INCIDENT);
        Instant firedAt = Instant.now();
        String processInstanceId = runtimeService.startProcessInstanceByKey(PROCESS_NAME, vars).getId();
        snooze();

        IncidentInfoDto fstIncident = incidentService.getIncidentsOfBatch(BATCH_ID, IncidentSortField.TIMESTAMP, Order.DESC).get(0);
        assertThat(fstIncident.getActivityId(), is(ACTIVITY_ID_FST));
        assertThat(fstIncident.getAssignee(), is("user"));
        assertThat(fstIncident.getBatchId(), is(BATCH_ID));
        assertThat(fstIncident.getExternalId(), is(EXTERNAL_ID));
        assertThat(fstIncident.getConfig(), is(CONFIG_EX_INCIDENT));
        assertThat(fstIncident.getProcessInstanceId(), is(processInstanceId));
        assertThat(fstIncident.getCreated(), greaterThan(firedAt));
        assertThat(fstIncident.getMessage(), notNullValue());
        assertThat(fstIncident.getStackTrace(), containsString("org.camunda.bpm.engine"));

        incidentService.solveIncidents(asList(fstIncident.getId()), CONFIG_EX_INCIDENT);
        sleep();

        IncidentInfoDto sndIncident = incidentService.getIncidentsOfBatch(BATCH_ID, IncidentSortField.TIMESTAMP, Order.DESC).get(0);
        assertThat(sndIncident.getCreated(), greaterThan(fstIncident.getCreated()));
    }

    @Test
    public void incidentsOrdering() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        vars.put(ACTIVITY_ID_FST, "incident");
        vars.put(BpmConstants.ProcessVariables.latestConfig, CONFIG_OK);
        vars.put(BpmConstants.ProcessVariables.batchId, BATCH_ID);
        Map<String, Object> vars2 = new HashMap<>();
        vars2.put(BpmConstants.ProcessVariables.latestConfig, CONFIG_OK);
        vars2.put(ACTIVITY_ID_SND, "incident");
        vars2.put(BpmConstants.ProcessVariables.batchId, BATCH_ID);

        runtimeService.startProcessInstanceByKey(PROCESS_NAME, vars);
        runtimeService.startProcessInstanceByKey(PROCESS_NAME, vars2);
        runtimeService.startProcessInstanceByKey(PROCESS_NAME, vars);
        runtimeService.startProcessInstanceByKey(PROCESS_NAME, vars2);
        snooze();

        List<IncidentInfoDto> timestampDesc = incidentService.getIncidentsOfBatch(null, IncidentSortField.TIMESTAMP, Order.DESC);
        List<IncidentInfoDto> timestampAsc = incidentService.getIncidentsOfBatch(null, IncidentSortField.TIMESTAMP, Order.ASC);
        assertThat(timestampAsc, contains(timestampDesc.get(3), timestampDesc.get(2), timestampDesc.get(1), timestampDesc.get(0)));

        List<IncidentInfoDto> activityDesc = incidentService.getIncidentsOfBatch(null, IncidentSortField.ACTIVITY, Order.DESC);
        assertThat(activityDesc, hasSize(4));
        assertThat(activityDesc.get(0).getActivityId(), endsWith("2"));
        assertThat(activityDesc.get(1).getActivityId(), endsWith("2"));
        assertThat(activityDesc.get(2).getActivityId(), endsWith("1"));
        assertThat(activityDesc.get(3).getActivityId(), endsWith("1"));

        List<IncidentInfoDto> activityAsc = incidentService.getIncidentsOfBatch(null, IncidentSortField.ACTIVITY, Order.ASC);
        assertThat(activityAsc, hasSize(4));
        assertThat(activityAsc.get(0).getActivityId(), endsWith("1"));
        assertThat(activityAsc.get(1).getActivityId(), endsWith("1"));
        assertThat(activityAsc.get(2).getActivityId(), endsWith("2"));
        assertThat(activityAsc.get(3).getActivityId(), endsWith("2"));
    }

    @Test
    public void incidentsFiltering() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        vars.put(BpmConstants.ProcessVariables.latestConfig, CONFIG_EX_INCIDENT);
        vars.put(BpmConstants.ProcessVariables.batchId, BATCH_ID);
        runtimeService.startProcessInstanceByKey(PROCESS_NAME, vars).getId();

        Map<String, Object> vars2 = new HashMap<>();
        vars2.put(BpmConstants.ProcessVariables.latestConfig, CONFIG_EX_INCIDENT);
        vars2.put(BpmConstants.ProcessVariables.batchId, "otherBatch");
        runtimeService.startProcessInstanceByKey(PROCESS_NAME, vars2).getId();
        snooze();

        List<IncidentInfoDto> incidents = incidentService.getIncidentsOfBatch(BATCH_ID, IncidentSortField.TIMESTAMP, Order.DESC);
        assertThat(incidents, hasSize(1));

        incidents = incidentService.getIncidentsOfBatch("otherBatch", IncidentSortField.TIMESTAMP, Order.DESC);
        assertThat(incidents, hasSize(1));

        incidents = incidentService.getIncidentsOfBatch(null, IncidentSortField.TIMESTAMP, Order.DESC);
        assertThat(incidents, hasSize(2));

        incidents = incidentService.getIncidentsOfBatch(BATCH_ID, IncidentSortField.TIMESTAMP, Order.DESC);
        assertThat(incidents, hasSize(1));
    }

    /**
     * used to wait for incident, between process instance start and incident service calls
     *
     * @throws InterruptedException
     */
    private void snooze() throws InterruptedException {
        Thread.sleep(500);
    }

    /**
     * used after {@link IncidentService#solveIncidents(List, String)} to wait for camunda job executor thread
     *
     * @throws InterruptedException
     */
    private void sleep() throws InterruptedException {
        Thread.sleep(5000);
    }
}
