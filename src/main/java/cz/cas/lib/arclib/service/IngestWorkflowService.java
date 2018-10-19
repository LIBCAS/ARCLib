package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.domain.Batch;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.dto.IncidentInfoDto;
import cz.cas.lib.arclib.dto.IngestWorkflowDto;
import cz.cas.lib.arclib.service.incident.IncidentService;
import cz.cas.lib.arclib.store.IngestWorkflowStore;
import cz.cas.lib.core.exception.GeneralException;
import cz.cas.lib.core.exception.MissingObject;
import cz.cas.lib.core.rest.data.DelegateAdapter;
import cz.cas.lib.core.store.Transactional;
import lombok.Getter;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cz.cas.lib.core.util.Utils.notNull;

@Service
public class IngestWorkflowService implements DelegateAdapter<IngestWorkflow> {

    @Getter
    private IngestWorkflowStore delegate;
    private HistoryService historyService;
    private IncidentService incidentService;

    @Override
    @Transactional
    public IngestWorkflow save(IngestWorkflow entity) {
        return delegate.save(entity);
    }

    /**
     * Gets ingest workflow by external id
     *
     * @param externalId ingestWorkflowExternalId
     * @return instance of ingest workflow found
     */
    @Transactional
    public IngestWorkflow findByExternalId(String externalId) {
        IngestWorkflow ingestWorkflow = delegate.findByExternalId(externalId);
        notNull(ingestWorkflow, () -> new MissingObject(IngestWorkflow.class, externalId));
        return ingestWorkflow;
    }

    @Transactional
    public IngestWorkflowDto getInfo(String externalId) {
        Map<String, Object> variables = getVariables(externalId);
        List<IncidentInfoDto> incidents = incidentService.getIncidentsOfIngestWorkflow(externalId);
        IngestWorkflow ingestWorkflow = delegate.findByExternalId(externalId);
        notNull(ingestWorkflow, () -> new MissingObject(IngestWorkflow.class, ingestWorkflow.getExternalId()));
        Batch batch = ingestWorkflow.getBatch();
        ingestWorkflow.setBatch(null);
        return new IngestWorkflowDto(ingestWorkflow, variables, incidents, batch);
    }

    /**
     * Gets all the process variables of a ingest workflow.
     *
     * @param externalId external id of the ingest workflow
     * @return {@link Map} of the variables
     */
    @Transactional
    public Map<String, Object> getVariables(String externalId) {
        List<HistoricProcessInstance> list = historyService
                .createHistoricProcessInstanceQuery()
                .variableValueEquals(BpmConstants.ProcessVariables.ingestWorkflowExternalId, externalId)
                .list();
        if (list.isEmpty())
            return null;
        if (list.size() > 1) {
            throw new GeneralException("Expecting one process instance but found " + list.size() + " for ingestWorkflow with ingestWorkflowExternalId  " + externalId);
        }

        return historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(list.get(0).getId())
                .list()
                .stream()
                .collect(Collectors.toMap(
                        HistoricVariableInstance::getName,
                        HistoricVariableInstance::getValue
                ));
    }

    @Inject
    public void setIncidentService(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @Inject
    public void setDelegate(IngestWorkflowStore delegate) {
        this.delegate = delegate;
    }

    @Inject
    public void setHistoryService(HistoryService historyService) {
        this.historyService = historyService;
    }

}
