package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.domain.Batch;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestEvent;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.dto.IngestWorkflowDto;
import cz.cas.lib.arclib.store.IngestEventStore;
import cz.cas.lib.arclib.store.IngestWorkflowStore;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.core.store.Transactional;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cz.cas.lib.core.util.Utils.notNull;

@Service
public class IngestWorkflowService {

    private IngestWorkflowStore store;
    private HistoryService historyService;
    private IngestEventStore ingestEventStore;

    public Collection<IngestWorkflow> findAll() {
        return store.findAll();
    }

    public IngestWorkflow find(String id) {
        return store.find(id);
    }

    public void delete(IngestWorkflow iw) {
        store.delete(iw);
    }

    public void hardDelete(IngestWorkflow iw) {
        store.hardDelete(iw);
    }

    @Transactional
    public IngestWorkflow save(IngestWorkflow entity) {
        return store.save(entity);
    }

    /**
     * Gets ingest workflow by external id
     *
     * @param externalId ingestWorkflowExternalId
     * @return instance of ingest workflow found
     */
    public IngestWorkflow findByExternalId(String externalId) {
        IngestWorkflow ingestWorkflow = store.findByExternalId(externalId);
        notNull(ingestWorkflow, () -> new MissingObject(IngestWorkflow.class, externalId));
        return ingestWorkflow;
    }

    public IngestWorkflowDto getInfo(String externalId) {
        Map<String, Object> variables = getVariables(externalId);
        List<IngestEvent> events = ingestEventStore.findAllOfIngestWorkflow(externalId);
        IngestWorkflow ingestWorkflow = store.findByExternalId(externalId);
        notNull(ingestWorkflow, () -> new MissingObject(IngestWorkflow.class, ingestWorkflow.getExternalId()));
        Batch batch = ingestWorkflow.getBatch();
        ingestWorkflow.setBatch(null);
        return new IngestWorkflowDto(ingestWorkflow, variables, events, batch);
    }

    public List<IngestWorkflow> findByAuthorialPackageId(String authorialPackageId) {
        return store.findByAuthorialPackageId(authorialPackageId);
    }

    public List<IngestWorkflow> findBySipId(String sipId) {
        return store.findBySipId(sipId);
    }

    /**
     * Gets all the process variables of a ingest workflow.
     *
     * @param externalId external id of the ingest workflow
     * @return {@link Map} of the variables
     */
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

        Map<String, Object> variables = new HashMap<>();
        for (HistoricVariableInstance var : historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(list.get(0).getId())
                .list()) {
            variables.put(var.getName(), var.getValue());
        }
        return variables;
    }

    /**
     * Gets specific process variable of a ingest workflow.
     *
     * @param externalId   external id of the ingest workflow
     * @param variableName
     * @return Object variable
     */
    public Object getVariable(String externalId, String variableName) {
        List<HistoricProcessInstance> list = historyService
                .createHistoricProcessInstanceQuery()
                .variableValueEquals(BpmConstants.ProcessVariables.ingestWorkflowExternalId, externalId)
                .list();
        if (list.isEmpty())
            return null;
        if (list.size() > 1) {
            throw new GeneralException("Expecting one process instance but found " + list.size() + " for ingestWorkflow with ingestWorkflowExternalId  " + externalId);
        }

        List<HistoricVariableInstance> vars = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(list.get(0).getId())
                .variableName(variableName)
                .list();
        if (vars.size() > 1)
            throw new IllegalStateException("found more then one variable: " + variableName + " for IW: " + externalId);
        return vars.isEmpty() ? null : vars.get(0).getValue();
    }

    @Inject
    public void setIngestEventStore(IngestEventStore ingestEventStore) {
        this.ingestEventStore = ingestEventStore;
    }

    @Inject
    public void setStore(IngestWorkflowStore store) {
        this.store = store;
    }

    @Inject
    public void setHistoryService(HistoryService historyService) {
        this.historyService = historyService;
    }

}
