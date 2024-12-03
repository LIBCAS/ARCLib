package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.domain.Batch;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestEvent;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.dto.IngestWorkflowDto;
import cz.cas.lib.arclib.service.tableexport.TableDataType;
import cz.cas.lib.arclib.service.tableexport.TableExportType;
import cz.cas.lib.arclib.service.tableexport.TableExporter;
import cz.cas.lib.arclib.store.IngestEventStore;
import cz.cas.lib.arclib.store.IngestWorkflowStore;
import cz.cas.lib.core.store.Transactional;
import jakarta.servlet.http.HttpServletResponse;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.stream.Collectors;

import static cz.cas.lib.core.util.Utils.notNull;

@Service
public class IngestWorkflowService {

    private IngestWorkflowStore store;
    private HistoryService historyService;
    private IngestEventStore ingestEventStore;
    private TableExporter tableExporter;

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
        IngestWorkflow ingestWorkflow = store.findByExternalId(externalId);
        notNull(ingestWorkflow, () -> new MissingObject(IngestWorkflow.class, externalId));
        Map<String, Object> variables = getVariables(externalId);
        List<IngestEvent> events = ingestEventStore.findAllOfIngestWorkflow(externalId);
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

    public void exportEvents(String externalId, String name, List<String> columns, List<String> header, TableExportType format, HttpServletResponse response) {
        List<IngestEvent> events = ingestEventStore.findAllOfIngestWorkflow(externalId);
        List<List<Object>> table = events.stream().map(e -> e.getExportTableValues(columns)).collect(Collectors.toList());

        try (OutputStream out = response.getOutputStream()) {
            switch (format) {
                case XLSX:
                    tableExporter.exportXlsx(name, header, IngestEvent.getExportTableConfig(columns), table, true, out);
                    break;
                case CSV:
                    tableExporter.exportCsv(name, header, table, out);
                    break;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void exportProcessVariables(String externalId, String name, TableExportType format, HttpServletResponse response) {
        Map<String, Object> variables = getVariables(externalId);
        List<List<Object>> table = variables.entrySet().stream().map(e -> {
            ArrayList<Object> list = new ArrayList<>();
            list.add(0, e.getKey());
            list.add(1, e.getValue());
            return (List<Object>) list;
        }).toList();
        List<String> header = List.of("", "");

        try (OutputStream out = response.getOutputStream()) {
            switch (format) {
                case XLSX:
                    tableExporter.exportXlsx(name, header, List.of(TableDataType.STRING_AUTO_SIZE, TableDataType.OTHER), table, false, out);
                    break;
                case CSV:
                    tableExporter.exportCsv(name, header, table, out);
                    break;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Autowired
    public void setIngestEventStore(IngestEventStore ingestEventStore) {
        this.ingestEventStore = ingestEventStore;
    }

    @Autowired
    public void setStore(IngestWorkflowStore store) {
        this.store = store;
    }

    @Autowired
    public void setHistoryService(HistoryService historyService) {
        this.historyService = historyService;
    }

    @Autowired
    public void setTableExporter(TableExporter tableExporter) {
        this.tableExporter = tableExporter;
    }
}
