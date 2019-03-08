package cz.cas.lib.arclib.service.preservationPlanning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.domain.preservationPlanning.*;
import cz.cas.lib.arclib.mail.ArclibMailCenter;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.service.UserService;
import cz.cas.lib.arclib.utils.ArclibUtils;
import cz.cas.lib.core.exception.GeneralException;
import cz.cas.lib.core.exception.MissingObject;
import cz.cas.lib.core.scheduling.job.Job;
import cz.cas.lib.core.scheduling.job.JobService;
import cz.cas.lib.core.script.ScriptType;
import cz.cas.lib.core.store.Transactional;
import cz.cas.lib.core.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.*;
import org.dom4j.io.SAXReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static cz.cas.lib.core.util.Utils.*;

@Slf4j
@Service
public class FormatLibraryUpdater {

    public static final String PATH_TO_FORMAT_IDS = "/PRONOM-Report/pronom:report_filetypes_detail/pronom:FileType/pronom:FormatID";
    public static final String PATH_TO_FILE_FORMAT = "/PRONOM-Report/pronom:report_format_detail/pronom:FileFormat";
    public static final String DATE_PATTERN = "dd MMM yyyy";
    public static final String PRONOM_NAMESPACE_URI = "http://pronom.nationalarchives.gov.uk";

    private String formatListUrl;
    private String formatDetailListUrl;
    private String formatLibraryUpdateCron;

    private ObjectMapper objectMapper;
    private FormatService formatService;
    private FormatDefinitionService formatDefinitionService;
    private FormatDeveloperService formatDeveloperService;
    private FormatIdentifierService formatIdentifierService;
    private RelatedFormatService relatedFormatService;
    private ArclibMailCenter arclibMailCenter;
    private UserDetails userDetails;
    private JobService jobService;
    private Resource formatLibraryUpdateScript;
    private UserService userService;

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
            formatIdXPath.setNamespaceURIs(asMap("pronom", PRONOM_NAMESPACE_URI));

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
            fileFormatXPath.setNamespaceURIs(asMap("pronom", PRONOM_NAMESPACE_URI));
            Element fileFormatElement = (Element) fileFormatXPath.selectNodes(doc).get(0);
            List<Element> formatAttributesElements = fileFormatElement.elements();

            Format format = formatService.findByFormatId(formatId);
            if (format == null) {
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
                    arclibMailCenter.sendUnsupportedPronomValueNotification(userDetails.getEmail(), e.getMessage(), Instant.now());
                    throw e;
                }

                switch (formatItem) {
                    case FormatAliases:
                        if (!attributeValue.trim().isEmpty()) {
                            String[] formatAliases = attributeValue.split(", ");
                            formatDefinition.setAliases(Arrays.stream(formatAliases).collect(Collectors.toSet()));
                        } else {
                            formatDefinition.setAliases(asSet());
                        }
                        break;
                    case FormatTypes: {
                        String[] formatTypes = attributeValue.split(", ");
                        Set<FormatClassification> parsedFormatClassifications = new HashSet<>();

                        if (!attributeValue.isEmpty()) {
                            for (String formatType : formatTypes) {
                                try {
                                    FormatClassification formatClassification = FormatClassification.valueOf(
                                            ArclibUtils.enumLabelToEnumName(formatType));
                                    parsedFormatClassifications.add(formatClassification);
                                } catch (IllegalArgumentException e) {
                                    arclibMailCenter.sendUnsupportedPronomValueNotification(userDetails.getEmail(), e.getMessage(), Instant.now());
                                    throw e;
                                }
                            }
                        }

                        Set<FormatClassification> formatClassifications = formatDefinition.getFormatClassifications();
                        if (formatClassifications == null) formatClassifications = asSet();
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

                        Set<FormatDeveloper> formatDevelopers = formatDefinition.getDevelopers();
                        if (formatDevelopers == null) formatDevelopers = asSet();
                        formatDevelopers.add(formatDeveloper);
                        formatDefinition.setDevelopers(formatDevelopers);
                        break;
                    case FormatID:
                        eq(Integer.valueOf(attributeValue), formatId, () -> new GeneralException("Value of attribute " +
                                FormatItem.FormatID + " does not match the expected value."));
                        break;
                    case FormatDescription:
                        formatDefinition.setFormatDescription(attributeValue);
                        break;
                    case FormatName:
                        if (format.getFormatName() != null) {
                            eq(attributeValue, format.getFormatName(), () -> new GeneralException(
                                    "Value of attribute " + FormatItem.FormatName + " does not match the expected value."));
                        } else {
                            format.setFormatName(attributeValue);
                        }
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
                                    identifierType = FormatIdentifierType.valueOf(ArclibUtils.enumLabelToEnumName(identifierTypeText));
                                } catch (IllegalArgumentException e) {
                                    arclibMailCenter.sendUnsupportedPronomValueNotification(userDetails.getEmail(), e.getMessage(), Instant.now());
                                    throw e;
                                }
                        }

