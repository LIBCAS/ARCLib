package cz.cas.lib.arclib.formatlibrary.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.formatlibrary.domain.*;
import cz.cas.lib.arclib.formatlibrary.util.FormatLibraryUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.dom4j.*;
import org.dom4j.io.SAXReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static cz.cas.lib.arclib.domainbase.util.DomainBaseUtils.eq;
import static cz.cas.lib.arclib.domainbase.util.DomainBaseUtils.notNull;

@Slf4j
@Service
public class FormatLibraryUpdater {

    public static final String PATH_TO_FORMAT_IDS = "/PRONOM-Report/pronom:report_filetypes_detail/pronom:FileType/pronom:FormatID";
    public static final String PATH_TO_FILE_FORMAT = "/PRONOM-Report/pronom:report_format_detail/pronom:FileFormat";
    public static final String DATE_PATTERN = "dd MMM yyyy";
    public static final String PRONOM_NAMESPACE_URI = "http://pronom.nationalarchives.gov.uk";

    private String formatListUrl;
    private String formatDetailListUrl;

    private ObjectMapper objectMapper;
    private FormatService formatService;
    private FormatDefinitionService formatDefinitionService;
    private FormatDeveloperService formatDeveloperService;
    private FormatIdentifierService formatIdentifierService;
    private UserDetails userDetails;
    private Optional<FormatLibraryNotifier> formatLibraryNotifier;

