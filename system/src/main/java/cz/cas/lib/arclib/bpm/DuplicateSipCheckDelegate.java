package cz.cas.lib.arclib.bpm;

import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import cz.cas.lib.arclib.utils.ArclibUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Service;

import static cz.cas.lib.arclib.domain.VersioningLevel.ARCLIB_XML_VERSIONING;

@Slf4j
@Service
public class DuplicateSipCheckDelegate extends ArclibDelegate {

    public static final String CONFIG_PATH = "/continueOnDuplicateSip";
    @Getter
    private String toolName = "ARCLib_duplicate_sip_check";

    @Override
    public void executeArclibDelegate(DelegateExecution execution) throws IncidentException {
        IngestWorkflow iw = ingestWorkflowService.findByExternalId(getIngestWorkflowExternalId(execution));
        if (iw.getVersioningLevel() == ARCLIB_XML_VERSIONING) {
            Pair<Boolean, String> booleanConfig = ArclibUtils.parseBooleanConfig(getConfigRoot(execution), CONFIG_PATH);
            if (booleanConfig.getLeft() == null)
                throw new IncidentException(booleanConfig.getRight());
            if (!booleanConfig.getLeft())
                throw new BpmnError(BpmConstants.ErrorCodes.ProcessFailure, booleanConfig.getRight());
        }
    }
}
