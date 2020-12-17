package cz.cas.lib.arclib.service.arclibxml;

import cz.cas.lib.arclib.domain.IngestToolFunction;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestEvent;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestIssue;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.packages.FolderStructure;
import cz.cas.lib.arclib.domain.packages.Sip;
import cz.cas.lib.arclib.domain.preservationPlanning.Tool;
import cz.cas.lib.arclib.exception.validation.MissingNode;
import cz.cas.lib.arclib.service.SipProfileService;
import cz.cas.lib.arclib.service.fixity.MetsChecksumType;
import cz.cas.lib.arclib.store.IngestEventStore;
import cz.cas.lib.arclib.store.IngestWorkflowStore;
import cz.cas.lib.arclib.utils.ArclibUtils;
import cz.cas.lib.arclib.utils.NamespaceChangingVisitor;
import cz.cas.lib.core.store.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.dom4j.*;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.DefaultAttribute;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static cz.cas.lib.arclib.bpm.BpmConstants.*;
import static cz.cas.lib.arclib.utils.ArclibUtils.*;
import static cz.cas.lib.core.util.Utils.isNullOrEmptyString;

@Slf4j
@Service
public class ArclibXmlGenerator {
    public static final String INITIAL_VERSION = "initial version";
    public static final String EVENT = "EVENT_";
    public static final String AGENT_ARCLIB = "agent_ARCLIB";
    public static final String EVENT_NUMBER_FORMAT = "%03d";
    public static final String EVENT_ID_NUMBER_FORMAT = "%03d";
    public static final String OBJ_NUMBER_FORMAT = "%03d";
    public static final String AGENT_NUMBER_FORMAT = "%03d";
    public static final String XML_UPDATE_PREMIS_EVENT = "metadata_modification";

    private IngestWorkflowStore ingestWorkflowStore;
    private Map<String, String> uris;
    private String arclibVersion;
    private IngestEventStore ingestEventStore;
    private SipProfileService sipProfileService;

    /**
     * Supplements ArclibXml with generated metadata
     *
     * @param xml       ARCLibXml
     * @param variables BPM variables from Camunda execution
     * @return ARCLibXml supplemented by generated metadata
     * @throws DocumentException provided <code>xml</code> could not be parsed
     */
    @Transactional
    public Document generateMetadata(String xml, Map<String, Object> variables, IngestEvent generationEvent) throws DocumentException {
        List<Pair<String, String>> filePathsAndObjIdentifiers = new ArrayList<>();

        String ingestWorkflowExternalId = (String) variables.get(ProcessVariables.ingestWorkflowExternalId);
        log.debug("Generating metadata for ingest workflow with external id " + ingestWorkflowExternalId + ".");
        IngestWorkflow ingestWorkflow = ingestWorkflowStore.findByExternalId(ingestWorkflowExternalId);

        //assign each file an object identifier
        List<String> filePaths = ArclibUtils.listFilePaths(Paths.get((String) variables.get(ProcessVariables.sipFolderWorkspacePath)));
        for (int i = 0; i < filePaths.size(); i++) {
            String objIdentifier = "obj-" + String.format(OBJ_NUMBER_FORMAT, i + 1);
            filePathsAndObjIdentifiers.add(Pair.of(filePaths.get(i), objIdentifier));
        }

        SAXReader reader = new SAXReader();
        Document doc = reader.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        if (doc.getRootElement().getNamespaceForPrefix(XLINK) == null)
            doc.getRootElement().addNamespace(XLINK, uris.get(XLINK));

        XPath metsXPath = doc.createXPath("/METS:mets");
        Element metsElement = (Element) metsXPath.selectSingleNode(doc);
        if (metsElement == null) throw new MissingNode(metsXPath.getText());

        /*
          Add METS:OBJID
         */
        metsElement.addAttribute("OBJID", ingestWorkflow.getSip().getId());

        /*
          Fill METS:metsHdr
         */
        XPath mestHdrPath = doc.createXPath("/METS:mets/METS:metsHdr");
        Element metsHdrElement = (Element) mestHdrPath.selectSingleNode(doc);
        if (metsHdrElement == null) throw new MissingNode(mestHdrPath.getText());
        fillMetsHdr(metsHdrElement, ingestWorkflow, variables);

        /*
          Add SIP and XML versions and related SIP and XML
         */
        addSipInfo(metsElement, ingestWorkflow, variables);

        /*
          Add premis:agents and respective premis:events
         */
        Set<String> eventIdentifiers = new HashSet<>();
        Element element = addPremisAgentsAndEvents(metsElement, variables, eventIdentifiers, generationEvent);
        int positionOfPremisAgentsAndEvents = metsElement.elements().indexOf(element);

        /*
          Add premis:object for whole package
         */
        //place the element with premis object before the element with premis agents and events
        addPremisObject(metsElement, variables, positionOfPremisAgentsAndEvents, eventIdentifiers, ingestWorkflow.getSip().getSizeInBytes());

        /*
          Add METS:fileSec
         */
        addFileSec(metsElement, variables, filePathsAndObjIdentifiers);

        /*
          Add METS:structMap
         */
        addStructMap(metsElement, ingestWorkflow.getSip(), filePathsAndObjIdentifiers);
        return doc;
    }

