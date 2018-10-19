package cz.cas.lib.arclib.bpm;

import cz.cas.lib.arclib.service.validator.Validator;
import cz.cas.lib.arclib.utils.ArclibUtils;
import cz.cas.lib.core.store.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static cz.cas.lib.core.util.Utils.notNull;

@Slf4j
@Component
public class ValidatorDelegate extends ArclibDelegate implements JavaDelegate {

    protected Validator service;

    /**
     * Executes the validation process for the given SIP. In case of a validation error a BPMN error is thrown.
     */
    @Transactional
    @Override
    public void execute(DelegateExecution execution) {
        log.info("Execution of Validator delegate started.");
        String validationProfileId = getStringVariable(execution, BpmConstants.Validation.validationProfileId);
        notNull(validationProfileId, () -> {
            throw new IllegalArgumentException("null id of the validation profile");
        });

        String externalId = getStringVariable(execution, BpmConstants.ProcessVariables.ingestWorkflowExternalId);
        notNull(externalId, () -> {
            throw new IllegalArgumentException(("empty external id of the ingest workflow"));
        });

        String sipId = getStringVariable(execution, BpmConstants.ProcessVariables.sipId);
        notNull(sipId, () -> {
            throw new IllegalArgumentException("null id of the sip of ingest workflow with external id "
                    + BpmConstants.ProcessVariables.ingestWorkflowExternalId);
        });

        try {
            String originalSipFileName = getStringVariable(execution, BpmConstants.Ingestion.originalSipFileName);
            service.validateSip(sipId, ArclibUtils.getSipFolderWorkspacePath(externalId, workspace,
                    originalSipFileName).toString(), validationProfileId);
        } catch (ParserConfigurationException | IOException | SAXException | XPathExpressionException e) {
            throw new BpmnError(BpmConstants.ErrorCodes.ProcessFailure, "SIP id: " + sipId + ". " + e.getMessage());
        }
        execution.setVariable(BpmConstants.Validation.success, true);
        execution.setVariable(BpmConstants.Validation.dateTime, Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
        log.info("Execution of Validator delegate finished.");
    }

    @Inject
    public void setService(Validator service) {
        this.service = service;
    }
}