    /**
     * Downloads format detail from the PRONOM server
     *
     * @param formatId id of the format to download
     * @return response entity from the PRONOM server, in case of successful response
     * the body contains XML with the format detail
     */
    public ResponseEntity<String> downloadFormatDetail(Integer formatId) {
        log.debug("Downloading format detail from PRONOM server for format with format id " + formatId + ".");

        MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();

        map.add("strAction", "Save As XML");
        map.add("strFileFormatID", String.valueOf(formatId));

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, headers);
        return restTemplate.exchange(formatListUrl, HttpMethod.POST, requestEntity, String.class);
    }

    /**
     * Downloads list of formats from the PRONOM server
     *
     * @return response entity from the PRONOM server, in case of successful response
     * the body contains XML with the list of formats
     */
    public ResponseEntity<String> downloadListOfFormats() {
        log.debug("Downloading list of formats from PRONOM server.");

        MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();

        map.add("strAction", "Save As XML");
        map.add("strLastQueryStatus", "formatname");
        map.add("strLastQueryExtension", "");
        map.add("strLastQueryFileExtension", "");
        map.add("strFileFormatName", "");
        map.add("strLastQueryFormatName", "");
        map.add("strOrderBy", "order_extension");

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, headers);
        return restTemplate.exchange(formatDetailListUrl, HttpMethod.POST, requestEntity, String.class);
    }

    /**
     * Gets list of format ids of formats from the PRONOM server
     *
     * @return list of format ids for all formats in the PRONOM server
     * @throws DocumentException if the input from the PRONOM server could not be parsed
     */
    public List<Integer> getListOfFormatIdsFromExternal() throws DocumentException {
        ResponseEntity<String> response = downloadListOfFormats();
        if (response.getStatusCode().is2xxSuccessful()) {
            log.debug("Successfully retrieved list of formats from PRONOM server.");
            String listOfFormats = response.getBody();

            SAXReader reader = new SAXReader();
            Document doc = reader.read(new ByteArrayInputStream(listOfFormats.getBytes(StandardCharsets.UTF_8)));

            XPath formatIdXPath = doc.createXPath(PATH_TO_FORMAT_IDS);
            formatIdXPath.setNamespaceURIs(Collections.singletonMap("pronom", PRONOM_NAMESPACE_URI));

            List<Element> formatIDElements = new ArrayList<>();
            List<Node> nodes = formatIdXPath.selectNodes(doc);
            for (Node node : nodes) {
                formatIDElements.add((Element) node);
            }
            return formatIDElements.stream().map(element -> Integer.valueOf(element.getText())).collect(Collectors.toList());
        } else {
            String message = "Retrieval of list of formats from PRONOM server failed. Error code: " +
                    response.getStatusCodeValue() + ", reason: " + response.getBody() + ".";
            log.error(message);
            throw new GeneralException(message);
        }
    }

    /**
     * Gets format definition entity for the specified format id filled with values retrieved from the PRONOM server,
     * if the respective format entity does not exist yet, it is created and saved to database
     *
     * @param formatId id of the format to retrieve from the PRONOM server
     * @return format definition entity filled with the values from the PRONOM server
     * @throws DocumentException if the input from the PRONOM server could not be parsed
     * @throws ParseException    could not parse date attribute
     */
    @Transactional
    public FormatDefinition getFormatDefinitionFromExternal(Integer formatId) throws DocumentException, ParseException {
        SAXReader reader = new SAXReader();

        ResponseEntity<String> response = downloadFormatDetail(formatId);
        if (response.getStatusCode().is2xxSuccessful()) {
            log.debug("Successfully retrieved format detail for format with formatID " + formatId +
                    " from PRONOM server.");
            String formatDetail = response.getBody();

            Document doc = reader.read(new ByteArrayInputStream(formatDetail.getBytes(StandardCharsets.UTF_8)));

            XPath fileFormatXPath = doc.createXPath(PATH_TO_FILE_FORMAT);
            fileFormatXPath.setNamespaceURIs(Collections.singletonMap("pronom", PRONOM_NAMESPACE_URI));
            Element fileFormatElement = (Element) fileFormatXPath.selectNodes(doc).get(0);
            List<Element> formatAttributesElements = fileFormatElement.elements();

            Format format = formatService.findByFormatId(formatId);
            boolean newFormatCreated = false;
            if (format == null) {
                newFormatCreated = true;
                format = new Format();
                format.setFormatId(formatId);
            }

            FormatDefinition formatDefinition = new FormatDefinition();
            formatDefinition.setLocalDefinition(false);
            formatDefinition.setFormat(format);

            for (Element elem : formatAttributesElements) {
                String attributeName = elem.getName();
                String attributeValue = elem.getText().trim();
                FormatItem formatItem;
                try {
                    formatItem = FormatItem.valueOf(attributeName);
                } catch (IllegalArgumentException e) {
                    formatLibraryNotifier.ifPresent(formatLibraryNotifier1 -> formatLibraryNotifier1.sendUnsupportedPronomValueNotification(userDetails.getUsername(), e.getMessage(), Instant.now()));
                    throw e;
                }

                switch (formatItem) {
                    case FormatAliases:
                        if (!attributeValue.trim().isEmpty()) {
                            String[] formatAliases = attributeValue.split(", ");
                            formatDefinition.setAliases(Arrays.stream(formatAliases).collect(Collectors.toSet()));
                        } else {
                            formatDefinition.setAliases(new HashSet<>());
                        }
                        break;
                    case FormatTypes: {
                        String[] formatTypes = attributeValue.split(", ");
                        Set<FormatClassification> parsedFormatClassifications = new HashSet<>();

                        if (!attributeValue.isEmpty()) {
                            for (String formatType : formatTypes) {
                                try {
                                    FormatClassification formatClassification = FormatClassification.valueOf(
                                            enumLabelToEnumName(formatType));
                                    parsedFormatClassifications.add(formatClassification);
                                } catch (IllegalArgumentException e) {
                                    formatLibraryNotifier.ifPresent(formatLibraryNotifier1 -> formatLibraryNotifier1.sendUnsupportedPronomValueNotification(userDetails.getUsername(), e.getMessage(), Instant.now()));
                                    throw e;
                                }
                            }
                        }

                        Set<FormatClassification> formatClassifications = formatDefinition.getFormatClassifications();
                        if (formatClassifications == null) formatClassifications = new HashSet<>();
                        formatClassifications.addAll(parsedFormatClassifications);
                        formatDefinition.setFormatClassifications(formatClassifications);
                    }
                    break;
                    case Developers:
                        Element developerIdElement = elem.element("DeveloperID");
                        Element developerNameElement = elem.element("DeveloperName");
                        Element organisationNameElement = elem.element("OrganisationName");
                        Element developerCompoundNameElement = elem.element("DeveloperCompoundName");

                        FormatDeveloper formatDeveloper = new FormatDeveloper();
                        formatDeveloper.setDeveloperId(Integer.valueOf(developerIdElement.getText()));
                        formatDeveloper.setDeveloperName(developerNameElement.getText());
                        formatDeveloper.setOrganisationName(organisationNameElement.getText());
                        formatDeveloper.setDeveloperCompoundName(developerCompoundNameElement.getText());
                        formatDeveloper = formatDeveloperService.updateFromExternal(formatDeveloper);
                        formatDefinition.getDevelopers().add(formatDeveloper);
                        break;
                    case FormatID:
                        eq(Integer.valueOf(attributeValue), formatId, () -> new GeneralException("Value of attribute " +
                                FormatItem.FormatID + " does not match the expected value."));
                        break;
                    case FormatDescription:
                        formatDefinition.setFormatDescription(attributeValue);
                        break;
                    case FormatName:
                        format.setFormatName(attributeValue);
                        break;
                    case FormatNote:
                        formatDefinition.setFormatNote(attributeValue);
                        break;
                    case FileFormatIdentifier:
                        Element identifierElement = elem.element("Identifier");
                        String identifier = identifierElement.getText();

                        Element identifierTypeElement = elem.element("IdentifierType");
                        String identifierTypeText = identifierTypeElement.getText();
                        FormatIdentifierType identifierType;
                        switch (identifierTypeText) {
                            case "4CC":
                                identifierType = FormatIdentifierType.FOUR_CC;
                                break;
                            case "UUID/GUID":
                                identifierType = FormatIdentifierType.UUID_GUID;
                                break;
                            default:
                                try {
                                    identifierType = FormatIdentifierType.valueOf(enumLabelToEnumName(identifierTypeText));
                                } catch (IllegalArgumentException e) {
                                    formatLibraryNotifier.ifPresent(formatLibraryNotifier1 -> formatLibraryNotifier1.sendUnsupportedPronomValueNotification(userDetails.getUsername(), e.getMessage(), Instant.now()));
                                    throw e;
                                }
                        }

                        FormatIdentifier formatIdentifier = formatIdentifierService.findByIdentifierTypeAndIdentifier(
                                identifierType, identifier);
                        if (formatIdentifier == null) {
                            formatIdentifier = new FormatIdentifier();
                            formatIdentifier.setIdentifierType(identifierType);
                            formatIdentifier.setIdentifier(identifier);
                        }

                        if (formatIdentifier.getIdentifierType() == FormatIdentifierType.PUID)
                            format.setPuid(formatIdentifier.getIdentifier());

                        formatDefinition.getIdentifiers().add(formatIdentifier);
                        break;
                    case ReleaseDate:
                        if (!(attributeValue.isEmpty())) {
                            SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_PATTERN, Locale.UK);
                            Date parse = dateFormatter.parse(attributeValue);
                            formatDefinition.setReleaseDate(parse == null ? null : parse.toInstant());
                        }
                        break;
                    case FormatVersion:
                        formatDefinition.setFormatVersion(attributeValue);
                        break;
                    case WithdrawnDate:
                        if (!(attributeValue.isEmpty())) {
                            SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_PATTERN, Locale.UK);
                            Date parse = dateFormatter.parse(attributeValue);
                            formatDefinition.setWithdrawnDate(parse == null ? null : parse.toInstant());
                        }
                        break;
                    case FormatDisclosure:
                        break;
                    case BinaryFileFormat:
                        break;
                    case ProvenanceDescription:
                        break;
                    case Document:
                        break;
                    case CharacterEncoding:
                        break;
                    case FormatFamilies:
                        Set<String> formatFamilies = formatDefinition.getFormatFamilies();
                        if (formatFamilies == null) formatFamilies = new HashSet<>();
                        if (!attributeValue.isEmpty()) formatFamilies.add(attributeValue);
                        formatDefinition.setFormatFamilies(formatFamilies);
                        break;
                    case FormatRisk:
                        break;
                    case RelatedFormat:
                        Element relationshipTypeElement = elem.element("RelationshipType");
                        Element relatedFormatIdElement = elem.element("RelatedFormatID");

                        FormatRelationshipType formatRelationshipType;
                        String value = relationshipTypeElement.getText().toUpperCase().replace(" ", "_");
                        try {
                            formatRelationshipType = FormatRelationshipType.valueOf(value);
                        } catch (IllegalArgumentException e) {
                            formatLibraryNotifier.ifPresent(formatLibraryNotifier1 -> formatLibraryNotifier1.sendUnsupportedPronomValueNotification(userDetails.getUsername(), e.getMessage(), Instant.now()));
                            throw e;
                        }
                        Integer relatedFormatId = Integer.valueOf(relatedFormatIdElement.getText());

                        RelatedFormat relatedFormat = new RelatedFormat();
                        relatedFormat.setRelationshipType(formatRelationshipType);
                        relatedFormat.setRelatedFormatId(relatedFormatId);
                        relatedFormat.setFormatDefinition(formatDefinition);

                        formatDefinition.getRelatedFormats().add(relatedFormat);
                        break;
                    case Support:
                        break;
                    case InternalSignature:
                        break;
                    case TechnicalEnvironment:
                        break;
                    case RelatedFormats:
                        break;
                    case ProvenanceName:
                        break;
                    case ProvenanceSourceDate:
                        break;
                    case ByteOrders:
                        break;
                    case Apple:
                        break;
                    case FormatProperties:
                        break;
                    case CompressionType:
                        break;
                    case ReferenceFile:
                        break;
                    case LastUpdatedDate:
                        break;
                    case ExternalSignature:
                        break;
                    case ProvenanceSourceID:
                        break;
                    default:
                }
            }
            if (newFormatCreated)
                formatService.create(format);
            else
                formatService.update(format);
            return formatDefinition;
        } else {
            String message = "Retrieval of format with formatID " + formatId + " from PRONOM server failed." +
                    " Error code: " + response.getStatusCodeValue() + ", reason: " +
                    response.getBody() + ".";
            log.error(message);
            throw new GeneralException(message);
        }
    }

    /**
     * Updates format library with the definitions from the PRONOM server
     *
     * @throws DocumentException if the input from the PRONOM server could not be parsed
     * @throws ParseException    could not parse date attribute
     */
    @Transactional
    @Async
    public void updateFormatsFromExternal(String username) throws DocumentException, ParseException {
        List<Integer> listOfFormatIds = getListOfFormatIdsFromExternal();
        StringBuilder report = new StringBuilder();

        for (Integer formatId : listOfFormatIds) {
            Pair<FormatDefinition, String> updatedFormatDefinitionAndMessage = updateFormatFromExternal(formatId);
            String message = updatedFormatDefinitionAndMessage.getRight();
            report.append("Format with format id " + formatId + ": " + message + ".\n");
        }

        log.debug("Successfully updated all formats from external.");
        formatLibraryNotifier.ifPresent(formatLibraryNotifier1 -> formatLibraryNotifier1.sendFormatLibraryUpdateNotification(username, report.toString(), Instant.now()));
    }

    @Transactional
    @Async
    @SneakyThrows
    public void updateFormatsFromExternal() {
        updateFormatsFromExternal(null);
    }

    /**
     * Updates format with the definition from the PRONOM server
     * <p>
     * Attribute preferred is set to <code>true</code> automatically
     *
     * @param formatId format id of the format to update
     * @throws DocumentException if the input from the PRONOM server could not be parsed
     * @throws ParseException    could not parse date attribute
     */
    @Transactional
    public Pair<FormatDefinition, String> updateFormatFromExternal(Integer formatId) throws DocumentException, ParseException {
        FormatDefinition formatDefinitionFromExternal = getFormatDefinitionFromExternal(formatId);
        return formatDefinitionService.saveWithVersioning(formatDefinitionFromExternal);
    }

    /**
     * Saves new local definition of format:
     * <p>
     * 1. sets attribute 'local definition' to <code>true</code>
     * 2. sets attribute 'format occurrences' to empty
     * 3. sets the internal version number according to current highest version in DB
     * 4. if attribute 'preferred' is <code>true</code> the currently preferred version is set to not preferred (if exists),
     * 5. sets attribute 'preservation file' to null
     * 6. sets attribute 'id' with generated UUID
     * </p>
     *
     * @param localDefinition format to save
     * @return local definition created
     */
    @Transactional
    public Pair<FormatDefinition, String> updateFormatWithLocalDefinition(FormatDefinition localDefinition) {
        if (localDefinition.getFormat() == null || localDefinition.getFormat().getFormatId() == null)
            throw new BadArgument("missing formatId attribute of format object");
        if (localDefinition.getRelatedFormats().stream().anyMatch(f -> f.getRelatedFormatId() == null))
            throw new BadArgument("missing relatedFormatId attribute of related format object");

        localDefinition.setId(null);
        localDefinition.setLocalDefinition(true);

        return formatDefinitionService.saveWithVersioning(localDefinition);
    }

    /**
     * Exports format definition to JSON
     *
     * @param formatDefinitionId id of the format definition to export
     * @return JSON representation of the exported format definition
     * @throws JsonProcessingException
     */
    public String exportFormatDefinitionToJson(String formatDefinitionId) throws JsonProcessingException {
        FormatDefinition formatDefinition = formatDefinitionService.find(formatDefinitionId);
        notNull(formatDefinition, () -> new MissingObject(FormatDefinition.class, formatDefinitionId));

        log.debug("Exporting format definition with id " + formatDefinitionId + " to JSON.");
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(formatDefinition);
    }

    /**
     * Exports format definition to byte array compressed in GZIP
     *
     * @param formatDefinitionId id of the format definition to export
     * @return byte array representation of the exported format definition
     * @throws JsonProcessingException
     */
    public byte[] exportFormatDefinitionToByteArray(String formatDefinitionId) throws JsonProcessingException {
        FormatDefinition formatDefinition = formatDefinitionService.find(formatDefinitionId);
        notNull(formatDefinition, () -> new MissingObject(FormatDefinition.class, formatDefinitionId));

        log.debug("Exporting format definition with id " + formatDefinitionId + " to byte array.");
        byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(formatDefinition);
        return FormatLibraryUtils.compress(bytes);
    }

    /**
     * Exports all format definitions to JSON
     *
     * @return JSON representation of the exported format definitions
     * @throws JsonProcessingException
     */
    public String exportFormatDefinitionsToJson() throws JsonProcessingException {
        Collection<FormatDefinition> formatDefinitions = formatDefinitionService.findAll();

        log.debug("Exporting all format definitions to JSON.");
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(formatDefinitions);
    }

    /**
     * Exports all format definitions to byte array compressed in GZIP
     *
     * @return byte array representation of the exported format definitions
     * @throws JsonProcessingException
     */
    public byte[] exportFormatDefinitionsToByteArray() throws JsonProcessingException {
        Collection<FormatDefinition> formatDefinitions = formatDefinitionService.findAll();

        log.debug("Exporting all format definitions to byte array.");
        byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(formatDefinitions);
        return FormatLibraryUtils.compress(bytes);
    }

    /**
     * Imports format definition from JSON.
     * <p>
     * Format definition is imported as local definition. Attribute `format occurrences` is set to empty.
     *
     * @param json JSON with the format definition
     *             pair of [format definition, message describing the versioning of the format definition]
     * @throws IOException
     */
    public Pair<FormatDefinition, String> importFormatDefinitionFromJson(String json) throws IOException {
        FormatDefinition formatDefinition = objectMapper.readValue(json, FormatDefinition.class);

        log.debug("Importing format definition with id " + formatDefinition.getId() + ".");
        return updateFormatWithLocalDefinition(formatDefinition);
    }

    /**
     * Imports format definition from byte array compressed in GZIP.
     * <p>
     * Format definition is imported as local definition.
     *
     * @param compressedBytes byte array with the format definition
     *                        pair of [format definition, message describing the versioning of the format definition]
     * @throws IOException
     */
    public Pair<FormatDefinition, String> importFormatDefinitionFromByteArray(byte[] compressedBytes) throws IOException {
        FormatDefinition formatDefinition = objectMapper.readValue(FormatLibraryUtils.decompress(compressedBytes), FormatDefinition.class);

        log.debug("Importing format definition with id " + formatDefinition.getId() + ".");
        return updateFormatWithLocalDefinition(formatDefinition);
    }

    /**
     * Imports format definitions from JSON.
     * <p>
     * Format definitions are imported as local definitions.
     *
     * @param json JSON with the format definitions
     * @return list of pairs of [format definition, message describing the versioning of the format definition]
     * @throws IOException
     */
    @Async
    public List<Pair<FormatDefinition, String>> importFormatDefinitionsFromJson(String json) throws IOException {
        CollectionType typeReference = TypeFactory.defaultInstance().constructCollectionType(Collection.class, FormatDefinition.class);
        Collection<FormatDefinition> formatDefinitions = objectMapper.readValue(json, typeReference);

        log.debug("Importing format definitions passed as JSON.");
        List<Pair<FormatDefinition, String>> result = new ArrayList<>();
        for (FormatDefinition formatDefinition : formatDefinitions) {
            result.add(updateFormatWithLocalDefinition(formatDefinition));
        }
        return result;
    }

    /**
     * Imports format definitions from byte array compressed in GZIP.
     * <p>
     * Format definitions are imported as local definitions.
     *
     * @param compressedBytes byte array with the format definitions
     * @return list of pairs of [format definition, message describing the versioning of the format definition]
     * @throws IOException
     */
    @Async
    public List<Pair<FormatDefinition, String>> importFormatDefinitionsFromByteArray(byte[] compressedBytes) throws IOException {
        CollectionType typeReference = TypeFactory.defaultInstance().constructCollectionType(Collection.class, FormatDefinition.class);
        Collection<FormatDefinition> formatDefinitions = objectMapper.readValue(FormatLibraryUtils.decompress(compressedBytes), typeReference);

        log.debug("Importing format definitions passed as byte array.");
        List<Pair<FormatDefinition, String>> result = new ArrayList<>();
        for (FormatDefinition formatDefinition : formatDefinitions) {
            result.add(updateFormatWithLocalDefinition(formatDefinition));
        }
        return result;
    }

    private String enumLabelToEnumName(String label) {
        return label.toUpperCase()
                .replace(" ", "_")
                .replace("(", "")
                .replace(")", "")
                .replace("-", "");
    }

    @Inject
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Inject
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }

    @Inject
    public void setFormatService(FormatService formatService) {
        this.formatService = formatService;
    }

    @Inject
    public void setFormatDeveloperService(FormatDeveloperService formatDeveloperService) {
        this.formatDeveloperService = formatDeveloperService;
    }

    @Inject
    public void setFormatDefinitionService(FormatDefinitionService formatDefinitionService) {
        this.formatDefinitionService = formatDefinitionService;
    }

    @Inject
    public void setFormatListUrl(@Value("${formatLibrary.formatListUrl}") String formatListUrl) {
        this.formatListUrl = formatListUrl;
    }

    @Inject
    public void setFormatDetailListUrl(@Value("${formatLibrary.formatDetailListUrl}") String formatDetailListUrl) {
        this.formatDetailListUrl = formatDetailListUrl;
    }

    @Inject
    public void setFormatIdentifierService(FormatIdentifierService formatIdentifierService) {
        this.formatIdentifierService = formatIdentifierService;
    }

    @Inject
    public void setFormatLibraryNotifier(Optional<FormatLibraryNotifier> formatLibraryNotifier) {
        this.formatLibraryNotifier = formatLibraryNotifier;
    }
}