    private void fillMetsHdr(Element metsHdrElement, IngestWorkflow ingestWorkflow, Map<String, Object> variables) {
        String xmlId = (String) variables.get(ProcessVariables.ingestWorkflowExternalId);
        String authorialId = (String) variables.get(Ingestion.authorialId);

        metsHdrElement.addAttribute("CREATEDATE", ingestWorkflow.getCreated().truncatedTo(ChronoUnit.SECONDS).toString());
        metsHdrElement.addAttribute("LASTMODDATE", ingestWorkflow.getUpdated().truncatedTo(ChronoUnit.SECONDS).toString());
        metsHdrElement.addAttribute("ID", xmlId);

        XPath creatorAgentPath = metsHdrElement.createXPath("METS:agent[@ROLE='CREATOR']");

        Element createAgentElement = (Element) creatorAgentPath.selectSingleNode(metsHdrElement);
        if (createAgentElement == null) {
            createAgentElement = metsHdrElement.addElement("METS:agent");
            createAgentElement.addAttribute("ROLE", "CREATOR");
            createAgentElement.addAttribute("TYPE", "ORGANIZATION");
        }
        XPath namePath = createAgentElement.createXPath("METS:name");
        Element nameElement = (Element) namePath.selectSingleNode(createAgentElement);
        if (nameElement == null) nameElement = createAgentElement.addElement("METS:name");
        nameElement.setText(ingestWorkflow.getBatch().getProducerProfile().getProducer().getName());

        Element sipIdentifierElement = metsHdrElement.addElement("METS:altRecordID");
        sipIdentifierElement.addText(authorialId);
        sipIdentifierElement.addAttribute("TYPE", "original SIP identifier");
    }

    private void addSipInfo(Element metsElement, IngestWorkflow ingestWorkflow, Map<String, Object> variables) {
        Element amdSecEl = metsElement.addElement("METS:amdSec");

        Element digiprovMdEl = amdSecEl.addElement("METS:digiprovMD");
        digiprovMdEl.addAttribute("ID", "ARCLIB_SIP_INFO");
        Element mdWrapEl = digiprovMdEl.addElement("METS:mdWrap");
        mdWrapEl.addAttribute("MDTYPE", "OTHER");
        Element xmlDataEl = mdWrapEl.addElement("METS:xmlData");
        Element arclibSipInfoEl = xmlDataEl.addElement("ARCLib:sipInfo", uris.get(ARCLIB));

        Element sipVersionNumber = arclibSipInfoEl.addElement("ARCLib:sipVersionNumber");
        sipVersionNumber.addText(String.valueOf(ingestWorkflow.getSip().getVersionNumber()));

        Sip previousVersionSip = ingestWorkflow.getSip().getPreviousVersionSip();
        String previousVersionSipId = previousVersionSip != null ? previousVersionSip.getId() : INITIAL_VERSION;
        Element sipVersionOf = arclibSipInfoEl.addElement("ARCLib:sipVersionOf");
        sipVersionOf.addText(previousVersionSipId);

        Element xmlVersionNumber = arclibSipInfoEl.addElement("ARCLib:xmlVersionNumber");
        xmlVersionNumber.addText(String.valueOf(ingestWorkflow.getXmlVersionNumber()));

        IngestWorkflow relatedWorkflow = ingestWorkflow.getRelatedWorkflow();
        String previousVersionXmlId = relatedWorkflow != null && ingestWorkflow.getXmlVersionNumber() > 1
                ? relatedWorkflow.getExternalId() : INITIAL_VERSION;
        Element xmlVersionOf = arclibSipInfoEl.addElement("ARCLib:xmlVersionOf");
        xmlVersionOf.addText(previousVersionXmlId);

        Element ingestProfilesEl = arclibSipInfoEl.addElement("ARCLib:ingestProfiles");
        ingestProfilesEl.addElement("ARCLib:producerProfile").addText(ingestWorkflow.getProducerProfile().getExternalId());
        ingestProfilesEl.addElement("ARCLib:sipProfile").addText((String) variables.get(MetadataExtraction.usedSipProfile));
        ingestProfilesEl.addElement("ARCLib:validationProfile").addText((String) variables.get(Validation.usedValidationProfile));
        ingestProfilesEl.addElement("ARCLib:workflowDefinition").addText(ingestWorkflow.getProducerProfile().getWorkflowDefinition().getExternalId());
    }

