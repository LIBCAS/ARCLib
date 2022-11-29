package cz.cas.lib.arclib.bpm;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cas.lib.arclib.domain.IngestToolFunction;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestEvent;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.preservationPlanning.FormatOccurrence;
import cz.cas.lib.arclib.domain.preservationPlanning.Tool;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.exception.bpm.ConfigParserException;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import cz.cas.lib.arclib.formatlibrary.domain.FormatDefinition;
import cz.cas.lib.arclib.formatlibrary.service.FormatDefinitionService;
import cz.cas.lib.arclib.service.ProducerProfileService;
import cz.cas.lib.arclib.service.formatIdentification.FormatIdentificationTool;
import cz.cas.lib.arclib.service.formatIdentification.FormatIdentificationToolType;
import cz.cas.lib.arclib.service.formatIdentification.droid.DroidFormatIdentificationTool;
import cz.cas.lib.arclib.store.FormatOccurrenceStore;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static cz.cas.lib.arclib.service.formatIdentification.FormatIdentificationTool.FORMAT_IDENTIFICATON_TOOL_EXPR;
import static cz.cas.lib.arclib.service.formatIdentification.FormatIdentificationTool.IDENTIFIER_TYPE_EXPR;
import static cz.cas.lib.arclib.utils.ArclibUtils.parseEnumFromConfig;

@Slf4j
@Service
public class FormatIdentificationDelegate extends ArclibDelegate {

    @Getter
    private String toolName="ARCLib_"+ IngestToolFunction.format_identification;
    private FormatOccurrenceStore formatOccurrenceStore;
    private ProducerProfileService producerProfileService;
    private FormatDefinitionService formatDefinitionService;
    /**
     * Performs the format analysis of files in SIP.
     *
     * @throws ConfigParserException the JSON configuration is invalid
     * @throws IncidentException     one or more files have been identified with multiple formats
     *                               and it was unable to resolve the conflict with the configuration JSON
     */
    @Override
    public void executeArclibDelegate(DelegateExecution execution) throws IncidentException, IOException {
        IngestWorkflow iw = ingestWorkflowService.findByExternalId(ingestWorkflowExternalId);
        JsonNode configRoot = getConfigRoot(execution);

        //map that captures format identification events and the respective format identification results
        HashMap<String, TreeMap<String, Pair<String, String>>> mapOfEventIdsToMapsOfFilesToFormats =
                (HashMap<String, TreeMap<String, Pair<String, String>>>)
                        execution.getVariable(BpmConstants.FormatIdentification.mapOfEventIdsToMapsOfFilesToFormats);

        //counter specifies the position of the format identification among other format identifications in the given BPM workflow
        int formatIdentificationToolCounter = mapOfEventIdsToMapsOfFilesToFormats.size();

        FormatIdentificationTool formatIdentificationTool = initialize(configRoot, iw, formatIdentificationToolCounter);

        Map<String, List<Pair<String, String>>> identifiedFormats = formatIdentificationTool.analyze(getSipFolderWorkspacePath(execution));

        TreeMap<String, Pair<String, String>> resultingFormats = formatIdentificationTool
                .resolveAmbiguousIdentifications(identifiedFormats, configRoot, iw.getExternalId());

        updateFormatOccurrences(resultingFormats, getProducerProfileExternalId(execution));

        IngestEvent event = ingestEventStore.save(new IngestEvent(new IngestWorkflow(iw.getId()), formatIdentificationTool.getToolEntity(), true, null));
        mapOfEventIdsToMapsOfFilesToFormats.put(event.getId(), resultingFormats);
        if (formatIdentificationToolCounter == 0)
            execution.setVariable(BpmConstants.FormatIdentification.preferredFormatIdentificationEventId, event.getId());
    }

    /**
     * Create and initialize the format identifier using the JSON configuration
     *
     * @param root JSON config
     * @return instance of {@link FormatIdentificationTool} implemented according to the JSON configuration
     * @throws ConfigParserException JSON configuration is missing the necessary attributes for the configuration
     *                               of {@link FormatIdentificationTool}
     */
    public FormatIdentificationTool initialize(JsonNode root, IngestWorkflow iw, int formatIdentificationToolCounter) throws ConfigParserException {
        FormatIdentificationToolType formatIdentificationToolType = parseEnumFromConfig(root,
                FORMAT_IDENTIFICATON_TOOL_EXPR + "/" + formatIdentificationToolCounter + IDENTIFIER_TYPE_EXPR,
                FormatIdentificationToolType.class,true);
        FormatIdentificationTool tool;
        switch (formatIdentificationToolType) {
            case DROID:
                log.debug("Format identification tool initialized with DROID.");
                tool = new DroidFormatIdentificationTool();
                break;
            default:
                throw new ConfigParserException(FORMAT_IDENTIFICATON_TOOL_EXPR + "/" + formatIdentificationToolCounter + IDENTIFIER_TYPE_EXPR, "not supported", FormatIdentificationToolType.class);
        }
        Tool toolEntity = toolService.createNewToolVersionIfNeeded(tool.getToolName(), tool.getToolVersion(), IngestToolFunction.format_identification);
        tool.inject(formatDefinitionService, ingestIssueService, ingestIssueDefinitionStore, iw, toolEntity, formatIdentificationToolCounter);
        return tool;
    }

    private void updateFormatOccurrences(Map<String, Pair<String, String>> analyzedFormats, String producerProfileExId) {
        Map<String, Integer> puidOccurrenceMap = analyzedFormats
                .values()
                .stream()
                .collect(Collectors.toMap(Pair::getLeft, pair -> 1, (fst, snd) -> fst + 1));
        for (String puid : puidOccurrenceMap.keySet()) {
            FormatDefinition formatDefinition = formatDefinitionService.findPreferredDefinitionsByPuid(puid);
            ProducerProfile producerProfile = producerProfileService.findByExternalId(producerProfileExId);
            FormatOccurrence formatOccurrence = formatOccurrenceStore.findByFormatDefinitionAndProducerProfile(
                    formatDefinition.getId(), producerProfile.getId());
            if (formatOccurrence == null)
                formatOccurrence = new FormatOccurrence(formatDefinition,0,producerProfile);
            formatOccurrence.setOccurrences(formatOccurrence.getOccurrences() + puidOccurrenceMap.get(puid));
            formatOccurrenceStore.save(formatOccurrence);
        }
    }

    @Inject
    public void setFormatDefinitionService(FormatDefinitionService formatDefinitionService) {
        this.formatDefinitionService = formatDefinitionService;
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
