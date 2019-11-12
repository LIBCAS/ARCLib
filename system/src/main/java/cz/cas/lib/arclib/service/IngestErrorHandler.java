package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflowFailureInfo;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflowFailureType;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflowState;
import cz.cas.lib.arclib.domain.packages.AuthorialPackage;
import cz.cas.lib.arclib.domain.packages.Sip;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.dto.JmsDto;
import cz.cas.lib.core.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.inject.Inject;

@Service
@Slf4j
public class IngestErrorHandler {

    private IngestWorkflowService ingestWorkflowService;
    private JmsTemplate template;
    private RuntimeService runtimeService;
    private AipService aipService;
    private TransactionTemplate transactionTemplate;

    /**
     * Assigns failure info to ingest workflow, deactivates AIP update lock, kills camunda process and notifies Coordinator
     *
     * @param externalId                external id of the ingest workflow
     * @param ingestWorkflowFailureInfo failure info to be assigned
     * @param processInstanceId         process instance id
     * @param userId                    id of the user
     */
    public void handleError(String externalId, IngestWorkflowFailureInfo ingestWorkflowFailureInfo, String processInstanceId, String userId) {
        IngestWorkflow iw = transactionTemplate.execute(status -> {
            //all DB operations in the template will be rolled back in case of any exception
            IngestWorkflow ingestWorkflow = ingestWorkflowService.findByExternalId(externalId);
            Utils.notNull(ingestWorkflow, () -> new MissingObject(IngestWorkflow.class, externalId));

            ingestWorkflow.setProcessingState(IngestWorkflowState.FAILED);
            ingestWorkflow.setFailureInfo(ingestWorkflowFailureInfo);
            ingestWorkflowService.save(ingestWorkflow);
            log.info("State of ingest workflow with external id " + ingestWorkflow.getExternalId() + " changed to " + IngestWorkflowState.FAILED + ".");

            IngestWorkflowFailureType failureType = ingestWorkflowFailureInfo.getIngestWorkflowFailureType();
            String cancellationReason = ingestWorkflowFailureInfo.getMsg();

            switch (failureType) {
                case BPM_ERROR:
                    log.error(failureType + " : " + ingestWorkflowFailureInfo.getMsg());
                    if (processInstanceId != null)
                        runtimeService.deleteProcessInstance(processInstanceId, cancellationReason, false, false);
                    break;
                case INTERNAL_ERROR:
                    log.error(failureType + " : " + ingestWorkflowFailureInfo.getMsg());
                    if (processInstanceId != null)
                        runtimeService.deleteProcessInstance(processInstanceId, cancellationReason, false, false);
                    break;
                case AUTHORIAL_PACKAGE_LOCKED:
                    log.info(failureType + " : " + ingestWorkflowFailureInfo.getMsg());
                    break;
                case INVALID_CHECKSUM:
                    log.error(failureType + " : " + ingestWorkflowFailureInfo.getMsg());
                    break;
                case INCIDENT_CANCELLATION:
                    log.info(failureType + " : " + ingestWorkflowFailureInfo.getMsg());
                    if (processInstanceId != null)
                        runtimeService.deleteProcessInstance(processInstanceId, cancellationReason, false, true);
                    break;
                case BATCH_CANCELLATION:
                    log.info(failureType + " : " + ingestWorkflowFailureInfo.getMsg());
                    if (processInstanceId != null)
                        runtimeService.deleteProcessInstance(processInstanceId, cancellationReason, false, true);
                    break;
            }

            Sip sip = ingestWorkflow.getSip();
            if (sip != null) {
                AuthorialPackage authorialPackage = sip.getAuthorialPackage();
                if (authorialPackage != null) {
                    String authorialPackageId = authorialPackage.getId();
                    aipService.deactivateLock(authorialPackageId);
                }
            }
            return ingestWorkflow;
        });
        if (ingestWorkflowFailureInfo.getIngestWorkflowFailureType() != IngestWorkflowFailureType.BATCH_CANCELLATION)
            template.convertAndSend("finish", new JmsDto(iw.getBatch().getId(), userId));
    }

    @Inject
    public void setIngestWorkflowService(IngestWorkflowService ingestWorkflowService) {
        this.ingestWorkflowService = ingestWorkflowService;
    }

    @Inject
    public void setTemplate(JmsTemplate template) {
        this.template = template;
    }

    @Inject
    public void setRuntimeService(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Inject
    public void setAipService(AipService aipService) {
        this.aipService = aipService;
    }

    @Inject
    public void setTransactionTemplate(TransactionTemplate transactionTemplate) {this.transactionTemplate = transactionTemplate;}
}