    /**
     * every external tool used results in new agent, internal tools falls under ARCLIB agent
     *
     * @return created METS:amdSec element to which the agents and events are added
     */
    private Element addPremisAgentsAndEvents(Element metsElement, Map<String, Object> variables, Set<String> eventIdentifiers, IngestEvent generationEvent) {
        String iwExternalId = (String) variables.get(ProcessVariables.ingestWorkflowExternalId);

        List<IngestEvent> allEvents = ingestEventStore.findAllOfIngestWorkflow(iwExternalId);
        allEvents.add(generationEvent);
        /*
          Add premis:agent elements
         */
        int agentCounter = 1;
        Element amdSecElement = metsElement.addElement("METS:amdSec");
        //ARCLib agent
        addEventAgent(amdSecElement, agentCounter++, AGENT_ARCLIB, "ARCLIB",
                "software", "Version " + arclibVersion);

        List<Tool> usedExternalTools = allEvents
                .stream()
                .filter(e -> !e.getTool().isInternal())
                .map(IngestEvent::getTool)
                .distinct()
                .collect(Collectors.toList());

        for (Tool t : usedExternalTools) {
            String agentNote = "Version: " + t.getVersion();
            if (!isNullOrEmptyString(t.getDescription()))
                agentNote = agentNote + ", Description: " + t.getDescription();
            addEventAgent(amdSecElement, agentCounter++, toAgentId(t), t.getName(),
                    "software", agentNote);
        }

        /*
          Add premis:event elements
         */
        int eventCounter = 1;
        Element amdSecForEventsElement = metsElement.addElement("METS:amdSec");

        Map<IngestToolFunction, Integer> eventsPerFunction = new HashMap<>();
        for (IngestEvent event : allEvents) {
            IngestToolFunction function = event.getTool().getToolFunction();
            Integer count = eventsPerFunction.get(function);
            if (count == null) count = 1;
            else count++;
            eventsPerFunction.put(function, count);
            String eventIdValue = toEventId(function) + "_" + String.format(EVENT_ID_NUMBER_FORMAT, count);
            String agentValue = event.getTool().isInternal() ? AGENT_ARCLIB : toAgentId(event.getTool());
            String eventDetail;
            if (event instanceof IngestIssue && ((IngestIssue) event).getIngestIssueDefinition().isReconfigurable()) {
                IngestIssue issue = (IngestIssue) event;
                eventDetail = issue.getIngestIssueDefinition().toString() + (issue.getFormatDefinition() == null ? "" : ", " + issue.getFormatDefinition()) + ", Issue details: " + issue.getDescription();
            } else
                eventDetail = event.getDescription();

            String eventIdentifier = EVENT + String.format(EVENT_NUMBER_FORMAT, eventCounter++);

            addEvent(amdSecForEventsElement, eventIdValue, eventIdentifier, event.isSuccess(), agentValue, eventDetail,
                    function.toString().replace("_", " "), event.getCreated().truncatedTo(ChronoUnit.SECONDS).toString());
            eventIdentifiers.add(eventIdValue);
        }
        return amdSecElement;
    }

