package cz.cas.lib.arclib.service.formatIdentification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestIssue;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.exception.bpm.ConfigParserException;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import cz.cas.lib.arclib.service.formatIdentification.droid.CsvResultColumn;
import cz.cas.lib.arclib.service.formatIdentification.droid.DroidFormatIdentificationTool;
import cz.cas.lib.arclib.store.IngestIssueStore;
import cz.cas.lib.arclib.store.IngestWorkflowStore;
import cz.cas.lib.core.exception.GeneralException;
import cz.cas.lib.core.store.Transactional;
import cz.cas.lib.core.util.Utils;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cz.cas.lib.arclib.utils.ArclibUtils.parseEnumFromConfig;

public abstract class FormatIdentificationTool {
    private IngestIssueStore ingestIssueStore;
    private IngestWorkflowStore ingestWorkflowStore;

    public static final String FORMAT_IDENTIFIER_EXPR = "/formatIdentification";
    public static final String PATHS_AND_FORMATS_EXPR = "/pathsAndFormats";
    public static final String IDENTIFIER_TYPE_EXPR = "/type";
    public static final String PARSED_COLUMN_EXPR = "/parsedColumn";
    public static final String FILE_PATH_EXPR = "filePath";
    public static final String FORMAT_EXPR = "format";

    public static final String EXPECTED_JSON_INPUT = "List of pairs of paths to files and their respective file formats, e.g. " +
            "[ {\"filePath\":\"this/is/a/filepath\", \"format\":\"fmt/101\"}," +
            " {\"filePath\":\"this/is/another/filepath\", \"format\":\"fmt/993\"} ]";

    public static final String DROID_FORMAT_IDENTIFIER_NAME = "DROID format identifier";

    /**
     * Performs the format identification analysis for all the files belonging the SIP package
     *
     * @param pathToSip path to the SIP to analyze
     * @return map of key-value pairs where the key is the path to a file from SIP and
     * value is a list of pairs of the format that has been identified for the file and
     * the respective identification method
     * @throws IOException if the SIP is not found
     */
    public abstract Map<String, List<Utils.Pair<String, String>>> analyze(Path pathToSip) throws IOException;

    /**
     * Returns a string containing the version of format identifier together with the versions of the signature files.
     *
     * @return string with the format identifier version
     */
    public abstract String getToolVersion();

    /**
     * Returns a string with the name of the format identifier.
     *
     * @return name of the format identifier
     */
    public abstract String getToolName();

    /**
     * Create and initialize the format identifier using the JSON configuration
     *
     * @param root JSON config
     * @return instance of {@link FormatIdentificationTool} implemented according to the JSON configuration
     * @throws ConfigParserException JSON configuration is missing the necessary attributes for the configuration
     *                               of {@link FormatIdentificationTool}
     */
    public static FormatIdentificationTool initialize(JsonNode root) throws ConfigParserException {
        FormatIdentificationToolType formatIdentificationToolType = parseEnumFromConfig(root, FORMAT_IDENTIFIER_EXPR + IDENTIFIER_TYPE_EXPR,
                FormatIdentificationToolType.class);
        switch (formatIdentificationToolType) {
            case DROID:
                CsvResultColumn parsedColumn = parseEnumFromConfig(root, FORMAT_IDENTIFIER_EXPR + PARSED_COLUMN_EXPR,
                        CsvResultColumn.class);
                return new DroidFormatIdentificationTool(parsedColumn);
            default:
                throw new GeneralException("unexpected format identifier configuration error");
        }
    }

    /**
     * Resolve the format ambiguity with the predefined values for the files that have been identified with multiple
     * formats.
     *
     * @param configRoot        root node of the JSON ingest workflow config
     * @param identifiedFormats map of file paths to lists of identified formats
     * @param externalId        external id of the ingest workflow
     * @return map of file paths to a format together with its identification method
     */
    public TreeMap<String, Utils.Pair<String, String>> resolveAmbiguousIdentifications(
            Map<String, List<Utils.Pair<String, String>>> identifiedFormats, JsonNode configRoot, String externalId)
            throws IncidentException {
        //list of paths to formats with predefined values to help to resolve the format identification ambiguity
        List<Utils.Pair<String, String>> predefinedFormats = parsePathsToFormats(configRoot, externalId);

        //map of files to lists of formats for which it was unable to unambiguously determine the format
        TreeMap<String, List<Utils.Pair<String, String>>> unresolvableFormats = new TreeMap<>();

        //map of files to formats without ambiguity
        TreeMap<String, Utils.Pair<String, String>> resultingFormats = new TreeMap<>();

        identifiedFormats.entrySet().stream()
                .forEach(pathToListOfFormats -> {
                    Utils.Pair formatToIdentificationMethod = null;
                    String filePath = pathToListOfFormats.getKey();
                    List<Utils.Pair<String, String>> formatsAndIdentificationMethods = pathToListOfFormats.getValue();

                    if (formatsAndIdentificationMethods.size() == 1) {
                        //file has been identified with only a single format
                        formatToIdentificationMethod = formatsAndIdentificationMethods.get(0);
                    } else {
                        //file has been identified with multiple formats
                        List<Utils.Pair> matchingPatterns = predefinedFormats.stream()
                                .filter(filePathRegexAndFormat -> Pattern.compile(filePathRegexAndFormat.getL()).matcher(filePath).matches())
                                .collect(Collectors.toList());
                        if (matchingPatterns.size() > 0) {
                            Utils.Pair filePathRegexAndFormat = matchingPatterns.get(0);
                            formatToIdentificationMethod = new Utils.Pair(filePathRegexAndFormat.getR(),
                                    "Manually determined format as defined for the files located at the path" +
                                            " matching regex: " + filePathRegexAndFormat.getL());
                            //recreating the config value as it was passed in the JSON config
                            String configValue = "{\"" + FILE_PATH_EXPR + "\":\"" + filePathRegexAndFormat.getL() +
                                    "\", \"" + FORMAT_EXPR + "\":\"" + filePathRegexAndFormat.getR() + "\"}";
                            invokeFormatResolvedByConfigIssue(FORMAT_IDENTIFIER_EXPR + PATHS_AND_FORMATS_EXPR,
                                    configValue, externalId);
                        } else {
                            //it was unable to determine the format using the predefined values
                            unresolvableFormats.put(filePath, formatsAndIdentificationMethods);
                        }
                    }
                    resultingFormats.put(filePath, formatToIdentificationMethod);
                });

        if (!unresolvableFormats.isEmpty()) {
            invokeUnresolvableFormatsIssue(unresolvableFormats, externalId);
        }
        return resultingFormats;
    }

