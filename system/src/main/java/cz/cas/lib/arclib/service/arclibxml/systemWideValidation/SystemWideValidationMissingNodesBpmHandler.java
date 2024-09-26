package cz.cas.lib.arclib.service.arclibxml.systemWideValidation;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestIssue;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.preservationPlanning.IngestIssueDefinitionCode;
import cz.cas.lib.arclib.domain.preservationPlanning.Tool;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import cz.cas.lib.arclib.service.IngestIssueService;
import cz.cas.lib.arclib.store.IngestIssueDefinitionStore;
import cz.cas.lib.arclib.utils.ArclibUtils;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static cz.cas.lib.arclib.utils.ArclibUtils.parseEnumFromConfig;

@Component
public class SystemWideValidationMissingNodesBpmHandler {

    public static final String SYSTEM_WIDE_VALIDATION_EXPR = "/systemWideValidation";
    public static final String SYSTEM_WIDE_VALIDATION_NODES_MISSING_AFTER_XSLT_CFG = "/missingNodesAfterXsltAction";
    public static final String SYSTEM_WIDE_VALIDATION_NODES_MISSING_AT_FINAL_VALIDATION_CFG = "/missingNodesAfterFinalValidationAction";
    private IngestIssueDefinitionStore ingestIssueDefinitionStore;
    private IngestIssueService ingestIssueService;

    public void handleMissingNodes(JsonNode configRoot,
                                   boolean xsltValidation,
                                   List<SystemWideValidationNodeConfig> missingNodes,
                                   IngestWorkflow iw,
                                   Tool tool) throws IncidentException {

        String sndLvlCfgKey = xsltValidation ? SYSTEM_WIDE_VALIDATION_NODES_MISSING_AFTER_XSLT_CFG : SYSTEM_WIDE_VALIDATION_NODES_MISSING_AT_FINAL_VALIDATION_CFG;
        SystemWideValidationNodeMissingAction missingNodesAction = parseEnumFromConfig(configRoot,
                SYSTEM_WIDE_VALIDATION_EXPR + sndLvlCfgKey, SystemWideValidationNodeMissingAction.class, false);

        List<IngestIssue> issues = new ArrayList<>();
        for (SystemWideValidationNodeConfig missingNode : missingNodes) {
            issues.add(new IngestIssue(
                    iw,
                    tool,
                    ingestIssueDefinitionStore.findByCode(IngestIssueDefinitionCode.SYSTEM_WIDE_VALIDATION_NODE_MISSING),
                    null,
                    "missing node " + missingNode.toString(),
                    missingNodesAction != null
            ));
        }

        if (missingNodesAction == null) {
            throw new IncidentException(issues);
        }

        switch (missingNodesAction) {
            case IGNORE:
                break;
            case CANCEL:
                ingestIssueService.save(issues);
                throw new BpmnError(BpmConstants.ErrorCodes.ProcessFailure, ArclibUtils.trimBpmnErrorMsg(
                        "Missing nodes detected during " + (xsltValidation ? "XSLT" : "final") + " validation, the process is cancelled according the JSON config. Missing nodes: " +
                                missingNodes.stream().map(SystemWideValidationNodeConfig::toString).collect(Collectors.joining(","))
                ));
        }
    }

    @Autowired
    public void setIngestIssueDefinitionStore(IngestIssueDefinitionStore ingestIssueDefinitionStore) {
        this.ingestIssueDefinitionStore = ingestIssueDefinitionStore;
    }

    @Autowired
    public void setIngestIssueService(IngestIssueService ingestIssueService) {
        this.ingestIssueService = ingestIssueService;
    }
}