    /**
     * @param position position of the amdSecElement to be created in withing METS file
     */
    private void addPremisObject(Element metsElement, Map<String, Object> variables, int position, Set<String> eventIdentifiers, long sizeInBytes) {
        Element amdSecElement = metsElement.addElement("METS:amdSec");

        //move created amdSecElement to the given position in the METS file
        List elements = metsElement.elements();
        elements.add(position, amdSecElement.detach());

        Element techMDElement = amdSecElement.addElement("METS:techMD");
        techMDElement.addAttribute("ID", "techMD_1");
        Element mdWrapElement = techMDElement.addElement("METS:mdWrap");
        mdWrapElement.addAttribute("MDTYPE", "PREMIS");
        Element xmlDataElement = mdWrapElement.addElement("METS:xmlData");
        Element objectElement = xmlDataElement.addElement("premis:object", uris.get(PREMIS));
        objectElement.addAttribute(QName.get("xsi:type", uris.get(XSI)), "premis:file");

        Element objectIdentifierElement = objectElement.addElement("premis:objectIdentifier");
        Element objectIdentifierType = objectIdentifierElement.addElement("premis:objectIdentifierType");
        objectIdentifierType.addText("local");
        Element objectIdentifierValue = objectIdentifierElement.addElement("premis:objectIdentifierValue");
        objectIdentifierValue.addText("obj-package");

        Element objectCharacteristicsElement = objectElement.addElement("premis:objectCharacteristics");
        Element compositionLevelElement = objectCharacteristicsElement.addElement("premis:compositionLevel");
        compositionLevelElement.addText("0");

        String preferredFixityGenerationEventId = (String) variables.get(FixityGeneration.preferredFixityGenerationEventId);
        if (preferredFixityGenerationEventId != null) {
            IngestEvent preferredFixityGenerationEvent = ingestEventStore.find(preferredFixityGenerationEventId);
            if (preferredFixityGenerationEvent != null && preferredFixityGenerationEvent.isSuccess()) {
                Map<String, String> mapOfEventIdsToMd5Calculations = (Map<String, String>)
                        variables.get(FixityGeneration.mapOfEventIdsToSipMd5);
                addFixity(objectCharacteristicsElement, "MD5", mapOfEventIdsToMd5Calculations.get(preferredFixityGenerationEventId));

                Map<String, String> mapOfEventIdsToCrc32Calculations = (Map<String, String>)
                        variables.get(FixityGeneration.mapOfEventIdsToSipCrc32);
                addFixity(objectCharacteristicsElement, "CRC32", mapOfEventIdsToCrc32Calculations.get(preferredFixityGenerationEventId));

                Map<String, String> mapOfEventIdsToSha512Calculations = (Map<String, String>)
                        variables.get(FixityGeneration.mapOfEventIdsToSipSha512);
                addFixity(objectCharacteristicsElement, "SHA-512", mapOfEventIdsToSha512Calculations.get(preferredFixityGenerationEventId));
            }
        }

        Element sizeElement = objectCharacteristicsElement.addElement("premis:size");
        sizeElement.addText(String.valueOf(sizeInBytes));

        Element formatElement = objectCharacteristicsElement.addElement("premis:format");
        Element formatDesignationElement = formatElement.addElement("premis:formatDesignation");

        Element formatNameElement = formatDesignationElement.addElement("premis:formatName");
        formatNameElement.addText("application/zip");

        Element formatRegistryElement = formatElement.addElement("premis:formatRegistry");
        Element formatRegistryNameElement = formatRegistryElement.addElement("premis:formatRegistryName");
        formatRegistryNameElement.addText("PRONOM");

        Element formatRegistryKeyElement = formatRegistryElement.addElement("premis:formatRegistryKey");
        formatRegistryKeyElement.addText("x-fmt/263");

        addAggregatedFormats(xmlDataElement, variables);

        for (String eventIdentifier : eventIdentifiers) {
            Element linkingEventIdentifierElement = objectElement.addElement("premis:linkingEventIdentifier");
            Element linkingEventIdentifierTypeElement = linkingEventIdentifierElement.addElement("premis:linkingEventIdentifierType");
            linkingEventIdentifierTypeElement.addText("EventId");
            Element linkingEventIdentifierValueElement = linkingEventIdentifierElement.addElement("premis:linkingEventIdentifierValue");
            linkingEventIdentifierValueElement.addText(eventIdentifier);
        }
    }

    private void addStructMap(Element metsElement, Sip sip, List<Pair<String, String>> filePathsAndObjIdentifiers) {
        Element structMapElement = metsElement.addElement("METS:structMap");
        structMapElement.addAttribute("ID", "Physical_Structure");
        structMapElement.addAttribute("TYPE", "PHYSICAL");

        Element aipDivElement = structMapElement.addElement("METS:div");

        FolderStructure sipFolderStructure = sip.getFolderStructure();
        addStructMapDivElementsRecursively(aipDivElement, sipFolderStructure, sipFolderStructure.getCaption(), filePathsAndObjIdentifiers);
    }

    private void addStructMapDivElementsRecursively(Element parentDivElement,
                                                    FolderStructure folderStructure,
                                                    String parentFolderStructurePath,
                                                    List<Pair<String, String>> filePathsAndObjIdentifiers) {
        Collection<FolderStructure> children = folderStructure.getChildren();
        if (children == null) {
            //folder structure represents a file
            Optional<Pair<String, String>> first = filePathsAndObjIdentifiers.stream()
                    .filter(pair -> {
                        String folderStructurePath = parentFolderStructurePath.substring(parentFolderStructurePath.indexOf("/") + 1);
                        return pair.getLeft().equals(folderStructurePath);
                    })
                    .findFirst();
            Pair<String, String> filePathAndObjectIdentifier = first.get();
            Element fptrElement = parentDivElement.addElement("METS:fptr");
            fptrElement.addAttribute("FILEID", filePathAndObjectIdentifier.getRight());
        } else {
            //folder structure represents a directory
            Element divElement = parentDivElement.addElement("METS:div");
            divElement.addAttribute("LABEL", parentFolderStructurePath);
            children.stream()
                    .sorted(Comparator.comparing(fs -> fs.getChildren() != null))
                    .forEach(childFolderStructure -> addStructMapDivElementsRecursively(divElement, childFolderStructure,
                            parentFolderStructurePath + "/" + childFolderStructure.getCaption(), filePathsAndObjIdentifiers));
        }
    }

