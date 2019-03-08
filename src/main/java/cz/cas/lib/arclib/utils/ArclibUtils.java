package cz.cas.lib.arclib.utils;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cas.lib.arclib.domain.Batch;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestIssue;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.packages.FolderStructure;
import cz.cas.lib.arclib.domain.preservationPlanning.FormatDefinition;
import cz.cas.lib.arclib.exception.bpm.ConfigParserException;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.service.preservationPlanning.FormatDefinitionService;
import cz.cas.lib.core.util.Utils;
import org.apache.commons.io.FileUtils;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

public class ArclibUtils {

    public static final String ZIP_EXTENSION = ".zip";
    public static final String XML_EXTENSION = ".xml";
    public static final String SUMS_EXTENSION = ".sums";

    public static final String METS = "mets";
    public static final String OAIS_DC = "oai_dc";
    public static final String PREMIS = "premis";
    public static final String ARCLIB = "arclib";
    public static final String DCTERMS = "dcterms";
    public static final String DC = "dc";

    /**
     * Looks for a specified text node and returns its value as an enum equivalent.
     * <p>
     * Node value is converted to uppercase before enum conversion attempt.
     * </p>
     *
     * @param root             root node from which to search
     * @param jsonPtrExpr      expression for node lookup
     * @param enumerationClass enum class used as equivalent to a node value
     * @return enum equivalent of a specified node
     * @throws ConfigParserException if there is no enum equivalent found
     */
    public static <T extends Enum<T>> T parseEnumFromConfig(JsonNode root, String jsonPtrExpr, Class<T> enumerationClass)
            throws ConfigParserException {
        JsonNode node = root.at(jsonPtrExpr);
        if (!node.isMissingNode()) {
            String value = node.textValue();
            for (Enum enumeration : enumerationClass.getEnumConstants()) {
                if (enumeration.toString().equals(value.toUpperCase()))
                    return enumeration.valueOf(enumerationClass, value.toUpperCase());
            }
        }
        throw new ConfigParserException(jsonPtrExpr, node.toString(), enumerationClass);
    }

    public static String toIncidentConfigVariableName(String incidentId) {
        return incidentId + "#config";
    }

    /**
     * deployment name has to start with a letter, it is injected into .bpmn file and used later during process instance startup
     *
     * @param batchId
     * @return
     */
    public static String toBatchDeploymentName(String batchId) {
        return "batch_" + batchId;
    }

    /**
     * Computes path to the zip file with the SIP content
     *
     * @param ingestWorkflow ingest workflow
     * @return computed path
     */
    public static Path getSipZipTransferAreaPath(IngestWorkflow ingestWorkflow) {
        Batch batch = ingestWorkflow.getBatch();
        return Paths.get(batch.getTransferAreaPath(), ingestWorkflow.getFileName());
    }

    /**
     * Computes path to the folder in the transfer area where the SIP of the ingest workflow is located
     *
     * @param sipZipTransferAreaPath path to the zip file with the SIP content
     * @return computed path
     */
    public static Path getSipSumsTransferAreaPath(Path sipZipTransferAreaPath) {
        String sipSumsTransferAreaPathString = sipZipTransferAreaPath.toString()
                .replace(ZIP_EXTENSION, SUMS_EXTENSION);
        return Paths.get(sipSumsTransferAreaPathString);
    }

    /**
     * Computes path to folder belonging the ingest workflow in workspace:
     * parent folder for the extracted and zipped version of SIP
     *
     * @param externalId external id of the ingest workflow
     * @param workspace  path to workspace
     * @return computed path
     */
    public static Path getIngestWorkflowWorkspacePath(String externalId, String workspace) {
        return Paths.get(workspace, externalId);
    }

    /**
     * Computes path to zip with the SIP content of the ingest workflow at workspace
     *
     * @param externalId  external id of ingest workflow
     * @param workspace   path to workspace
     * @param zipFileName name of the zip file
     * @return computed path
     */
    public static Path getSipZipWorkspacePath(String externalId, String workspace, String zipFileName) {
        return Paths.get(workspace, externalId, zipFileName);
    }

    /**
     * Converts set of file paths to tree folder structure
     *
     * @param filePats paths to files
     * @param rootName name of the root of the tree folder structure
     * @return generated folder structure
     */
    public static FolderStructure filePathsToFolderStructure(List<String> filePats, String rootName) {
        FolderStructure rootFolderStructure = new FolderStructure(null, rootName);

        filePats.forEach(filePath -> {
            FolderStructure folderStructure = rootFolderStructure;
            String childPath = filePath;
            int indexOfSlash = filePath.indexOf("/");

            while (indexOfSlash != -1) {
                String localRootName = childPath.substring(0, indexOfSlash);
                childPath = childPath.substring(indexOfSlash + 1, childPath.length());

                FolderStructure localRoot = new FolderStructure(null, localRootName);
                folderStructure = folderStructure.addChildIfNotExists(localRoot);
                indexOfSlash = childPath.indexOf("/");
            }
            FolderStructure child = new FolderStructure(null, childPath);
            folderStructure.addChildIfNotExists(child);
        });
        return rootFolderStructure;
    }

