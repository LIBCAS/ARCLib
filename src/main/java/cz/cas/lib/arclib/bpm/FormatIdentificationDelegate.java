package cz.cas.lib.arclib.bpm;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cas.lib.arclib.exception.bpm.ConfigParserException;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import cz.cas.lib.arclib.service.formatIdentification.FormatIdentificationTool;
import cz.cas.lib.core.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static cz.cas.lib.arclib.service.formatIdentification.FormatIdentificationTool.initialize;
import static cz.cas.lib.arclib.utils.ArclibUtils.getSipFolderWorkspacePath;

@Slf4j
@Service
public class FormatIdentificationDelegate extends ArclibDelegate implements JavaDelegate {

    /**
     * Performs the format analysis of files in SIP.
     *
     * @throws ConfigParserException the JSON configuration is invalid
     * @throws IncidentException     one or more files have been identified with multiple formats
     *                               and it was unable to resolve the conflict with the configuration JSON
     */
    @Override
    public void execute(DelegateExecution execution) throws IncidentException, IOException {
        String ingestWorkflowExternalId = getIngestWorkflowExternalId(execution);
        log.info("Execution of Format identifier delegate started for ingest workflow " + ingestWorkflowExternalId + ".");

        JsonNode configRoot = getConfigRoot(execution);

        FormatIdentificationTool formatIdentificationTool = initialize(configRoot);
        execution.setVariable(BpmConstants.FormatIdentification.toolVersion, formatIdentificationTool.getToolVersion());
        execution.setVariable(BpmConstants.FormatIdentification.toolName, formatIdentificationTool.getToolName());

        String originalSipFileName = getStringVariable(execution, BpmConstants.Ingestion.originalSipFileName);
        Map<String, List<Utils.Pair<String, String>>> identifiedFormats = formatIdentificationTool.analyze
                (getSipFolderWorkspacePath(ingestWorkflowExternalId, workspace, originalSipFileName));

        TreeMap<String, Utils.Pair<String, String>> resultingFormats = formatIdentificationTool
                .resolveAmbiguousIdentifications(identifiedFormats, configRoot, ingestWorkflowExternalId);

        execution.setVariable(BpmConstants.FormatIdentification.mapOfFilesToFormats, resultingFormats);
        execution.setVariable(BpmConstants.FormatIdentification.success, true);
        execution.setVariable(BpmConstants.FormatIdentification.dateTime,
                Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());

        log.info("Execution of Format identifier delegate finished for ingest workflow " + ingestWorkflowExternalId + ".");
    }
}