    private void addFileSec(Element metsElement, Map<String, Object> variables, List<Pair<String, String>> filePathsAndObjIdentifiers) {
        Element fileSecElement = metsElement.addElement("METS:fileSec");
        Element fileGrpElement = fileSecElement.addElement("METS:fileGrp");
        fileGrpElement.addAttribute("USE", "file");

        IngestEvent preferredFixityGenerationEvent = null;
        String preferredFixityGenerationEventId = (String) variables.get(FixityGeneration.preferredFixityGenerationEventId);
        if (preferredFixityGenerationEventId != null)
            preferredFixityGenerationEvent = ingestEventStore.find(preferredFixityGenerationEventId);
        if (preferredFixityGenerationEventId == null || preferredFixityGenerationEvent == null || !preferredFixityGenerationEvent.isSuccess()) {
            for (Pair<String, String> filePathsAndObjIdentifier : filePathsAndObjIdentifiers) {
                Element fileElement = fileGrpElement.addElement("METS:file");
                fileElement.addAttribute("ID", filePathsAndObjIdentifier.getRight());
                Element fLocatElement = fileElement.addElement("METS:FLocat");
                fLocatElement.addAttribute("LOCTYPE", "OTHER");
                fLocatElement.addAttribute(XLINK+":href", filePathsAndObjIdentifier.getLeft());
            }
            return;
        }
        Map<String, Triple<Long, String, String>> sipContentFixityData =
                ((Map<String, Map<String, Triple<Long, String, String>>>)
                        variables.get(FixityGeneration.mapOfEventIdsToSipContentFixityData)).get(preferredFixityGenerationEventId);
        for (Pair<String, String> filePathsAndObjIdentifier : filePathsAndObjIdentifiers) {
            Element fileElement = fileGrpElement.addElement("METS:file");
            fileElement.addAttribute("ID", filePathsAndObjIdentifier.getRight());
            Element fLocatElement = fileElement.addElement("METS:FLocat");
            fLocatElement.addAttribute("LOCTYPE", "OTHER");
            fLocatElement.addAttribute(XLINK+":href", filePathsAndObjIdentifier.getLeft());
            Triple<Long, String, String> fileFixityData = sipContentFixityData.get(filePathsAndObjIdentifier.getLeft());
            if (fileFixityData != null) {
                if (fileFixityData.getLeft() != null)
                    fileElement.addAttribute("SIZE", fileFixityData.getLeft().toString());
                if (fileFixityData.getMiddle() != null) {
                    MetsChecksumType metsChecksumType = EnumUtils.getEnum(MetsChecksumType.class, fileFixityData.getMiddle());
                    if (metsChecksumType != null) {
                        fileElement.addAttribute("CHECKSUMTYPE", metsChecksumType.getXmlValue());
                        fileElement.addAttribute("CHECKSUM", fileFixityData.getRight());
                    }
                }
            }
        }
    }

    private void addEventAgent(Element amdSecElement, Integer agentCounter, String agentIdentifierValue,
                               String agentName, String agentType, String agentNote) {
        Element digiprovMDElement = amdSecElement.addElement("METS:digiprovMD");
        digiprovMDElement.addAttribute("ID", "AGENT_" + String.format(AGENT_NUMBER_FORMAT, agentCounter));
        Element digiProvMDmdWrapElement = digiprovMDElement.addElement("METS:mdWrap");
        digiProvMDmdWrapElement.addAttribute("MDTYPE", "PREMIS");
        Element digiProvMDXmlDataElement = digiProvMDmdWrapElement.addElement("METS:xmlData");

        Element premisAgentElement = digiProvMDXmlDataElement.addElement("premis:agent", uris.get(PREMIS));
        Element agentIdentifierElement = premisAgentElement.addElement("premis:agentIdentifier");

        Element agentIdentifierTypeElement = agentIdentifierElement.addElement("premis:agentIdentifierType");
        agentIdentifierTypeElement.addText("AgentID");

        Element agentIdentifierValueElement = agentIdentifierElement.addElement("premis:agentIdentifierValue");
        agentIdentifierValueElement.addText(agentIdentifierValue);
        Element agentNameElement = premisAgentElement.addElement("premis:agentName");
        agentNameElement.addText(agentName);
        Element agentTypeElement = premisAgentElement.addElement("premis:agentType");
        agentTypeElement.addText(agentType);

        Element agentNoteElement = premisAgentElement.addElement("premis:agentNote");
        agentNoteElement.setText(agentNote);
    }

