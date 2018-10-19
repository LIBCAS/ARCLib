package cz.cas.lib.arclib.service.incident;

import com.google.common.collect.Lists;
import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflowFailureInfo;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflowFailureType;
import cz.cas.lib.arclib.dto.IncidentInfoDto;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import cz.cas.lib.arclib.service.IngestErrorHandler;
import cz.cas.lib.arclib.service.SolveIncidentDto;
import cz.cas.lib.core.exception.GeneralException;
import cz.cas.lib.core.exception.MissingObject;
import cz.cas.lib.core.index.dto.Order;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.*;
import org.camunda.bpm.engine.runtime.Incident;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cz.cas.lib.arclib.utils.ArclibUtils.toIncidentConfigVariableName;
import static cz.cas.lib.core.util.Utils.notNull;
import static java.util.stream.Collectors.*;

/**
 * Incident manager.
 */
@Slf4j
@ConditionalOnProperty(prefix = "bpm", name = "enabled", havingValue = "true")
@Service
public class IncidentService {

    private ManagementService managementService;
    private RuntimeService runtimeService;
    private HistoryService historyService;
    private JmsTemplate template;
    @Getter
    private IngestErrorHandler ingestErrorHandler;

    /**
     * Gets incidents of active processes instances of a batch.
     *
     * @param batchId id of batch, if null returns incidents for all batches
     * @param sort    field used for sorting
     * @param order   sorting order
     * @return Sorted {@link List}
     */
    public List<IncidentInfoDto> getIncidentsOfBatch(String batchId, IncidentSortField sort, Order order) {
        List<IncidentInfoDto> incidents = new ArrayList<>();
        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().active();

        if (batchId != null && !batchId.isEmpty())
            query.variableValueEquals(BpmConstants.ProcessVariables.batchId, batchId);

        List<ProcessInstance> processInstances = query.list();

        processInstances.forEach(pi -> {
                    Incident incident = runtimeService
                            .createIncidentQuery()
                            .processInstanceId(pi.getProcessInstanceId())
                            .singleResult();
                    if (incident == null)
                        return;
                    String stackTrace = managementService.getJobExceptionStacktrace(incident.getConfiguration());

                    Map<String, Object> variables = runtimeService.getVariables(incident.getExecutionId());
                    incidents.add(
                            new IncidentInfoDto(
                                    incident.getId(),
                                    incident.getIncidentTimestamp().toInstant(),
                                    null,
                                    incident.getIncidentMessage(),
                                    stackTrace,
                                    incident.getActivityId(),
                                    (String) variables.get(BpmConstants.ProcessVariables.batchId),
                                    (String) variables.get(BpmConstants.ProcessVariables.ingestWorkflowExternalId),
                                    (String) variables.get(BpmConstants.ProcessVariables.assignee),
                                    (String) variables.get(BpmConstants.ProcessVariables.latestConfig),
                                    incident.getProcessInstanceId()
                            )
                    );
                }
        );
        Collator collator = Collator.getInstance();
        switch (sort) {
            case MESSAGE:
                incidents.sort((fst, snd) -> collator.compare(fst.getMessage(), snd.getMessage()));
                break;
            case ACTIVITY:
                incidents.sort((fst, snd) -> collator.compare(fst.getActivityId(), snd.getActivityId()));
                break;
            case BATCH:
                incidents.sort((fst, snd) -> collator.compare(fst.getBatchId(), snd.getBatchId()));
                break;
            case ASSIGNEE:
                incidents.sort((fst, snd) -> collator.compare(fst.getAssignee(), snd.getAssignee()));
                break;
            case TIMESTAMP:
                incidents.sort(Comparator.comparing(IncidentInfoDto::getCreated));
                break;
            default:
                throw new GeneralException("unknown sort field");
        }
        if (order == Order.DESC)
            return Lists.reverse(incidents);
        return incidents;
    }