    /**
     * Parses boolean config
     * <p>
     * examples:
     * </p>
     * <ol>
     * <li>the config entry to be parsed is not contained in the config -> method returns Pair<null,text> where text
     * contains the information that the config value is missing</li>
     * <li>the config entry to be parsed is present and contains valid boolean value -> method returns Pair<value,text>
     * where the value=value parsed from config entry and text contains information that the parsed value from the config entry was used to solve the issue</li>
     * </ol>
     *
     * @param configRoot root of JSON config
     * @param configPath path to config field
     * @return config value together with string to be used in {@link IngestIssue#description} if this parsing was done to solve an issue
     */
    public static Utils.Pair<Boolean, String> parseBooleanConfig(JsonNode configRoot, String configPath) {
        JsonNode configValue = configRoot.at(configPath);
        String stringValue = configValue.toString();
        if (configValue.isMissingNode()) {
            return new Utils.Pair<>(null, IngestIssue.createMissingConfigNote(configPath));
        }
        if (!configValue.isBoolean()) {
            return new Utils.Pair<>(null, IngestIssue.createInvalidConfigNote(configPath, stringValue, "true", "false"));
        }
        return new Utils.Pair<>(configValue.booleanValue(), IngestIssue.createUsedConfigNote(configPath, stringValue));
    }

    public static boolean hasRole(UserDetails userDetails, String permission) {
        return userDetails.getAuthorities().contains(new SimpleGrantedAuthority(permission));
    }

    /**
     * Lists file paths and file sizes for all files of the SIP package
     *
     * @param pathToSipFolder path to the folder with the sip content
     * @return list of pairs of a file path and file size
     */
    public static List<Utils.Pair<String, String>> listSipFilePathsAndFileSizes(Path pathToSipFolder) {
        Collection<File> files = FileUtils.listFiles(pathToSipFolder.toFile(), null, true);
        return files.stream().map(file -> new Utils.Pair<>(
                (file.toPath().toUri()).toString().replaceAll(pathToSipFolder.toUri().toString(), ""),
                String.valueOf(file.length())))
                .collect(Collectors.toList());
    }

    /**
     * Prints the 'pretty' version of the XML document (including white spaces at the beginning of line for indentation)
     *
     * @param doc document to be printed
     * @return 'pretty' version of the document
     * @throws IOException
     */
    public static String prettyPrint(Document doc) throws IOException {
        OutputFormat format = OutputFormat.createPrettyPrint();
        StringWriter outputWriter = new StringWriter();
        XMLWriter writer = new XMLWriter(outputWriter, format);
        writer.write(doc);
        outputWriter.close();
        writer.close();
        return outputWriter.toString();
    }

    /**
     * Aggregates the collection of pairs of values with respect to the unique combinations of the two values
     * and computes the aggregated count
     *
     * @param collection collection of pairs of values
     * @return map of aggregated values where the key is the pair and value is the aggregated count
     */
    public static Map<Utils.Pair<String, String>, Long> computeAggregatedCount(Collection<Utils.Pair<String, String>> collection) {
        return collection.stream().collect(groupingBy(Function.identity(), Collectors.counting()));
    }

    /**
     * Gets file name of the exported AIP
     *
     * @param aipId id of the AIP to export
     * @return
     */
    public static String getAipExportName(String aipId) {
        return aipId + ZIP_EXTENSION;
    }

    /**
     * Gets file name of the exported XML
     *
     * @param aipId   id of the AIP of XML to export
     * @param version version of the XML to export
     * @return
     */
    public static String getXmlExportName(String aipId, Integer version) {
        String versionSuffix = version != null ? String.valueOf(version) : "latest";
        return aipId + "_xml_" + versionSuffix + XML_EXTENSION;
    }

    public static String enumLabelToEnumName(String label) {
        return label.toUpperCase()
                .replace(" ", "_")
                .replace("(", "")
                .replace(")", "")
                .replace("-", "");
    }

    /**
     * returns relative path of file in sip and its corresponding format entity
     *
     * @param wsSipPath
     * @param filePath
     * @param formatIdentificationResult
     * @param formatDefinitionService
     * @return
     */
    public static Utils.Pair<String, FormatDefinition> findFormat(Path wsSipPath, Path filePath, Map<String,
            Utils.Pair<String, String>> formatIdentificationResult, FormatDefinitionService formatDefinitionService) {
        String pathToSipStr = wsSipPath.toAbsolutePath().toString().replace("\\", "/");
        String fileRelativePath = filePath.toAbsolutePath().toString().replace("\\", "/").replace(pathToSipStr, "");
        FormatDefinition formatDefinition;
        Utils.Pair<String, String> fileIdentification = formatIdentificationResult.get("file:/" + fileRelativePath);
        if (fileIdentification == null)
            formatDefinition = null;
        else
            formatDefinition = formatDefinitionService.findPreferredDefinitionsByPuid(fileIdentification.getL());
        return new Utils.Pair<>(fileRelativePath, formatDefinition);
    }
}
