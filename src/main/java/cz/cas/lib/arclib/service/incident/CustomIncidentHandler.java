package cz.cas.lib.arclib.service.incident;

import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflowFailureInfo;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflowFailureType;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import cz.cas.lib.arclib.service.IngestErrorHandler;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.incident.IncidentContext;
import org.camunda.bpm.engine.impl.incident.IncidentHandler;
import org.camunda.bpm.engine.impl.persistence.entity.IncidentEntity;
import org.camunda.bpm.engine.runtime.Incident;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Custom incident handler to distinguish reconfigurable error from non-reconfigurable. Non-reconfigurable error causes
 * e.g. runtime error, results in failing ingest workflow and deleted process instance whereas for reconfigurable errors
 * (exceptions extending {@link IncidentException} an incident is created and application waits for its resolution.
 */
@Slf4j
@Service
public class CustomIncidentHandler implements IncidentHandler {

    private final static String INCIDENT_HANDLER_TYPE = "failedJob";
    @Setter
    private ManagementService managementService;
    @Setter
    private RuntimeService runtimeService;
    @Setter
    private IngestErrorHandler ingestErrorHandler;

    @Override
    public void handleIncident(IncidentContext context, String message) {
        createIncident(context, message);
    }

    private Incident createIncident(IncidentContext context, String message) {
        if (message == null || !message.contains(IncidentException.INCIDENT_MSG_PREFIX)) {
            String externalId = (String) runtimeService.getVariable(context.getExecutionId(), BpmConstants.ProcessVariables.ingestWorkflowExternalId);
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().variableValueEquals(BpmConstants.ProcessVariables.ingestWorkflowExternalId, externalId).singleResult();
            IngestWorkflowFailureInfo failureInfo = new IngestWorkflowFailureInfo(message, managementService.getJobExceptionStacktrace(context.getConfiguration()), IngestWorkflowFailureType.INTERNAL_ERROR);
            String assignee = (String) runtimeService.getVariable(context.getExecutionId(), BpmConstants.ProcessVariables.assignee);
            ingestErrorHandler.handleError(externalId, failureInfo, processInstance.getId(), assignee);
            return null;
        }
        IncidentEntity newIncident = IncidentEntity.createAndInsertIncident(INCIDENT_HANDLER_TYPE, context, message);
        log.info("Created incident ID " + newIncident.getId() + " with message " + message + ".");

        if (context.getExecutionId() != null) {
            newIncident.createRecursiveIncidents();
            log.info("Created recursive incidents for incident ID " + newIncident.getId() + ".");
        }

        return newIncident;
    }

    @Override
    public void resolveIncident(IncidentContext context) {
        removeIncident(context, true);
    }

    @Override
    public void deleteIncident(IncidentContext context) {
        removeIncident(context, false);
    }

    private void removeIncident(IncidentContext context, boolean incidentResolved) {
        List<Incident> incidents = Context
                .getCommandContext()
                .getIncidentManager()
                .findIncidentByConfiguration(context.getConfiguration());

        for (Incident currentIncident : incidents) {
            IncidentEntity incident = (IncidentEntity) currentIncident;
            if (incidentResolved) {
                incident.resolve();
            } else {
                incident.delete();
            }
        }
    }

    public String getIncidentHandlerType() {
        return INCIDENT_HANDLER_TYPE;
    }
}