    /**
     * Parse list of predefined pairs of a file path and a file format from the JSON config
     *
     * @param root       JSON config
     * @param externalId external id of the ingest workflow
     * @return list parsed out pairs of paths and formats
     * @throws ConfigParserException JSON configuration is invalid
     */
    public List<Utils.Pair<String, String>> parsePathsToFormats(JsonNode root, String externalId) throws IncidentException {
        ObjectMapper om = new ObjectMapper();
        JsonNode pathsToFormatsNode = root.at(FORMAT_IDENTIFIER_EXPR + PATHS_AND_FORMATS_EXPR);

        if (pathsToFormatsNode.getNodeType() != JsonNodeType.ARRAY)
            invokeInvalidConfigIssue(FORMAT_IDENTIFIER_EXPR + PATHS_AND_FORMATS_EXPR, pathsToFormatsNode.toString(), externalId);
        List<LinkedHashMap> list = om.convertValue(pathsToFormatsNode, List.class);

        List<Utils.Pair<String, String>> result = new ArrayList<>();
        for (LinkedHashMap pathToFormat : list) {
            String filePath = (String) pathToFormat.get(FILE_PATH_EXPR);
            String format = (String) pathToFormat.get(FORMAT_EXPR);
            if (filePath == null || format == null) {
                invokeInvalidConfigIssue(FORMAT_IDENTIFIER_EXPR + PATHS_AND_FORMATS_EXPR, pathsToFormatsNode.toString(), externalId);
            }
            result.add(new Utils.Pair<>(filePath, format));
        }
        return result;
    }

    @Transactional
    private void invokeInvalidConfigIssue(String configPath, String nodeValue, String getExternalId)
            throws IncidentException {
        IngestWorkflow ingestWorkflow = ingestWorkflowStore.findByExternalId(getExternalId);

        IngestIssue issue = new IngestIssue(ingestWorkflow,
                BpmConstants.FormatIdentification.class,
                "Invalid config."
        );
        issue.setSolvedByConfig(false);
        issue.setInvalidConfigNote(configPath, nodeValue, EXPECTED_JSON_INPUT);
        ingestIssueStore.save(issue);
        throw new IncidentException(issue);
    }

    @Transactional
    private void invokeFormatResolvedByConfigIssue(String configPath, String configValue,
                                                   String getExternalId) {
        IngestWorkflow ingestWorkflow = ingestWorkflowStore.findByExternalId(getExternalId);
        IngestIssue issue = new IngestIssue(
                ingestWorkflow,
                BpmConstants.FormatIdentification.class,
                "Ambiguous format resolved by config.",
                true,
                ""
        );
        issue.setUsedConfigNote(configPath, configValue);

        ingestIssueStore.save(issue);
    }

    @Transactional
    private void invokeUnresolvableFormatsIssue(TreeMap<String, List<Utils.Pair<String, String>>> ambiguousFormats,
                                                String getExternalId) throws IncidentException {
        StringBuilder sb = new StringBuilder();
        ambiguousFormats.entrySet().stream()
                .forEach(item -> {
                    sb.append("File at path: " + item.getKey() + " was identified with multiple formats: {\n");
                    List<Utils.Pair<String, String>> formats = item.getValue();
                    formats.forEach(pair ->
                            sb.append("format: " + pair.getL() + ", identification method: " + pair.getR() + "\n"));
                });
        sb.append("}");

        IngestWorkflow ingestWorkflow = ingestWorkflowStore.findByExternalId(getExternalId);
        IngestIssue issue = new IngestIssue(
                ingestWorkflow,
                BpmConstants.FormatIdentification.class,
                sb.toString(),
                false,
                ""
        );
        ingestIssueStore.save(issue);
        throw new IncidentException(issue);
    }

    @Inject
    public void setIngestWorkflowStore(IngestWorkflowStore ingestWorkflowStore) {
        this.ingestWorkflowStore = ingestWorkflowStore;
    }

    @Inject
    public void setIngestIssueStore(IngestIssueStore ingestIssueStore) {
        this.ingestIssueStore = ingestIssueStore;
    }
}
