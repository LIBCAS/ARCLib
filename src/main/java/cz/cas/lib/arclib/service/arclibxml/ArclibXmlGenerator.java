package cz.cas.lib.arclib.service.arclibxml;

import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestIssue;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.packages.FolderStructure;
import cz.cas.lib.arclib.domain.packages.Sip;
import cz.cas.lib.arclib.exception.validation.MissingNode;
import cz.cas.lib.arclib.store.IngestIssueStore;
import cz.cas.lib.arclib.store.IngestWorkflowStore;
import cz.cas.lib.arclib.utils.NamespaceChangingVisitor;
import cz.cas.lib.core.store.Transactional;
import cz.cas.lib.core.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.*;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.DefaultAttribute;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static cz.cas.lib.arclib.utils.ArclibUtils.*;

@Slf4j
@Service
public class ArclibXmlGenerator {
    public static final String INGESTION_EVENT = "ingestion_event";
    public static final String VALIDATION_EVENT = "validation_event";
    public static final String QUARANTINE_EVENT = "quarantine_event";
    public static final String MESSAGE_DIGEST_CALCULATION_EVENT = "message_digest_calculation_event";
    public static final String METADATA_EXTRACTION_EVENT = "metadata_extraction_event";
    public static final String FIXITY_CHECK_EVENT = "fixity_check_event";
    public static final String FORMAT_IDENTIFICATION_EVENT = "format_identification_event";
    public static final String VIRUS_CHECK_EVENT = "virus_check_event";
    public static final String METADATA_MODIFICATION_EVENT = "metadata_modification_event";

    public static final String INITIAL_VERSION = "initial version";
    public static final String EVENT = "EVENT_";
    public static final String AGENT_ARCLIB = "agent_ARCLIB";
    public static final String EVENT_NUMBER_FORMAT = "%03d";
    public static final String OBJ_NUMBER_FORMAT = "%03d";
    public static final String AGENT_NUMBER_FORMAT = "%03d";

    private IngestWorkflowStore ingestWorkflowStore;
    private Map<String, String> uris;
    private String arclibVersion;
    private IngestIssueStore ingestIssueStore;

    private List<String> events;
    private List<Utils.Pair<String, String>> filePathsAndObjIdentifiers;

    /**
     * Supplements ArclibXml with generated metadata
     *
     * @param xml       ARCLibXml
     * @param variables BPM variables from Camunda execution
     * @return ARCLibXml supplemented by generated metadata
     * @throws DocumentException provided <code>xml</code> could not be parsed
     */
    @Transactional
    public Document generateMetadata(String xml, Map<String, Object> variables) throws DocumentException {
        events = new ArrayList<>();
        filePathsAndObjIdentifiers = new ArrayList<>();

        String ingestWorkflowExternalId = (String) variables.get(BpmConstants.ProcessVariables.ingestWorkflowExternalId);
        log.info("Generating metadata for ingest workflow with external id " + ingestWorkflowExternalId + ".");
        IngestWorkflow ingestWorkflow = ingestWorkflowStore.findByExternalId(ingestWorkflowExternalId);

        List<Utils.Pair<String, String>> filePathsAndFileSizes = (List<Utils.Pair<String, String>>)
                variables.get(BpmConstants.Ingestion.filePathsAndFileSizes);

        //assign each file and object identifier
        List<String> filePaths = filePathsAndFileSizes.stream().map(Utils.Pair::getL).collect(Collectors.toList());
        for (int i = 0; i < filePaths.size(); i++) {
            String objIdentifier = "obj-" + String.format(OBJ_NUMBER_FORMAT, i + 1);
            filePathsAndObjIdentifiers.add(new Utils.Pair<>(filePaths.get(i), objIdentifier));
        }

        SAXReader reader = new SAXReader();
        Document doc = reader.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        //change 'mets' namespace prefix to upper case 'METS'
        Namespace oldNs = Namespace.get(uris.get(METS));
        Namespace newNs = Namespace.get("METS", uris.get(METS));
        Visitor visitor = new NamespaceChangingVisitor(oldNs, newNs);
        doc.accept(visitor);

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
        XPath xmlDataXPath = doc.createXPath("/METS:mets/METS:dmdSec/METS:mdWrap[@MDTYPE='DC']/METS:xmlData");
        Element xmlDataElement = (Element) xmlDataXPath.selectSingleNode(doc);
        if (xmlDataElement == null) throw new MissingNode(xmlDataXPath.getText());
        addSipAndXmlVersion(xmlDataElement, ingestWorkflow);

        /*
          Add premis:agents and respective premis:events
         */
        Element element = addPremisAgentsAndEvents(metsElement, variables);
        int positionOfPremisAgentsAndEvents = metsElement.elements().indexOf(element);

        /*
          Add premis:object for whole package
         */
        //place the element with premis object before the element with premis agents and events
        addPremisObject(metsElement, variables, positionOfPremisAgentsAndEvents);

        /*
          Add METS:fileSec
         */
        addFileSec(metsElement, variables);

        /*
          Add METS:structMap
         */
        addStructMap(metsElement, ingestWorkflow.getSip());
        return doc;
    }