    /**
     * Gets all incidents (also the historical ones) of ingest workflow
     *
     * @param externalId external id of ingest workflow
     * @return Sorted {@link List}
     */
    public List<IncidentInfoDto> getIncidentsOfIngestWorkflow(String externalId) {
        List<IncidentInfoDto> incidentDtos = new ArrayList<>();
        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();
        query.variableValueEquals(BpmConstants.ProcessVariables.ingestWorkflowExternalId, externalId);
        List<HistoricProcessInstance> processInstances = query.list();
        if (processInstances.isEmpty()) {
            return null;
        }
        if (processInstances.size() > 1) {
            throw new GeneralException("Expecting one process instance but found " + processInstances.size() +
                    " for ingest workflow with external id  " + externalId);
        }
        HistoricProcessInstance pi = processInstances.get(0);

        Map<String, Object> processVariables = historyService
                .createHistoricVariableInstanceQuery()
                .processInstanceId(pi.getId())
                .list()
                .stream()
                .collect(Collectors.toMap(
                        HistoricVariableInstance::getName,
                        HistoricVariableInstance::getValue
                ));

        Map<String, List<HistoricIncident>> incidents = historyService
                .createHistoricIncidentQuery()
                .processInstanceId(pi.getId())
                .orderByCreateTime()
                .asc()
                .list()
                .stream()
                .collect(groupingBy(HistoricIncident::getConfiguration, mapping(hi -> hi, toList())));

        for (String jobId : incidents.keySet()) {
            List<HistoricIncident> incidentsOfJob = incidents.get(jobId);
            List<HistoricJobLog> jobLogs = historyService
                    .createHistoricJobLogQuery()
                    .jobId(jobId)
                    .failureLog()
                    .orderPartiallyByOccurrence()
                    .asc()
                    .list();
            jobLogs = jobLogs.stream().filter(l -> {
                String msg = l.getJobExceptionMessage();
                return msg != null && msg.contains(IncidentException.INCIDENT_MSG_PREFIX);
            }).collect(Collectors.toList());
            if (incidentsOfJob.size() != jobLogs.size())
                throw new GeneralException("Unexpected error - count of incidents of a job is not the same as count of job failure logs");
            for (int i = 0; i < incidentsOfJob.size(); i++) {
                HistoricIncident incident = incidentsOfJob.get(i);
                String stackTrace = historyService.getHistoricJobLogExceptionStacktrace(jobLogs.get(i).getId());

                String configUsedWhenIncidentOccurred = (String) processVariables.get(toIncidentConfigVariableName(incident.getId()));
                if (configUsedWhenIncidentOccurred == null)
                    configUsedWhenIncidentOccurred = (String) processVariables.get(BpmConstants.ProcessVariables.latestConfig);

                incidentDtos.add(
                        new IncidentInfoDto(
                                incident.getId(),
                                incident.getCreateTime().toInstant(),
                                incident.getEndTime() == null ? null : incident.getEndTime().toInstant(),
                                incident.getIncidentMessage(),
                                stackTrace,
                                incident.getActivityId(),
                                (String) processVariables.get(BpmConstants.ProcessVariables.batchId),
                                externalId,
                                (String) processVariables.get(BpmConstants.ProcessVariables.assignee),
                                configUsedWhenIncidentOccurred,
                                incident.getProcessInstanceId()
                        )
                );
            }
        }
        return incidentDtos;
    }

    /**
     * Tries to solve incidents by new config.
     * <p>
     * Incident are solved by workers. If the exception is thrown during incident solution other incident solutions are not affected.
     * </p>
     *
     * @param incidentIds
     * @param config
     */
    public void solveIncidents(List<String> incidentIds, String config) {
        for (String incidentId : incidentIds) {
            template.convertAndSend("incident", new SolveIncidentDto(incidentId, config));
        }
    }

    /**
     * Cancel process instance of the incident.
     * <p>
     * Process instances are cancelled one by one. If the exception is thrown already cancelled process instances are not rollbacked.
     * </p>
     *
     * @param incidentIds
     * @param reason
     * @throws MissingObject if some incident can't be found by id
     */
    public void cancelIncidents(List<String> incidentIds, String reason) {
        for (String incidentId : incidentIds) {
            log.info("Cancelling incidents with ids " + incidentIds.toString() + ".");
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().incidentId(incidentId).singleResult();
            notNull(processInstance, (() -> new MissingObject(Incident.class, incidentId)));
            String externalId = (String) runtimeService.getVariable(processInstance.getId(), BpmConstants.ProcessVariables.ingestWorkflowExternalId);
            String assignee = (String) runtimeService.getVariable(processInstance.getId(), BpmConstants.ProcessVariables.assignee);

            ingestErrorHandler.handleError(
                    externalId,
                    new IngestWorkflowFailureInfo(reason, null, IngestWorkflowFailureType.INCIDENT_CANCELLATION),
                    processInstance.getId(), assignee);
        }
    }

    @Inject
    public void setRuntimeService(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Inject
    public void setManagementService(ManagementService managementService) {
        this.managementService = managementService;
    }

    @Inject
    public void setHistoryService(HistoryService historyService) {
        this.historyService = historyService;
    }

    @Inject
    public void setTemplate(JmsTemplate template) {
        this.template = template;
    }

    @Inject
    public void setIngestErrorHandler(IngestErrorHandler ingestErrorHandler) {
        this.ingestErrorHandler = ingestErrorHandler;
    }
}