                        FormatIdentifier formatIdentifier = formatIdentifierService.findByIdentifierTypeAndIdentifier(
                                identifierType, identifier);
                        if (formatIdentifier == null) {
                            formatIdentifier = new FormatIdentifier();
                            formatIdentifier.setIdentifierType(identifierType);
                            formatIdentifier.setIdentifier(identifier);
                            formatIdentifierService.save(formatIdentifier);
                        }

                        if (formatIdentifier.getIdentifierType() == FormatIdentifierType.PUID)
                            format.setPuid(formatIdentifier.getIdentifier());

                        Set<FormatIdentifier> identifiers = formatDefinition.getIdentifiers();
                        if (identifiers == null) identifiers = asSet();
                        identifiers.add(formatIdentifier);
                        formatDefinition.setIdentifiers(identifiers);
                        break;
                    case ReleaseDate:
                        if (!(attributeValue.isEmpty())) {
                            SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_PATTERN, Locale.UK);
                            formatDefinition.setReleaseDate(toInstant(dateFormatter.parse(attributeValue)));
                        }
                        break;
                    case FormatVersion:
                        formatDefinition.setFormatVersion(attributeValue);
                        break;
                    case WithdrawnDate:
                        if (!(attributeValue.isEmpty())) {
                            SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_PATTERN, Locale.UK);
                            formatDefinition.setWithdrawnDate(toInstant(dateFormatter.parse(attributeValue)));
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
                        if (formatFamilies == null) formatFamilies = asSet();
                        if (!attributeValue.isEmpty()) formatFamilies.add(attributeValue);
                        formatDefinition.setFormatFamilies(formatFamilies);
                        break;
                    case FormatRisk:
                        break;
                    case RelatedFormat:
                        Element relationshipTypeElement = elem.element("RelationshipType");
                        Element relatedFormatIdElement = elem.element("RelatedFormatID");
                        Element relatedFormatName = elem.element("RelatedFormatName");
                        Element relatedFormatVersion = elem.element("RelatedFormatVersion");

                        RelatedFormat relatedFormat = new RelatedFormat();

                        FormatRelationshipType formatRelationshipType;
                        String value = relationshipTypeElement.getText().toUpperCase().replace(" ", "_");
                        try {
                            formatRelationshipType = FormatRelationshipType.valueOf(value);
                        } catch (IllegalArgumentException e) {
                            arclibMailCenter.sendUnsupportedPronomValueNotification(userDetails.getEmail(), e.getMessage(), Instant.now());
                            throw e;
                        }
                        relatedFormat.setRelationshipType(formatRelationshipType);
                        relatedFormat.setRelatedFormatId(Integer.valueOf(relatedFormatIdElement.getText()));
                        relatedFormat.setRelatedFormatName(relatedFormatName.getText());
                        relatedFormat.setRelatedFormatVersion(relatedFormatVersion.getText());
                        relatedFormat = relatedFormatService.updateFromExternal(relatedFormat);

                        Set<RelatedFormat> relatedFormats = formatDefinition.getRelatedFormats();
                        if (relatedFormats == null) relatedFormats = asSet();
                        relatedFormats.add(relatedFormat);
                        formatDefinition.setRelatedFormats(relatedFormats);
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
            formatService.save(format);
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
    public void updateFormatsFromExternal(String userId) throws DocumentException, ParseException {
        List<Integer> listOfFormatIds = getListOfFormatIdsFromExternal();
        StringBuilder report = new StringBuilder();

        for (Integer formatId : listOfFormatIds) {
            String message = updateFormatFromExternal(formatId);
            report.append("Format with format id " + formatId + ": " + message + ".\n");
        }

        log.debug("Successfully updated all formats from external.");
        String userMail = null;
        if (userId != null) {
            User user = userService.find(userId);
            notNull(user, () -> new MissingObject(User.class, userId));
            userMail = user.getEmail();
        }
        arclibMailCenter.sendFormatLibraryUpdateNotification(userMail, report.toString(), Instant.now());
    }

    /**
     * Updates format with the definition from the PRONOM server
     *
     * @param formatId format id of the format to update
     * @return message describing the update process
     * @throws DocumentException if the input from the PRONOM server could not be parsed
     * @throws ParseException    could not parse date attribute
     */
    @Transactional
    public String updateFormatFromExternal(Integer formatId) throws DocumentException, ParseException {
        FormatDefinition formatDefinitionFromExternal = getFormatDefinitionFromExternal(formatId);

        List<FormatDefinition> localDefinitions = formatDefinitionService.findByFormatId(formatId, true);
        List<FormatDefinition> upstreamDefinitions = formatDefinitionService.findByFormatId(formatId, false);

        String message;

        /**
         * Case 1: both upstream definitions and local definitions are empty
         */
        if (localDefinitions.isEmpty() && upstreamDefinitions.isEmpty()) {
            formatDefinitionFromExternal.setPreferred(true);
            formatDefinitionFromExternal.setInternalVersionNumber(1);
            formatDefinitionService.save(formatDefinitionFromExternal);

            message = "new upstream definition created";
            log.debug("Format with format id " + formatId + ": " + message);
            return message;
        }

        /**
         * Case 2: upstream definitions are not empty, local definitions are empty
         */
        if (!upstreamDefinitions.isEmpty() && localDefinitions.isEmpty()) {
            FormatDefinition highestVersionUpstreamDefinition = upstreamDefinitions.stream()
                    .max(Comparator.comparing(FormatDefinition::getInternalVersionNumber))
                    .get();
            if (highestVersionUpstreamDefinition.equals(formatDefinitionFromExternal)) {
                message = "recent upstream definition is equal to the current definition";
            } else {
                formatDefinitionFromExternal.setInternalVersionNumber(highestVersionUpstreamDefinition.getInternalVersionNumber() + 1);
                formatDefinitionFromExternal.setPreviousInternalDefinition(highestVersionUpstreamDefinition);

                FormatDefinition preferredDefinition = formatDefinitionService.findPreferredDefinitionByFormatId(formatId);
                if (preferredDefinition != null) {
                    preferredDefinition.setPreferred(false);
                    formatDefinitionService.save(preferredDefinition);
                }

                formatDefinitionFromExternal.setPreferred(true);
                formatDefinitionService.save(formatDefinitionFromExternal);
                message = "has been updated with the recent upstream definition";
            }

            log.debug("Format with format id " + formatId + ": " + message);
            return message;
        }

        /**
         * Case 3: local definitions are not empty, upstream definitions are empty
         */
        if (!localDefinitions.isEmpty() && upstreamDefinitions.isEmpty()) {
            formatDefinitionFromExternal.setInternalVersionNumber(1);

            FormatDefinition preferredDefinition = formatDefinitionService.findPreferredDefinitionByFormatId(formatId);
            if (preferredDefinition != null) {
                formatDefinitionFromExternal.setPreferred(false);
                message = "new upstream definition created, there is a preferred local definition that " +
                        "overrides this upstream definition";
            } else {
                formatDefinitionFromExternal.setPreferred(true);
                message = "new upstream definition created, there is a local definition that is " +
                        "overriden by this upstream definition";
            }
            formatDefinitionService.save(formatDefinitionFromExternal);

            log.debug("Format with format id " + formatId + ": " + message);
            return message;
        }

        /**
         * Case 4: upstream definitions are not empty and local definitions are not empty
         */
        FormatDefinition highestVersionUpstreamDefinition = upstreamDefinitions.stream()
                .max(Comparator.comparing(FormatDefinition::getInternalVersionNumber))
                .get();
        if (highestVersionUpstreamDefinition.equals(formatDefinitionFromExternal)) {
            message = "recent upstream definition is equal to the current definition";
        } else {
            formatDefinitionFromExternal.setInternalVersionNumber(highestVersionUpstreamDefinition.getInternalVersionNumber() + 1);
            formatDefinitionFromExternal.setPreviousInternalDefinition(highestVersionUpstreamDefinition);

            FormatDefinition preferredDefinition = formatDefinitionService.findPreferredDefinitionByFormatId(formatId);

            if (preferredDefinition != null) {
                if (preferredDefinition.isLocalDefinition()) {
                    formatDefinitionFromExternal.setPreferred(false);
                    message = "has been updated with the recent upstream definition, there is a preferred local definition that " +
                            "overrides this upstream definition";
                } else {
                    preferredDefinition.setPreferred(false);
                    formatDefinitionService.save(preferredDefinition);

                    formatDefinitionFromExternal.setPreferred(true);
                    message = "has been updated with the recent upstream definition";
                }
            } else {
                formatDefinitionFromExternal.setPreferred(true);
                message = "has been updated with the recent upstream definition";
            }

            formatDefinitionService.save(formatDefinitionFromExternal);
        }

        log.debug("Format with format id " + formatId + ": " + message);
        return message;
    }

    /**
     * Saves new local definition of format:
     *
     * 1. sets attribute 'local definition' to <code>true</code>
     * 2. sets attribute `format occurrences` to empty
     * 3. sets the internal version number according to current highest version in DB
     * 4. if attribute 'preferred' is <code>true</code> the currently preferred version is set to not preferred (if exists),
     * 5. sets attribute 'preservation file' to null
     * if there is no currently preferred version, this version is set to preferred
     * </p>
     *
     * @param localDefinition format to save
     */
    @Transactional
    public void updateFormatWithLocalDefinition(FormatDefinition localDefinition) {
        localDefinition.setLocalDefinition(true);
        localDefinition.setFormatOccurrences(new HashSet<>());
        localDefinition.setPreservationPlanFile(null);
        Integer formatId = localDefinition.getFormat().getFormatId();

        List<FormatDefinition> localDefinitionsByFormatId = formatDefinitionService.findByFormatId(formatId, true);
        if (!localDefinitionsByFormatId.isEmpty()) {
            FormatDefinition highestVersionLocalDefinition = localDefinitionsByFormatId.stream()
                    .max(Comparator.comparing(FormatDefinition::getInternalVersionNumber))
                    .get();
            localDefinition.setInternalVersionNumber(highestVersionLocalDefinition.getInternalVersionNumber() + 1);
            localDefinition.setPreviousInternalDefinition(highestVersionLocalDefinition);
        } else {
            localDefinition.setInternalVersionNumber(1);
        }

        formatDefinitionService.save(localDefinition);
        log.debug("Format with format id " + formatId + " has been updated with local definition.");
    }

    @Transactional
    public void scheduleFormatLibraryUpdates() {
        log.debug("Scheduling format library updates with the definitions from PRONOM server.");

        Job job = new Job();
        job.setTiming(formatLibraryUpdateCron);
        job.setName("Format library updater.");
        job.setParams(new HashMap<>());
        job.setScriptType(ScriptType.GROOVY);
        try {
            String script = StreamUtils.copyToString(formatLibraryUpdateScript.getInputStream(), Charset.defaultCharset());
            job.setScript(script);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        job.setActive(true);
        jobService.save(job);
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
        Utils.notNull(formatDefinition, () -> new MissingObject(FormatDefinition.class, formatDefinitionId));

        log.debug("Exporting format definition with id " + formatDefinitionId + " to JSON.");
        objectMapper.findAndRegisterModules();
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
        Utils.notNull(formatDefinition, () -> new MissingObject(FormatDefinition.class, formatDefinitionId));

        log.debug("Exporting format definition with id " + formatDefinitionId + " to byte array.");
        objectMapper.findAndRegisterModules();
        byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(formatDefinition);
        return Utils.compress(bytes);
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
        objectMapper.findAndRegisterModules();
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
        objectMapper.findAndRegisterModules();
        byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(formatDefinitions);
        return Utils.compress(bytes);
    }

    /**
     * Imports format definition from JSON.
     * <p>
     * Format definition is imported as local definition. Attribute `format occurrences` is set to empty.
     *
     * @param json JSON with the format definition
     * @throws IOException
     */
    public void importFormatDefinitionFromJson(String json) throws IOException {
        objectMapper.findAndRegisterModules();
        FormatDefinition formatDefinition = objectMapper.readValue(json, FormatDefinition.class);

        log.debug("Importing format definition with id " + formatDefinition.getId() + ".");
        updateFormatWithLocalDefinition(formatDefinition);
    }

    /**
     * Imports format definition from byte array compressed in GZIP.
     * <p>
     * Format definition is imported as local definition.
     *
     * @param compressedBytes byte array with the format definition
     * @throws IOException
     */
    public void importFormatDefinitionFromByteArray(byte[] compressedBytes) throws IOException {
        byte[] bytes = decompress(compressedBytes);

        objectMapper.findAndRegisterModules();
        FormatDefinition formatDefinition = objectMapper.readValue(bytes, FormatDefinition.class);

        log.debug("Importing format definition with id " + formatDefinition.getId() + ".");
        updateFormatWithLocalDefinition(formatDefinition);
    }

    /**
     * Imports format definitions from JSON.
     * <p>
     * Format definitions are imported as local definitions.
     *
     * @param json JSON with the format definitions
     * @throws IOException
     */
    @Async
    public void importFormatDefinitionsFromJson(String json) throws IOException {
        CollectionType typeReference =
                TypeFactory.defaultInstance().constructCollectionType(Collection.class, FormatDefinition.class);
        objectMapper.findAndRegisterModules();
        Collection<FormatDefinition> formatDefinitions = objectMapper.readValue(json, typeReference);

        log.debug("Importing format definitions passed as JSON.");
        formatDefinitions.forEach(this::updateFormatWithLocalDefinition);
    }

    /**
     * Imports format definitions from byte array compressed in GZIP.
     * <p>
     * Format definitions are imported as local definitions.
     *
     * @param compressedBytes byte array with the format definitions
     * @throws IOException
     */
    @Async
    public void importFormatDefinitionsFromByteArray(byte[] compressedBytes) throws IOException {
        byte[] bytes = decompress(compressedBytes);
        CollectionType typeReference =
                TypeFactory.defaultInstance().constructCollectionType(Collection.class, FormatDefinition.class);
        objectMapper.findAndRegisterModules();
        Collection<FormatDefinition> formatDefinitions = objectMapper.readValue(bytes, typeReference);

        log.debug("Importing format definitions passed as byte array.");
        formatDefinitions.forEach(this::updateFormatWithLocalDefinition);
    }

    @Inject
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Inject
    public void setJobService(JobService jobService) {
        this.jobService = jobService;
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
    public void setArclibMailCenter(ArclibMailCenter arclibMailCenter) {
        this.arclibMailCenter = arclibMailCenter;
    }

    @Inject
    public void setFormatDeveloperService(FormatDeveloperService formatDeveloperService) {
        this.formatDeveloperService = formatDeveloperService;
    }

    @Inject
    public void setRelatedFormatService(RelatedFormatService relatedFormatService) {
        this.relatedFormatService = relatedFormatService;
    }

    @Inject
    public void setFormatDefinitionService(FormatDefinitionService formatDefinitionService) {
        this.formatDefinitionService = formatDefinitionService;
    }

    @Inject
    public void setFormatLibraryUpdateCron(@Value("${arclib.formatLibraryUpdateCron}") String formatLibraryUpdateCron) {
        this.formatLibraryUpdateCron = formatLibraryUpdateCron;
    }

    @Inject
    public void setFormatLibraryUpdateScript(@Value("${arclib.formatLibraryUpdateScript}") Resource formatLibraryUpdateScript) {
        this.formatLibraryUpdateScript = formatLibraryUpdateScript;
    }

    @Inject
    public void setFormatListUrl(@Value("${pronom.formatListUrl}") String formatListUrl) {
        this.formatListUrl = formatListUrl;
    }

    @Inject
    public void setFormatDetailListUrl(@Value("${pronom.formatDetailListUrl}") String formatDetailListUrl) {
        this.formatDetailListUrl = formatDetailListUrl;
    }

    @Inject
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Inject
    public void setFormatIdentifierService(FormatIdentifierService formatIdentifierService) {
        this.formatIdentifierService = formatIdentifierService;
    }
}