    private void addEvent(Element amdSecForEventsElement, String eventIdentifierValue, String eventIdentifier, Boolean success,
                          String linkingAgentIdentifier, String eventDetail, String eventType, String eventDateTime) {
        Element digiprovMDIdentificationElement = amdSecForEventsElement.addElement("METS:digiprovMD");
        digiprovMDIdentificationElement.addAttribute("ID", eventIdentifier);

        Element mdWrapIdentificationEl = digiprovMDIdentificationElement.addElement("METS:mdWrap");
        mdWrapIdentificationEl.addAttribute("MDTYPE", "PREMIS");
        Element xmlDataIdentificationEl = mdWrapIdentificationEl.addElement("METS:xmlData");
        Element eventElement = xmlDataIdentificationEl.addElement("premis:event", uris.get(PREMIS));

        Element eventIdentifierElement = eventElement.addElement("premis:eventIdentifier");
        Element eventIdentifierType = eventIdentifierElement.addElement("premis:eventIdentifierType");
        eventIdentifierType.addText("EventId");
        Element eventIdentifierValueElement = eventIdentifierElement.addElement("premis:eventIdentifierValue");
        eventIdentifierValueElement.addText(eventIdentifierValue);

        Element eventTypeElement = eventElement.addElement("premis:eventType");
        eventTypeElement.addText(eventType);

        Element eventDateTimeElement = eventElement.addElement("premis:eventDateTime");
        eventDateTimeElement.addText(eventDateTime);

        if (eventDetail != null) {
            Element eventDetailElement = eventElement.addElement("premis:eventDetail");
            eventDetailElement.addText(eventDetail);
        }

        Element eventOutcomeInformationElement =
                eventElement.addElement("premis:eventOutcomeInformation");
        Element eventOutcomeElement = eventOutcomeInformationElement.addElement("premis:eventOutcome");

        String eventOutcomeInformation = success ? "successful" : "unsuccessful";
        eventOutcomeElement.setText(eventOutcomeInformation);

        Element linkingAgentIdentifierElement = eventElement.addElement("premis:linkingAgentIdentifier");
        Element linkingAgentIdentifierTypeElement = linkingAgentIdentifierElement.addElement("premis:linkingAgentIdentifierType");
        linkingAgentIdentifierTypeElement.addText("AgentId");
        Element linkingAgentIdentifierValueElement = linkingAgentIdentifierElement.addElement("premis:linkingAgentIdentifierValue");
        linkingAgentIdentifierValueElement.setText(linkingAgentIdentifier);
    }

    /**
     * Add fixity to object characteristics element
     *
     * @param objectCharacteristicsElement object characteristics element
     * @param fixityType                   name of the element with the fixity
     * @param hash                         content of the fixity hash
     */
    private void addFixity(Element objectCharacteristicsElement, String fixityType, String hash) {
        Element fixityElement = objectCharacteristicsElement.addElement("premis:fixity");
        Element messageDigestAlgorithmElement = fixityElement.addElement("premis:messageDigestAlgorithm");
        messageDigestAlgorithmElement.addText(fixityType);
        Element messageDigest = fixityElement.addElement("premis:messageDigest");
        messageDigest.addText(hash);
    }