    private void fillMetsHdr(Element metsHdrElement, IngestWorkflow ingestWorkflow, Map<String, Object> variables) {
        String xmlId = (String) variables.get(BpmConstants.ProcessVariables.ingestWorkflowExternalId);
        String authorialId = (String) variables.get(BpmConstants.Ingestion.authorialId);

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

    private void addSipAndXmlVersion(Element xmlDataElement, IngestWorkflow ingestWorkflow) {
        Element sipVersionNumber = xmlDataElement.addElement("dcterms:sipVersionNumber", uris.get(DCTERMS));
        sipVersionNumber.addText(String.valueOf(ingestWorkflow.getSip().getVersionNumber()));

        Sip previousVersionSip = ingestWorkflow.getSip().getPreviousVersionSip();
        String previousVersionSipId = previousVersionSip != null ? previousVersionSip.getId() : INITIAL_VERSION;
        Element sipVersionOf = xmlDataElement.addElement("dcterms:sipVersionOf", uris.get(DCTERMS));
        sipVersionOf.addText(previousVersionSipId);

        Element xmlVersionNumber = xmlDataElement.addElement("dcterms:xmlVersionNumber", uris.get(DCTERMS));
        xmlVersionNumber.addText(String.valueOf(ingestWorkflow.getXmlVersionNumber()));

        IngestWorkflow relatedWorkflow = ingestWorkflow.getRelatedWorkflow();
        String previousVersionXmlId = relatedWorkflow != null && ingestWorkflow.getXmlVersionNumber() > 1
                ? relatedWorkflow.getExternalId() : INITIAL_VERSION;
        Element xmlVersionOf = xmlDataElement.addElement("dcterms:xmlVersionOf", uris.get(DCTERMS));
        xmlVersionOf.addText(previousVersionXmlId);
    }

    /**
     * @return created METS:amdSec element to which the agents and events are added
     */
    private Element addPremisAgentsAndEvents(Element metsElement, Map<String, Object> variables) {
        /*
          Add premis:agent elements
         */
        Element amdSecElement = metsElement.addElement("METS:amdSec");
        int agentCounter = 1;

        //ARCLib agent
        String arclibEventAgentIdentifier = AGENT_ARCLIB;
        addEventAgent(amdSecElement, agentCounter++, arclibEventAgentIdentifier, "ARCLIB",
                "software", "Version " + arclibVersion);

        //format identifier agent
        String formatIdentificationAgentIdentifier = "";
        String formatIdentificationToolName = (String) variables.get(BpmConstants.FormatIdentification.toolName);
        if (formatIdentificationToolName != null) {
            formatIdentificationAgentIdentifier = "agent_" + formatIdentificationToolName;
            addEventAgent(amdSecElement, agentCounter++, formatIdentificationAgentIdentifier, formatIdentificationToolName,
                    "software", (String) variables.get(BpmConstants.FormatIdentification.toolVersion));
        }

        //virus check agent
        String virusCheckAgentIdentifier = "";
        String virusCheckToolName = (String) variables.get(BpmConstants.VirusCheck.toolName);
        if (virusCheckToolName != null) {
            virusCheckAgentIdentifier = "agent_" + virusCheckToolName;
            addEventAgent(amdSecElement, agentCounter++, virusCheckAgentIdentifier, virusCheckToolName,
                    "software", (String) variables.get(BpmConstants.VirusCheck.toolVersion));
        }

        /*
          Add premis:event elements
         */
        Element amdSecForEventsElement = metsElement.addElement("METS:amdSec");
        int eventCounter = 1;

        //ingestion event
        if (variables.get(BpmConstants.Ingestion.success) != null) {
            events.add(INGESTION_EVENT);
            addEvent(amdSecForEventsElement, INGESTION_EVENT, EVENT + String.format("%03d", eventCounter++),
                    (Boolean) variables.get(BpmConstants.Ingestion.success),
                    arclibEventAgentIdentifier, "Original name of the SIP package: " +
                            variables.get(BpmConstants.Ingestion.originalSipFileName), "ingestion",
                    (String) variables.get(BpmConstants.Ingestion.dateTime));
        }

        //validation event
        if (variables.get(BpmConstants.Validation.success) != null) {
            events.add(VALIDATION_EVENT);
            addEvent(amdSecForEventsElement, VALIDATION_EVENT, EVENT + String.format("%03d", eventCounter++),
                    (Boolean) variables.get(BpmConstants.Validation.success),
                    arclibEventAgentIdentifier, null, "validation",
                    (String) variables.get(BpmConstants.Validation.dateTime));
        }

        int fixityEventCounter = 1;
        //fixity check events
        if (variables.get(BpmConstants.FixityCheck.success) != null) {
            String fixityEventIdentifier = FIXITY_CHECK_EVENT + "_" + String.format("%03d", fixityEventCounter++);
            events.add(fixityEventIdentifier);

            addEvent(amdSecForEventsElement, fixityEventIdentifier, EVENT + String.format("%03d", eventCounter++),
                    (Boolean) variables.get(BpmConstants.FixityCheck.success),
                    arclibEventAgentIdentifier, null, "fixity check",
                    (String) variables.get(BpmConstants.FixityCheck.dateTime));
        }

        List<IngestIssue> fixityCheckIssues = ingestIssueStore.findByTaskExecutorAndExternalId(
                BpmConstants.FixityCheck.class, (String) variables.get(BpmConstants.ProcessVariables.ingestWorkflowExternalId));
        for (IngestIssue issue : fixityCheckIssues) {
            String fixityEventIdentifier = FIXITY_CHECK_EVENT + "_" + String.format("%03d", fixityEventCounter++);
            events.add(fixityEventIdentifier);

            String eventDetail = "Issue: " + issue.getIssue() + " Config: " + issue.getConfigNote();
            addEvent(amdSecForEventsElement,
                    fixityEventIdentifier,
                    EVENT + String.format(EVENT_NUMBER_FORMAT, eventCounter++),
                    issue.isSolvedByConfig(), arclibEventAgentIdentifier, eventDetail, "fixity check",
                    issue.getUpdated().truncatedTo(ChronoUnit.SECONDS).toString());
        }

        int formatIdentificationCounter = 1;
        //format identification events
        if (variables.get(BpmConstants.FormatIdentification.success) != null) {
            String formatIdentificationEventIdentifier = FORMAT_IDENTIFICATION_EVENT + "_" + String.format("%03d", formatIdentificationCounter++);
            events.add(formatIdentificationEventIdentifier);

            addEvent(amdSecForEventsElement,
                    formatIdentificationAgentIdentifier,
                    EVENT + String.format(EVENT_NUMBER_FORMAT, eventCounter++),
                    (Boolean) variables.get(BpmConstants.FormatIdentification.success),
                    formatIdentificationAgentIdentifier, null, "format identification",
                    (String) variables.get(BpmConstants.FormatIdentification.dateTime));
        }
        List<IngestIssue> formatIdentificationIssues = ingestIssueStore.findByTaskExecutorAndExternalId(
                BpmConstants.FormatIdentification.class, (String) variables.get(BpmConstants.ProcessVariables.ingestWorkflowExternalId));
        for (IngestIssue issue : formatIdentificationIssues) {
            String eventDetail = "Issue: " + issue.getIssue() + " Config: " + issue.getConfigNote();

            String formatIdentificationEventIdentifier = FORMAT_IDENTIFICATION_EVENT + "_" + String.format("%03d", formatIdentificationCounter++);
            events.add(formatIdentificationEventIdentifier);

            addEvent(amdSecForEventsElement,
                    formatIdentificationEventIdentifier,
                    EVENT + String.format(EVENT_NUMBER_FORMAT, eventCounter++),
                    issue.isSolvedByConfig(), formatIdentificationAgentIdentifier, eventDetail, "format identification",
                    issue.getUpdated().truncatedTo(ChronoUnit.SECONDS).toString());
        }

        //metadata extraction event
        if (variables.get(BpmConstants.MetadataExtraction.success) != null) {
            events.add(METADATA_EXTRACTION_EVENT);
            addEvent(amdSecForEventsElement, METADATA_EXTRACTION_EVENT, EVENT + String.format("%03d", eventCounter++),
                    (Boolean) variables.get(BpmConstants.MetadataExtraction.success),
                    arclibEventAgentIdentifier, null, "metadata extraction",
                    (String) variables.get(BpmConstants.MetadataExtraction.dateTime));
        }

        int virusCheckEventCounter = 1;
        //virus check events
        if (variables.get(BpmConstants.VirusCheck.success) != null) {
            String virusCheckEventIdentifier = VIRUS_CHECK_EVENT + "_" + String.format("%03d", virusCheckEventCounter++);
            events.add(virusCheckEventIdentifier);

            addEvent(amdSecForEventsElement, virusCheckEventIdentifier,
                    EVENT + String.format(EVENT_NUMBER_FORMAT, eventCounter++),
                    (Boolean) variables.get(BpmConstants.VirusCheck.success),
                    virusCheckAgentIdentifier, null, "virus check",
                    (String) variables.get(BpmConstants.VirusCheck.dateTime));
        }
        List<IngestIssue> virusCheckIssues = ingestIssueStore.findByTaskExecutorAndExternalId(
                BpmConstants.VirusCheck.class, (String) variables.get(BpmConstants.ProcessVariables.ingestWorkflowExternalId));
        for (IngestIssue issue : virusCheckIssues) {
            String eventDetail = "Issue: " + issue.getIssue() + " Config: " + issue.getConfigNote();

            String virusCheckEventIdentifier = VIRUS_CHECK_EVENT + "_" + String.format("%03d", virusCheckEventCounter++);
            events.add(virusCheckEventIdentifier);

            addEvent(amdSecForEventsElement, virusCheckEventIdentifier,
                    EVENT + String.format(EVENT_NUMBER_FORMAT, eventCounter++),
                    issue.isSolvedByConfig(), virusCheckAgentIdentifier, eventDetail, "virus check",
                    issue.getUpdated().truncatedTo(ChronoUnit.SECONDS).toString());
        }

        //quarantine event
        if (variables.get(BpmConstants.Quarantine.success) != null) {
            events.add(QUARANTINE_EVENT);
            addEvent(amdSecForEventsElement, QUARANTINE_EVENT, EVENT + String.format("%03d", eventCounter++),
                    (Boolean) variables.get(BpmConstants.Quarantine.success),
                    arclibEventAgentIdentifier, null, "quarantine",
                    (String) variables.get(BpmConstants.Quarantine.dateTime));
        }

        //message digest calculation event
        if (variables.get(BpmConstants.MessageDigestCalculation.success) != null) {
            events.add(MESSAGE_DIGEST_CALCULATION_EVENT);
            addEvent(amdSecForEventsElement, MESSAGE_DIGEST_CALCULATION_EVENT,
                    EVENT + String.format(EVENT_NUMBER_FORMAT, eventCounter++),
                    (Boolean) variables.get(BpmConstants.MessageDigestCalculation.success),
                    arclibEventAgentIdentifier, null, "message digest calculation",
                    (String) variables.get(BpmConstants.MessageDigestCalculation.dateTime));
        }

        return amdSecElement;
    }

    /**
     * @param position position of the amdSecElement to be created in withing METS file
     */
    private void addPremisObject(Element metsElement, Map<String, Object> variables, int position) {
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
        objectElement.addAttribute("xsi:type", "premis:file");

        Element objectIdentifierElement = objectElement.addElement("premis:objectIdentifier");
        Element objectIdentifierType = objectIdentifierElement.addElement("premis:objectIdentifierType");
        objectIdentifierType.addText("local");
        Element objectIdentifierValue = objectIdentifierElement.addElement("premis:objectIdentifierValue");
        objectIdentifierValue.addText("obj-package");

        Element objectCharacteristicsElement = objectElement.addElement("premis:objectCharacteristics");
        Element compositionLevelElement = objectCharacteristicsElement.addElement("premis:compositionLevel");
        compositionLevelElement.addText("0");

        if (variables.get(BpmConstants.MessageDigestCalculation.success) != null) {
            addFixity(objectCharacteristicsElement, "MD5", (String) variables.get(BpmConstants.MessageDigestCalculation.checksumMd5));
            addFixity(objectCharacteristicsElement, "CRC32", (String) variables.get(BpmConstants.MessageDigestCalculation.checksumCrc32));
            addFixity(objectCharacteristicsElement, "SHA-512", (String) variables.get(BpmConstants.MessageDigestCalculation.checksumSha512));
    }

        Element sizeElement = objectCharacteristicsElement.addElement("premis:size");
        long sizeInBytes = (long) (variables.get(BpmConstants.Ingestion.sizeInBytes));
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

        for (String eventIdentifier : events) {
            Element linkingEventIdentifierElement = objectElement.addElement("premis:linkingEventIdentifier");
            Element linkingEventIdentifierTypeElement = linkingEventIdentifierElement.addElement("premis:linkingEventIdentifierType");
            linkingEventIdentifierTypeElement.addText("EventId");
            Element linkingEventIdentifierValueElement = linkingEventIdentifierElement.addElement("premis:linkingEventIdentifierValue");
            linkingEventIdentifierValueElement.addText(eventIdentifier);
        }
    }

    private void addStructMap(Element metsElement, Sip sip) {
        Element structMapElement = metsElement.addElement("METS:structMap");
        structMapElement.addAttribute("ID", "Physical_Structure");
        structMapElement.addAttribute("TYPE", "PHYSICAL");

        Element aipDivElement = structMapElement.addElement("METS:div");
        aipDivElement.addAttribute("TYPE", "Archival Information Package");

        FolderStructure sipFolderStructure = sip.getFolderStructure();
        addStructMapDivElementsRecursively(aipDivElement, sipFolderStructure, sipFolderStructure.getCaption());
    }

    private void addStructMapDivElementsRecursively(Element parentDivElement, FolderStructure folderStructure, String parentFolderStructurePath) {
        Collection<FolderStructure> children = folderStructure.getChildren();
        if (children == null) {
            //folder structure represents a file
            Utils.Pair<String, String> filePathAndObjectIdentifier = filePathsAndObjIdentifiers.stream()
                    .filter(pair -> {
                        String folderStructurePath = parentFolderStructurePath.substring(parentFolderStructurePath.indexOf("/") + 1);
                        return pair.getL().equals(folderStructurePath);
                    })
                    .findFirst().get();
            Element fptrElement = parentDivElement.addElement("METS:fptr");
            fptrElement.addAttribute("FILEID", filePathAndObjectIdentifier.getR());
        } else {
            //folder structure represents a directory
            Element divElement = parentDivElement.addElement("METS:div");
            divElement.addAttribute("TYPE", "directory");

            divElement.addAttribute("LABEL", parentFolderStructurePath);
            children.stream()
                    .sorted(Comparator.comparing(fs -> fs.getChildren() != null))
                    .forEach(childFolderStructure -> addStructMapDivElementsRecursively(divElement, childFolderStructure,
                            parentFolderStructurePath + "/" + childFolderStructure.getCaption()));
        }
    }

    private void addFileSec(Element metsElement, Map<String, Object> variables) {
        Element fileSecElement = metsElement.addElement("METS:fileSec");
        Element fileGrpElement = fileSecElement.addElement("METS:fileGrp");
        fileGrpElement.addAttribute("USE", "file");

        if (variables.get(BpmConstants.FixityCheck.success) != null) {
            List<Utils.Pair<String, String>> filePathsAndFileSizes =
                    (List<Utils.Pair<String, String>>) variables.get(BpmConstants.Ingestion.filePathsAndFileSizes);

            List<Utils.Triplet<String, String, String>> filePathsAndFileFixities =
                    (List<Utils.Triplet<String, String, String>>) variables.get(BpmConstants.FixityCheck.filePathsAndFixities);
            //merge list of fixities computed during fixity verification with fixities for files located in the root folder
            filePathsAndFileFixities.addAll(
                    (List<Utils.Triplet<String, String, String>>) variables.get(BpmConstants.Ingestion.rootDirFilesAndFixities));

            for (Utils.Pair<String, String> filePathAndObjIdentifier : filePathsAndObjIdentifiers) {
                Utils.Triplet<String, String, String> filePathAndFixity = filePathsAndFileFixities.stream()
                        .filter(triplet -> {
                            String filePath = triplet.getT();
                            return filePath.equals(filePathAndObjIdentifier.getL());
                        })
                        .findFirst().get();
                String fixityType = filePathAndFixity.getU();
                String fixityValue = filePathAndFixity.getV();

                Element fileElement = fileGrpElement.addElement("METS:file");

                fileElement.addAttribute("ID", filePathAndObjIdentifier.getR());

                Element fLocatElement = fileElement.addElement("METS:FLocat");
                fLocatElement.addAttribute("LOCTYPE", "OTHER");
                fLocatElement.addAttribute("xlink:href", filePathAndObjIdentifier.getL());

                fileElement.addAttribute("CHECKSUMTYPE", fixityType);
                fileElement.addAttribute("CHECKSUM", fixityValue);

                Utils.Pair<String, String> filePathAndSize = filePathsAndFileSizes.stream()
                        .filter(pair -> pair.getL().equals(filePathAndObjIdentifier.getL()))
                        .findFirst().get();
                fileElement.addAttribute("SIZE", filePathAndSize.getR());
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
        Element arclibFormatsElement = xmlDataElement.addElement("ARCLIB:formats", uris.get(ARCLIB));

        if (variables.get(BpmConstants.FormatIdentification.success) != null) {
            TreeMap<String, Utils.Pair<String, String>> identifiedFormats =
                    (TreeMap<String, Utils.Pair<String, String>>) variables.get(BpmConstants.FormatIdentification.mapOfFilesToFormats);
            Map<Utils.Pair<String, String>, Long> aggregatedFormats =
                    computeAggregatedCount(identifiedFormats.values());

            aggregatedFormats.keySet()
                .forEach(formatToIdentifier -> {
                    Element arclibFormatElement = arclibFormatsElement.addElement("ARCLIB:format");

                    Element formatRegistryKeyElement = arclibFormatElement.addElement("ARCLIB:formatRegistryKey");
                    formatRegistryKeyElement.setText(formatToIdentifier.getL());

                    Element formatRegistryNameElement = arclibFormatElement.addElement("ARCLIB:formatRegistryName");
                    formatRegistryNameElement.setText("PRONOM");

                    Element creatingApplicationNameElement =
                            arclibFormatElement.addElement("ARCLIB:creatingApplicationName");
                    creatingApplicationNameElement.setText((String) variables.get(BpmConstants.FormatIdentification.toolName));

                    Element creatingApplicationVersionElement =
                            arclibFormatElement.addElement("ARCLIB:creatingApplicationVersion");
                    creatingApplicationVersionElement.setText((String) variables.get(BpmConstants.FormatIdentification.toolVersion));

                    Element dateCreatedByApplicationElement =
                            arclibFormatElement.addElement("ARCLIB:dateCreatedByApplication");
                    dateCreatedByApplicationElement.setText(((String) variables.get(BpmConstants.FormatIdentification.dateTime)).substring(0, 10));

                    Element fileCountElement = arclibFormatElement.addElement("ARCLIB:fileCount");
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
                "/mets:mets/mets:dmdSec/mets:mdWrap/mets:xmlData/dcterms:xmlVersionNumber");
        xmlVersionNumberPath.setNamespaceURIs(uris);
        Element xmlVersionNumberElement = (Element) xmlVersionNumberPath.selectSingleNode(doc);
        if (xmlVersionNumberElement == null) throw new MissingNode(xmlVersionNumberPath.getText());
        xmlVersionNumberElement.setText(String.valueOf(ingestWorkflow.getXmlVersionNumber()));

        //update 'xml version of'
        XPath xmlVersionOfPath = doc.createXPath(
                "/mets:mets/mets:dmdSec/mets:mdWrap/mets:xmlData/dcterms:xmlVersionOf");
        xmlVersionOfPath.setNamespaceURIs(uris);
        Element xmlVersionOfElement = (Element) xmlVersionOfPath.selectSingleNode(doc);
        if (xmlVersionOfElement == null) throw new MissingNode(xmlVersionOfPath.getText());
        xmlVersionOfElement.setText(ingestWorkflow.getRelatedWorkflow().getExternalId());

        XPath digiprovMdIdPath = doc.createXPath("/mets:mets/mets:amdSec/mets:digiprovMD[mets:mdWrap/mets:xmlData/premis:event]/@ID");
        digiprovMdIdPath.setNamespaceURIs(uris);
        List<DefaultAttribute> digiprovMdIds = (List<DefaultAttribute>) digiprovMdIdPath.selectNodes(doc);

        int eventNumber = 1;
        Optional<DefaultAttribute> highestNumberEvent = digiprovMdIds.stream()
                .max(Comparator.comparing(DefaultAttribute::getValue));
        if (highestNumberEvent.isPresent()) {
            String highestNumberEventIdentifier = highestNumberEvent.get().getValue();
            eventNumber += Integer.parseInt(highestNumberEventIdentifier.substring(EVENT.length()));
        }
        String eventIdentifier = EVENT + String.format(EVENT_NUMBER_FORMAT, eventNumber);

        XPath amdSecPath = doc.createXPath("/mets:mets/mets:amdSec[mets:digiprovMD/mets:mdWrap/mets:xmlData/premis:event]");
        amdSecPath.setNamespaceURIs(uris);
        Element amdSecForEventsElement = (Element) amdSecPath.selectSingleNode(doc);

        String eventDetail = "XML was modified by user " + username + " from the reason: " + reason;
        addEvent(amdSecForEventsElement, METADATA_MODIFICATION_EVENT, eventIdentifier, true, AGENT_ARCLIB,
                eventDetail, "metadata modification", Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());

        xml = prettyPrint(doc);
        log.info("Updated ARCLib XML: \n" + xml);
        return xml;
    }

    @Inject
    public void setUris(@Value("${namespaces.mets}") String mets, @Value("${namespaces.arclib}") String arclib, @Value("${namespaces" +
            ".premis}") String premis, @Value("${namespaces.oai_dc}") String oai_dc, @Value("${namespaces.dc}") String dc,
                        @Value("${namespaces.dcterms}") String dcterms) {
        Map<String, String> uris = new HashMap<>();
        uris.put(METS, mets);
        uris.put(ARCLIB, arclib);
        uris.put(PREMIS, premis);
        uris.put(OAIS_DC, oai_dc);
        uris.put(DC, dc);
        uris.put(DCTERMS, dcterms);

        this.uris = uris;
    }

    @Inject
    public void setIngestWorkflowStore(IngestWorkflowStore ingestWorkflowStore) {
        this.ingestWorkflowStore = ingestWorkflowStore;
    }

    @Inject
    public void setIngestIssueStore(IngestIssueStore ingestIssueStore) {
        this.ingestIssueStore = ingestIssueStore;
    }

    @Inject
    public void setArclibVersion(@Value("${arclib.version}") String arclibVersion) {
        this.arclibVersion = arclibVersion;
    }
}
