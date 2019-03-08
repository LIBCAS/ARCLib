package cz.cas.lib.arclib.bpm;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cas.lib.arclib.domain.IngestToolFunction;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestEvent;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestIssue;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.preservationPlanning.FormatDefinition;
import cz.cas.lib.arclib.domain.preservationPlanning.FormatOccurrence;
import cz.cas.lib.arclib.domain.preservationPlanning.IngestIssueDefinitionCode;
import cz.cas.lib.arclib.domain.preservationPlanning.Tool;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.exception.bpm.ConfigParserException;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import cz.cas.lib.arclib.service.ProducerProfileService;
import cz.cas.lib.arclib.service.formatIdentification.FormatIdentificationTool;
import cz.cas.lib.arclib.service.formatIdentification.FormatIdentificationToolType;
import cz.cas.lib.arclib.service.formatIdentification.droid.CsvResultColumn;
import cz.cas.lib.arclib.service.formatIdentification.droid.DroidFormatIdentificationTool;
import cz.cas.lib.arclib.store.FormatOccurrenceStore;
import cz.cas.lib.core.store.Transactional;
import cz.cas.lib.core.util.Utils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static cz.cas.lib.arclib.service.formatIdentification.FormatIdentificationTool.*;
import static cz.cas.lib.arclib.utils.ArclibUtils.parseEnumFromConfig;

@Slf4j
@Service
public class FormatIdentificationDelegate extends ArclibDelegate implements JavaDelegate {

    @Getter
    private String toolName="ARCLib_"+ IngestToolFunction.format_identification;
    private FormatOccurrenceStore formatOccurrenceStore;
    private ProducerProfileService producerProfileService;
    /**
     * Performs the format analysis of files in SIP.
     *
     * @throws ConfigParserException the JSON configuration is invalid
     * @throws IncidentException     one or more files have been identified with multiple formats
     *                               and it was unable to resolve the conflict with the configuration JSON
     */
    @Override
    @Transactional
    public void execute(DelegateExecution execution) throws IncidentException, IOException {
        IngestWorkflow iw = ingestWorkflowStore.findByExternalId(getIngestWorkflowExternalId(execution));
        log.debug("Execution of Format identifier delegate started for ingest workflow " + iw.getExternalId() + ".");

        JsonNode configRoot = getConfigRoot(execution);

        FormatIdentificationTool formatIdentificationTool = initialize(configRoot, iw);
        execution.setVariable(BpmConstants.FormatIdentification.toolId, formatIdentificationTool.getToolEntity().getId());

        Map<String, List<Utils.Pair<String, String>>> identifiedFormats = formatIdentificationTool.analyze
                (Paths.get((String) execution.getVariable(BpmConstants.ProcessVariables.sipFolderWorkspacePath)));
        updateFormatOccurrences(identifiedFormats, getProducerProfileExternalId(execution));

        TreeMap<String, Utils.Pair<String, String>> resultingFormats = formatIdentificationTool
                .resolveAmbiguousIdentifications(identifiedFormats, configRoot, iw.getExternalId());

        execution.setVariable(BpmConstants.FormatIdentification.mapOfFilesToFormats, resultingFormats);
        IngestEvent event = ingestEventStore.save(new IngestEvent(new IngestWorkflow(iw.getId()), formatIdentificationTool.getToolEntity(), true, null));
        execution.setVariable(BpmConstants.FormatIdentification.success, true);
        execution.setVariable(BpmConstants.FormatIdentification.dateTime,
                event.getCreated().truncatedTo(ChronoUnit.SECONDS).toString());

        log.debug("Execution of Format identifier delegate finished for ingest workflow " + iw.getExternalId() + ".");
    }

    /**
     * Create and initialize the format identifier using the JSON configuration
     *
     * @param root JSON config
     * @return instance of {@link FormatIdentificationTool} implemented according to the JSON configuration
     * @throws ConfigParserException JSON configuration is missing the necessary attributes for the configuration
     *                               of {@link FormatIdentificationTool}
     */
    @Transactional
    public FormatIdentificationTool initialize(JsonNode root, IngestWorkflow iw) throws ConfigParserException {
        try {
            FormatIdentificationToolType formatIdentificationToolType = parseEnumFromConfig(root, FORMAT_IDENTIFIER_EXPR + IDENTIFIER_TYPE_EXPR,
                    FormatIdentificationToolType.class);
            FormatIdentificationTool tool;
            switch (formatIdentificationToolType) {
                case DROID:
                    CsvResultColumn parsedColumn = parseEnumFromConfig(root, FORMAT_IDENTIFIER_EXPR + PARSED_COLUMN_EXPR,
                            CsvResultColumn.class);
                    log.debug("Format identification tool initialized with DROID.");
                    tool = new DroidFormatIdentificationTool(parsedColumn);
                    break;
                default:
                    throw new ConfigParserException(FORMAT_IDENTIFIER_EXPR + IDENTIFIER_TYPE_EXPR, "not supported", FormatIdentificationToolType.class);
            }
            Tool toolEntity = toolService.createNewToolVersionIfNeeded(tool.getToolName(), tool.getToolVersion(),IngestToolFunction.format_identification);
            tool.inject(formatDefinitionService, ingestIssueService, ingestIssueDefinitionStore, iw, toolEntity);
            return tool;
        } catch (ConfigParserException e) {
            ingestIssueService.save(new IngestIssue(
                    iw,
                    toolService.findByNameAndVersion(getToolName(),getToolVersion()),
                    ingestIssueDefinitionStore.findByCode(IngestIssueDefinitionCode.CONFIG_PARSE_ERROR),
                    null,
                    e.getMessage(),
                    false
            ));
            throw e;
        }
    }

    private void updateFormatOccurrences(Map<String, List<Utils.Pair<String, String>>> analyzedFormats, String producerProfileExId) {
        Map<String, Integer> puidOccurrenceMap = analyzedFormats
                .values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(pair -> pair.getL(), pair -> 1, (fst, snd) -> fst + 1));
        for (String puid : puidOccurrenceMap.keySet()) {
            FormatDefinition formatDefinition = formatDefinitionService.findPreferredDefinitionsByPuid(puid);
            ProducerProfile producerProfile = producerProfileService.findByExternalId(producerProfileExId);
            FormatOccurrence formatOccurrence = formatOccurrenceStore.findByFormatDefinitionAndProducerProfile(
                    formatDefinition.getId(), producerProfile.getId());
            if(formatOccurrence == null)
                formatOccurrence = new FormatOccurrence(formatDefinition,0,producerProfile);
            formatOccurrence.setOccurrences(formatOccurrence.getOccurrences() + puidOccurrenceMap.get(puid));
            formatOccurrenceStore.save(formatOccurrence);
        }
    }

    @Inject
    public void setFormatOccurrenceStore(FormatOccurrenceStore formatOccurrenceStore) {
        this.formatOccurrenceStore = formatOccurrenceStore;
    }

    @Inject
    public void setProducerProfileService(ProducerProfileService producerProfileService) {
        this.producerProfileService = producerProfileService;
    }
}