    private void addAggregatedFormats(Element xmlDataElement, Map<String, Object> variables) {
        Element arclibFormatsElement = xmlDataElement.addElement("ARCLib:formats", uris.get(ARCLIB));
        String preferredFormatIdentificationEventId = (String) variables.get(FormatIdentification.preferredFormatIdentificationEventId);
        if (preferredFormatIdentificationEventId == null)
            return;
        IngestEvent preferredFormatIdentificationEvent = ingestEventStore.find(preferredFormatIdentificationEventId);
        if (preferredFormatIdentificationEvent != null && preferredFormatIdentificationEvent.isSuccess()) {
            HashMap<String, TreeMap<String, Pair<String, String>>> mapOfEventIdsToMapsOfFilesToFormats =
                    (HashMap<String, TreeMap<String, Pair<String, String>>>)
                            variables.get(FormatIdentification.mapOfEventIdsToMapsOfFilesToFormats);

            TreeMap<String, Pair<String, String>> identifiedFormats = mapOfEventIdsToMapsOfFilesToFormats.get(preferredFormatIdentificationEventId);

            Map<Pair<String, String>, Long> aggregatedFormats =
                    computeAggregatedCount(identifiedFormats.values());

            aggregatedFormats.keySet()
                    .forEach(formatToIdentifier -> {
                        Element arclibFormatElement = arclibFormatsElement.addElement("ARCLib:format");

                        Element formatRegistryKeyElement = arclibFormatElement.addElement("ARCLib:formatRegistryKey");
                        formatRegistryKeyElement.setText(formatToIdentifier.getLeft());

                        Element formatRegistryNameElement = arclibFormatElement.addElement("ARCLib:formatRegistryName");
                        formatRegistryNameElement.setText("PRONOM");

                        Tool formatIdentificationTool = preferredFormatIdentificationEvent.getTool();

                        Element creatingApplicationNameElement =
                                arclibFormatElement.addElement("ARCLib:creatingApplicationName");
                        creatingApplicationNameElement.setText(formatIdentificationTool.getName());

                        Element creatingApplicationVersionElement =
                                arclibFormatElement.addElement("ARCLib:creatingApplicationVersion");
                        creatingApplicationVersionElement.setText(formatIdentificationTool.getVersion());

                        Element dateCreatedByApplicationElement =
                                arclibFormatElement.addElement("ARCLib:dateCreatedByApplication");
                        dateCreatedByApplicationElement.setText((preferredFormatIdentificationEvent.getUpdated()
                                .truncatedTo(ChronoUnit.SECONDS)).toString().substring(0, 10));

                        Element fileCountElement = arclibFormatElement.addElement("ARCLib:fileCount");
                        fileCountElement.setText(Long.toString(aggregatedFormats.get(formatToIdentifier)));
                    });
        }
    }

