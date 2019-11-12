package cz.cas.lib.arclib.bpm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import cz.cas.lib.arclib.domain.IngestToolFunction;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestEvent;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestIssue;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.preservationPlanning.IngestIssueDefinitionCode;
import cz.cas.lib.arclib.domain.preservationPlanning.Tool;
import cz.cas.lib.arclib.exception.bpm.ConfigParserException;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import cz.cas.lib.arclib.service.antivirus.Antivirus;
import cz.cas.lib.arclib.service.antivirus.AntivirusType;
import cz.cas.lib.arclib.service.antivirus.ClamAntivirus;
import cz.cas.lib.arclib.service.antivirus.InfectedSipAction;
import cz.cas.lib.arclib.utils.ArclibUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static cz.cas.lib.arclib.utils.ArclibUtils.parseEnumFromConfig;

@Slf4j
@Service
public class AntivirusDelegate extends ArclibDelegate {

    public static final String ANTIVIRUS_TOOL_EXPR = "/antivirus";
    public static final String INFECTED_SIP_ACTION = "/infectedSipAction";
    public static final String ANTIVIRUS_TYPE = "/type";
    public static final String ANTIVIRUS_CMD = "/cmd";
    private Path quarantinePath;
    @Getter
    private String toolName = "ARCLib_" + IngestToolFunction.virus_check;

    /**
     * Scans SIP package for viruses.
     *
     * @throws IncidentException if the config cannot be parsed
     */
    @Override
    public void executeArclibDelegate(DelegateExecution execution) throws IncidentException, FileNotFoundException {
        String ingestWorkflowExternalId = getIngestWorkflowExternalId(execution);
        log.debug("Execution of Antivirus delegate started for ingest workflow " + ingestWorkflowExternalId + ".");
        IngestWorkflow iw = ingestWorkflowStore.findByExternalId(ingestWorkflowExternalId);
        JsonNode configRoot = getConfigRoot(execution);

        Path sipPath = ArclibUtils.getIngestWorkflowWorkspacePath(ingestWorkflowExternalId, workspace);
        int antivirusToolCounter = (int) execution.getVariable(BpmConstants.Antivirus.antivirusToolCounter);

        Antivirus antivirus = initialize(configRoot, iw, execution, antivirusToolCounter);
        antivirus.scan(sipPath, iw);

        ingestEventStore.save(new IngestEvent(new IngestWorkflow(iw.getId()), antivirus.getToolEntity(), true, antivirus.getToolVersion()));

        execution.setVariable(BpmConstants.Antivirus.antivirusToolCounter, antivirusToolCounter + 1);
        log.debug("Execution of Antivirus delegate finished for ingest workflow " + ingestWorkflowExternalId + ".");
    }

    public Antivirus initialize(JsonNode root, IngestWorkflow iw, DelegateExecution ex, int antivirusToolCounter) throws ConfigParserException {
        try {
            InfectedSipAction infectedSipAction = parseEnumFromConfig(root,
                    ANTIVIRUS_TOOL_EXPR + "/" + antivirusToolCounter + AntivirusDelegate.INFECTED_SIP_ACTION, InfectedSipAction.class);
            AntivirusType avType = parseEnumFromConfig(root, ANTIVIRUS_TOOL_EXPR + "/" + antivirusToolCounter + AntivirusDelegate.ANTIVIRUS_TYPE, AntivirusType.class);
            Antivirus antivirusToBeUsed;
            switch (avType) {
                case CLAMAV:
                    String cmdExpr = ANTIVIRUS_TOOL_EXPR + "/" + antivirusToolCounter + ANTIVIRUS_CMD;
                    JsonNode cmdNode = root.at(cmdExpr);
                    if (cmdNode.getNodeType() != JsonNodeType.ARRAY || cmdNode.size() < 1)
                        throw new ConfigParserException(cmdExpr, cmdNode.toString(), "Antivirus executable, with full path if not in $PATH variable, with switches");
                    String[] cmd = ((List<String>) objectMapper.convertValue(cmdNode, List.class)).toArray(new String[0]);
                    antivirusToBeUsed = new ClamAntivirus(cmd);
                    break;
                default:
                    throw new ConfigParserException(ANTIVIRUS_TOOL_EXPR + "/" + antivirusToolCounter + AntivirusDelegate.ANTIVIRUS_TYPE,
                            "not supported", AntivirusType.class);
            }
            //e.g. 'CLAMAV version: [ClamAV 0.100.2/25043/Tue Oct 16 23:06:18 2018]'
            String fullToolVersion = antivirusToBeUsed.getToolVersion();
            //e.g. 'CLAMAV version: ClamAV 0.100.2'
            String shortToolVersion = fullToolVersion.substring(1, fullToolVersion.indexOf("/"));

            Tool toolEntity = toolService.createNewToolVersionIfNeeded(antivirusToBeUsed.getToolName(), shortToolVersion, IngestToolFunction.virus_check);
            antivirusToBeUsed.inject(formatDefinitionService, ingestIssueService, toolEntity, ingestIssueDefinitionStore, quarantinePath, infectedSipAction, getFormatIdentificationResult(ex));
            return antivirusToBeUsed;
        } catch (ConfigParserException e) {
            ingestIssueService.save(new IngestIssue(
                    iw,
                    toolService.findByNameAndVersion(getToolName(), getToolVersion()),
                    ingestIssueDefinitionStore.findByCode(IngestIssueDefinitionCode.CONFIG_PARSE_ERROR),
                    null,
                    e.getMessage(),
                    false
            ));
            throw e;
        }
    }

    @Inject
    public void setQuarantinePath(@Value("${arclib.path.quarantine}") String quarantinePath) {
        this.quarantinePath = Paths.get(quarantinePath);
    }
}
