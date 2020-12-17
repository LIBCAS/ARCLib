package cz.cas.lib.arclib.bpm;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cas.lib.arclib.domain.IngestToolFunction;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestEvent;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.service.validator.Validator;
import cz.cas.lib.arclib.store.ProducerProfileStore;
import cz.cas.lib.arclib.utils.ArclibUtils;
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

import static cz.cas.lib.core.util.Utils.isNull;
import static cz.cas.lib.core.util.Utils.notNull;

@Slf4j
@Service
public class ValidatorDelegate extends ArclibDelegate {

    public static final String VALIDATION_PROFILE_CONFIG_ENTRY = "validationProfile";

    @Getter
    private String toolName = "ARCLib_" + IngestToolFunction.validation;

    protected Validator service;
    private ProducerProfileStore producerProfileStore;


    /**
     * Executes the validation process for the given SIP. In case of a validation error a BPMN error is thrown.
     */
    @Override
    public void executeArclibDelegate(DelegateExecution execution) {
        String usedValidationProfile = getStringVariable(execution, BpmConstants.Validation.usedValidationProfile);
        isNull(usedValidationProfile, () -> new BpmnError(BpmConstants.ErrorCodes.ProcessFailure, ArclibUtils.trimBpmnErrorMsg(
                "Attempting to start Validator but the usedValidationProfile variable is already filled. Workflow definition can contain only one instance of the validator.")));

        JsonNode configRoot = getConfigRoot(execution);
        String validationProfileExternalId = configRoot.get(VALIDATION_PROFILE_CONFIG_ENTRY).textValue();
        if (validationProfileExternalId == null) {
            String producerProfileExternalId = getProducerProfileExternalId(execution);
            ProducerProfile producerProfile = producerProfileStore.findByExternalId(producerProfileExternalId);
            validationProfileExternalId = producerProfile.getValidationProfile().getExternalId();
        }

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
            throw new BpmnError(BpmConstants.ErrorCodes.ProcessFailure, ArclibUtils.trimBpmnErrorMsg("SIP id: " + sipId + ". " + e.getMessage()));
        }
        ingestEventStore.save(new IngestEvent(ingestWorkflowService.findByExternalId(externalId), toolService.getByNameAndVersion(getToolName(), getToolVersion()), true, null));
        execution.setVariable(BpmConstants.Validation.usedValidationProfile, validationProfileExternalId);
    }

    @Inject
    public void setProducerProfileStore(ProducerProfileStore producerProfileStore) {
        this.producerProfileStore = producerProfileStore;
    }

    @Inject
    public void setService(Validator service) {
        this.service = service;
    }
}
