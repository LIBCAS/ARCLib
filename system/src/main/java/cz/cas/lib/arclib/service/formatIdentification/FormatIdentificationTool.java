package cz.cas.lib.arclib.service.formatIdentification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import cz.cas.lib.arclib.bpm.IngestTool;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestIssue;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.preservationPlanning.IngestIssueDefinitionCode;
import cz.cas.lib.arclib.domain.preservationPlanning.Tool;
import cz.cas.lib.arclib.exception.bpm.ConfigParserException;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import cz.cas.lib.arclib.formatlibrary.domain.FormatDefinition;
import cz.cas.lib.arclib.formatlibrary.service.FormatDefinitionService;
import cz.cas.lib.arclib.service.IngestIssueService;
import cz.cas.lib.arclib.store.IngestIssueDefinitionStore;
import cz.cas.lib.core.store.Transactional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public abstract class FormatIdentificationTool implements IngestTool {
    private IngestWorkflow ingestWorkflow;
    private IngestIssueDefinitionStore ingestIssueDefinitionStore;
    private FormatDefinitionService formatDefinitionService;
    private IngestIssueService ingestIssueService;
    @Getter
    private Tool toolEntity;
    private int formatIdentificationToolCounter;

    public static final String FORMAT_IDENTIFICATON_TOOL_EXPR = "/formatIdentification";
    public static final String PATHS_AND_FORMATS_EXPR = "/pathsAndFormats";
    public static final String IDENTIFIER_TYPE_EXPR = "/type";
    public static final String FILE_PATH_EXPR = "filePath";
    public static final String FORMAT_EXPR = "format";

    public static final String EXPECTED_JSON_INPUT = "List of pairs of paths to files and their respective file formats, e.g. " +
            "{\"0\":{\"filePath\":\"this/is/a/filepath\", \"format\":\"fmt/101\"}," +
            " \"1\":{\"filePath\":\"this/is/another/filepath\", \"format\":\"fmt/993\"}}";

    /**
     * Performs the format identification analysis for all the files belonging the SIP package
     *
     * @param pathToSip path to the SIP to analyze
     * @return map of key-value pairs where the key is the path to a file from SIP and
     * value is a list of pairs of the format that has been identified for the file and
     * the respective identification method.. <b>value can be null if the format was not identified</b>
     * @throws IOException if the SIP is not found
     */
    public abstract Map<String, List<Pair<String, String>>> analyze(Path pathToSip) throws IOException;

    /**
     * Resolve the format ambiguity with the predefined values for the files that have been identified with multiple
     * formats.
     *
     * @param configRoot        root node of the JSON ingest workflow config
     * @param identifiedFormats map of file paths to lists of identified formats
     * @param externalId        external id of the ingest workflow
     * @return map of file paths to a format together with its identification method
     */
    public TreeMap<String, Pair<String, String>> resolveAmbiguousIdentifications(
            Map<String, List<Pair<String, String>>> identifiedFormats, JsonNode configRoot, String externalId)
            throws IncidentException {
        //list of paths and formats with predefined values to help to resolve the format identification ambiguity
        List<Pair<String, String>> predefinedFormats = parsePathsAndFormats(configRoot);

        //map of files to lists of formats for which it was unable to unambiguously determine the format
        TreeMap<String, List<Pair<String, String>>> unresolvableFormats = new TreeMap<>();

        //map of files to formats without ambiguity
        TreeMap<String, Pair<String, String>> resultingFormats = new TreeMap<>();

        identifiedFormats.entrySet().stream()
                .forEach(pathToListOfFormats -> {
                    Pair<String, String> formatToIdentificationMethod = null;
                    String filePath = pathToListOfFormats.getKey();
                    List<Pair<String, String>> formatsAndIdentificationMethods = pathToListOfFormats.getValue();

                    if (formatsAndIdentificationMethods != null && formatsAndIdentificationMethods.size() == 1) {
                        //file has been identified with only a single format
                        formatToIdentificationMethod = formatsAndIdentificationMethods.get(0);
                    } else {
                        //file has been identified with multiple formats or was not identified at all
                        log.debug("Resolving ambiguous format identification for file at path " + filePath + ".");
                        List<Pair<String, String>> matchingPatterns = predefinedFormats.stream()
                                .filter(filePathRegexAndFormat -> Pattern.compile(filePathRegexAndFormat.getLeft()).matcher(filePath).matches())
                                .collect(Collectors.toList());
                        if (matchingPatterns.size() > 0) {
                            Pair<String, String> filePathRegexAndFormat = matchingPatterns.get(0);
                            formatToIdentificationMethod = Pair.of(filePathRegexAndFormat.getRight(),
                                    "Manually determined format as defined for the files located at the path" +
                                            " matching regex: " + filePathRegexAndFormat.getLeft());
                            //recreating the config value as it was passed in the JSON config
                            String configValue = "{\"" + FILE_PATH_EXPR + "\":\"" + filePathRegexAndFormat.getLeft() +
                                    "\", \"" + FORMAT_EXPR + "\":\"" + filePathRegexAndFormat.getRight() + "\"}";
                            FormatDefinition formatDefinitionEntity = formatDefinitionService.findPreferredDefinitionsByPuid(formatToIdentificationMethod.getLeft());
                            invokeFormatResolvedByConfigIssue(FORMAT_IDENTIFICATON_TOOL_EXPR +  "/" + formatIdentificationToolCounter + PATHS_AND_FORMATS_EXPR,
                                    configValue, externalId, formatDefinitionEntity);
                            log.debug("Ambiguous format identification for file at path " + filePath + " resolved successfully.");
                        } else {
                            //it was unable to determine the format using the predefined values
                            unresolvableFormats.put(filePath, formatsAndIdentificationMethods);
                            log.warn("Unable to unambiguously resolve format for file at path " + filePath + ".");
                        }
                    }
                    resultingFormats.put(filePath, formatToIdentificationMethod);
                });

        if (!unresolvableFormats.isEmpty()) invokeUnresolvableFormatsIssue(unresolvableFormats, externalId);
        return resultingFormats;
    }

    /**
     * Parse list of predefined pairs of a file path and a file format from the JSON config
     *
     * @param root JSON config
     * @return list parsed out pairs of paths and formats
     * @throws ConfigParserException JSON configuration is invalid
     */
    public List<Pair<String, String>> parsePathsAndFormats(JsonNode root) throws IncidentException {
        ObjectMapper om = new ObjectMapper();
        List<Pair<String, String>> result = new ArrayList<>();

        JsonNode pathsToFormatsNode = root.at(FORMAT_IDENTIFICATON_TOOL_EXPR + "/" + formatIdentificationToolCounter + PATHS_AND_FORMATS_EXPR);
        if (pathsToFormatsNode.isMissingNode())
            return result;

        if (pathsToFormatsNode.getNodeType() != JsonNodeType.OBJECT)
            throw new ConfigParserException(FORMAT_IDENTIFICATON_TOOL_EXPR + "/" + formatIdentificationToolCounter + PATHS_AND_FORMATS_EXPR, pathsToFormatsNode.toString(), EXPECTED_JSON_INPUT);

        Map<String, Map> list = om.convertValue(pathsToFormatsNode, Map.class);

        for (Map pathToFormat : list.values()) {
            String filePath = (String) pathToFormat.get(FILE_PATH_EXPR);
            String format = (String) pathToFormat.get(FORMAT_EXPR);
            if (filePath == null || format == null) {
                throw new ConfigParserException(FORMAT_IDENTIFICATON_TOOL_EXPR + "/" + formatIdentificationToolCounter + PATHS_AND_FORMATS_EXPR, pathsToFormatsNode.toString(), EXPECTED_JSON_INPUT);
            }
            result.add(Pair.of(filePath, format));
        }
        return result;
    }

    @Transactional
    private void invokeFormatResolvedByConfigIssue(String configPath, String configValue, String externalId, FormatDefinition formatDefinition) {
        log.debug("Invoking format resolved by config issue for ingest workflow " + externalId + ".");

        IngestIssue issue = new IngestIssue(
                ingestWorkflow,
                toolEntity,
                ingestIssueDefinitionStore.findByCode(IngestIssueDefinitionCode.FILE_FORMAT_RESOLVED_BY_CONFIG),
                formatDefinition,
                IngestIssue.createUsedConfigNote(configPath, configValue),
                true
        );
        ingestIssueService.save(issue);
    }

    @Transactional
    private void invokeUnresolvableFormatsIssue(TreeMap<String, List<Pair<String, String>>> ambiguousFormats,
                                                String externalId) throws IncidentException {
        log.warn("Invoking unresolvable formats issue for ingest workflow " + externalId + ".");
        List<IngestIssue> issues = new ArrayList<>();

        ambiguousFormats.forEach((key, formats) -> {
            StringBuilder oneFileSb = new StringBuilder();
            if (formats == null) {
                oneFileSb.append("Format of file at path: ").append(key).append(" was not identified\n");
                issues.add(new IngestIssue(
                        ingestWorkflow,
                        toolEntity,
                        ingestIssueDefinitionStore.findByCode(IngestIssueDefinitionCode.FILE_FORMAT_UNRESOLVABLE),
                        null,
                        oneFileSb.toString(),
                        false
                ));
            } else {
                oneFileSb.append("File at path: ").append(key).append(" was identified with multiple formats: {\n");
                formats.forEach(pair -> {
                    oneFileSb.append("format: ").append(pair.getLeft()).append(", identification method: ").append(pair.getRight()).append("\n");
                });
                oneFileSb.append("}\n");
                formats.forEach(pair -> {
                    issues.add(new IngestIssue(
                            ingestWorkflow,
                            toolEntity,
                            ingestIssueDefinitionStore.findByCode(IngestIssueDefinitionCode.FILE_FORMAT_UNRESOLVABLE),
                            formatDefinitionService.findPreferredDefinitionsByPuid(pair.getLeft()),
                            oneFileSb.toString(),
                            false
                    ));
                });
            }
        });
        throw new IncidentException(issues);
    }

    public void inject(FormatDefinitionService formatDefinitionService,
                       IngestIssueService ingestIssueService,
                       IngestIssueDefinitionStore ingestIssueDefinitionStore,
                       IngestWorkflow iw,
                       Tool tool,
                       int formatIdentificationToolCounter
    ) {
        this.ingestWorkflow = iw;
        this.formatDefinitionService = formatDefinitionService;
        this.ingestIssueDefinitionStore = ingestIssueDefinitionStore;
        this.ingestIssueService = ingestIssueService;
        this.toolEntity = tool;
        this.formatIdentificationToolCounter = formatIdentificationToolCounter;
    }
}