    /**
     * Adds necessary metadata to ArclibXml being updated:
     * <p>
     * 1. changes 'mets' namespace prefix to upper case 'METS'
     * 2. updates 'XML id' and 'LASTMODDATE'
     * 3. updates 'xml version number'
     * 4. updates 'xml version of'
     * 5. adds metadata modification premis:event element specifying the reason, username and time of the update
     *
     * @param xml            ArclibXml being updated
     * @param reason         reason of the update
     * @param username       name of the user that performed the update
     * @param ingestWorkflow ingest worflow
     * @return XML with updated metadata
     */
    public String addUpdateMetadata(String xml, String reason, String username, IngestWorkflow ingestWorkflow)
            throws DocumentException, IOException {
        SAXReader reader = new SAXReader();
        Document doc = reader.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        //change 'mets' namespace prefix to upper case 'METS'
        Namespace oldNs = Namespace.get(uris.get(METS));
        Namespace newNs = Namespace.get("METS", uris.get(METS));
        Visitor visitor = new NamespaceChangingVisitor(oldNs, newNs);
        doc.accept(visitor);

        //update 'XML id' and 'LASTMODDATE'
        XPath mestHdrPath = doc.createXPath("/METS:mets/METS:metsHdr");
        Element metsHdrElement = (Element) mestHdrPath.selectSingleNode(doc);
        if (metsHdrElement == null) throw new MissingNode(mestHdrPath.getText());
        metsHdrElement.addAttribute("ID", ingestWorkflow.getExternalId());
        metsHdrElement.addAttribute("LASTMODDATE", Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());

        //update 'xml version number'
        XPath xmlVersionNumberPath = doc.createXPath(
                "/mets:mets/mets:amdSec/mets:digiprovMD[@ID='ARCLIB_SIP_INFO']/mets:mdWrap/mets:xmlData/arclib:sipInfo/arclib:xmlVersionNumber");
        xmlVersionNumberPath.setNamespaceURIs(uris);
        Element xmlVersionNumberElement = (Element) xmlVersionNumberPath.selectSingleNode(doc);
        if (xmlVersionNumberElement == null) throw new MissingNode(xmlVersionNumberPath.getText());
        xmlVersionNumberElement.setText(String.valueOf(ingestWorkflow.getXmlVersionNumber()));

        //update 'xml version of'
        XPath xmlVersionOfPath = doc.createXPath(
                "/mets:mets/mets:amdSec/mets:digiprovMD[@ID='ARCLIB_SIP_INFO']/mets:mdWrap/mets:xmlData/arclib:sipInfo/arclib:xmlVersionOf");
        xmlVersionOfPath.setNamespaceURIs(uris);
        Element xmlVersionOfElement = (Element) xmlVersionOfPath.selectSingleNode(doc);
        if (xmlVersionOfElement == null) throw new MissingNode(xmlVersionOfPath.getText());
        xmlVersionOfElement.setText(ingestWorkflow.getRelatedWorkflow().getExternalId());

        XPath digiprovMdIdPath = doc.createXPath("/mets:mets/mets:amdSec/mets:digiprovMD[mets:mdWrap/mets:xmlData/premis:event]/@ID");
        digiprovMdIdPath.setNamespaceURIs(uris);
        List<Node> nodes = digiprovMdIdPath.selectNodes(doc);
        List<DefaultAttribute> digiprovMdIds = new ArrayList<>();
        for (Node node : nodes) {
            digiprovMdIds.add((DefaultAttribute) node);
        }
        int eventNumber = 1;
        Optional<DefaultAttribute> highestNumberEvent = digiprovMdIds.stream()
                .max(Comparator.comparing(DefaultAttribute::getValue));
        if (highestNumberEvent.isPresent()) {
            String highestNumberEventIdentifier = highestNumberEvent.get().getValue();
            eventNumber += Integer.parseInt(highestNumberEventIdentifier.substring(EVENT.length()));
        }

        int nextModEventNumber = 1;
        XPath modificationEventXpath = doc.createXPath("/mets:mets/mets:amdSec/mets:digiprovMD/mets:mdWrap/mets:xmlData/premis:event/premis:eventIdentifier/premis:eventIdentifierValue[starts-with(text(),'" + XML_UPDATE_PREMIS_EVENT + "')]");
        modificationEventXpath.setNamespaceURIs(uris);
        List<Node> modificationEventNodes = modificationEventXpath.selectNodes(doc);
        if (modificationEventNodes != null) {
            Optional<Node> latestModificationEvent = modificationEventNodes.stream().max(Comparator.comparing(Node::getText));
            if (latestModificationEvent.isPresent()) {
                String latestModificationEventText = latestModificationEvent.get().getText();
                String latestModEventNumberString = latestModificationEventText.substring(latestModificationEventText.lastIndexOf('_')+1);
                nextModEventNumber += Integer.parseInt(latestModEventNumberString);
            }
        }

        String eventIdentifier = EVENT + String.format(EVENT_NUMBER_FORMAT, eventNumber);
        String eventIdValue = XML_UPDATE_PREMIS_EVENT+"_event_" + String.format(EVENT_ID_NUMBER_FORMAT, nextModEventNumber);

        XPath amdSecPath = doc.createXPath("/mets:mets/mets:amdSec[mets:digiprovMD/mets:mdWrap/mets:xmlData/premis:event]");
        amdSecPath.setNamespaceURIs(uris);
        Element amdSecForEventsElement = (Element) amdSecPath.selectSingleNode(doc);

        String eventDetail = "XML was modified by user " + username + " from the reason: " + reason;
        addEvent(amdSecForEventsElement, eventIdValue, eventIdentifier, true, AGENT_ARCLIB,
                eventDetail, XML_UPDATE_PREMIS_EVENT.replace("_"," "), Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());

        log.debug("Ingest Workflow: " + ingestWorkflow.getId() + ": " + eventDetail);
        return prettyPrint(doc);
    }

    private String toEventId(IngestToolFunction f) {
        return f + "_event";
    }

    private String toAgentId(Tool tool) {
        return "agent_" + tool.getName();
    }

    @Inject
    public void setIngestWorkflowStore(IngestWorkflowStore ingestWorkflowStore) {
        this.ingestWorkflowStore = ingestWorkflowStore;
    }

    @Inject
    public void setIngestEventStore(IngestEventStore ingestEventStore) {
        this.ingestEventStore = ingestEventStore;
    }

    @Inject
    public void setUris(@Value("${namespaces.mets}") String mets, @Value("${namespaces.xsi}") String xsi, @Value("${namespaces.arclib}") String arclib, @Value("${namespaces" +
            ".premis}") String premis, @Value("${namespaces.oai_dc}") String oai_dc, @Value("${namespaces.dc}") String dc, @Value("${namespaces.xlink}") String xlink) {
        Map<String, String> uris = new HashMap<>();
        uris.put(METS, mets);
        uris.put(ARCLIB, arclib);
        uris.put(PREMIS, premis);
        uris.put(XSI, xsi);
        uris.put(OAIS_DC, oai_dc);
        uris.put(DC, dc);
        uris.put(XLINK, xlink);

        this.uris = uris;
    }

    @Inject
    public void setArclibVersion(@Value("${arclib.version}") String arclibVersion) {
        this.arclibVersion = arclibVersion;
    }

    @Inject
    public void setSipProfileService(SipProfileService sipProfileService) {
        this.sipProfileService = sipProfileService;
    }
}
