package cz.cas.lib.arclib.bpm;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cas.lib.arclib.domain.IngestToolFunction;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestEvent;
import cz.cas.lib.arclib.service.validator.Validator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;

import static cz.cas.lib.core.util.Utils.notNull;

@Slf4j
@Service
public class ValidatorDelegate extends ArclibDelegate {

    protected Validator service;
    @Getter
    private String toolName = "ARCLib_" + IngestToolFunction.validation;
    public static final String VALIDATION_PROFILE_CONFIG_ENTRY = "validationProfile";

    /**
     * Executes the validation process for the given SIP. In case of a validation error a BPMN error is thrown.
     */
    @Override
    public void executeArclibDelegate(DelegateExecution execution) {
        log.debug("Execution of Validator delegate started.");
        JsonNode configRoot = getConfigRoot(execution);
        String validationProfileExternalId = configRoot.get(VALIDATION_PROFILE_CONFIG_ENTRY).textValue();
        notNull(validationProfileExternalId, () -> {
            throw new IllegalArgumentException("null id of the validation profile");
        });

        String externalId = getStringVariable(execution, BpmConstants.ProcessVariables.ingestWorkflowExternalId);
        notNull(externalId, () -> {
            throw new IllegalArgumentException(("empty external id of the ingest workflow"));
        });

        String sipId = getStringVariable(execution, BpmConstants.ProcessVariables.sipId);
        notNull(sipId, () -> {
            throw new IllegalArgumentException("null id of the sip of ingest workflow with external id "
                    + externalId);
        });

        try {
            service.validateSip(sipId, (String) execution.getVariable(BpmConstants.ProcessVariables.sipFolderWorkspacePath),
                    validationProfileExternalId);

        } catch (ParserConfigurationException | IOException | SAXException | XPathExpressionException e) {
            throw new BpmnError(BpmConstants.ErrorCodes.ProcessFailure, "SIP id: " + sipId + ". " + e.getMessage());
        }
        ingestEventStore.save(new IngestEvent(ingestWorkflowService.findByExternalId(externalId), toolService.findByNameAndVersion(getToolName(), getToolVersion()), true, null));
        log.debug("Execution of Validator delegate finished.");
    }

    @Inject
    public void setService(Validator service) {
        this.service = service;
    }
}
