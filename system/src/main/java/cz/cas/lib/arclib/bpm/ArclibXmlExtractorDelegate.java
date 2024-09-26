package cz.cas.lib.arclib.bpm;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cas.lib.arclib.domain.IngestToolFunction;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestEvent;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.preservationPlanning.Tool;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.exception.bpm.ConfigParserException;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import cz.cas.lib.arclib.service.arclibxml.ArclibXmlValidator;
import cz.cas.lib.arclib.service.arclibxml.ArclibXmlXsltExtractor;
import cz.cas.lib.arclib.service.arclibxml.systemWideValidation.SystemWideValidationMissingNodesBpmHandler;
import cz.cas.lib.arclib.service.arclibxml.systemWideValidation.SystemWideValidationNodeConfig;
import cz.cas.lib.arclib.store.ProducerProfileStore;
import cz.cas.lib.arclib.utils.ArclibUtils;
import cz.cas.lib.arclib.utils.NamespaceChangingVisitor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Namespace;
import org.dom4j.Visitor;
import org.dom4j.io.SAXReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;


import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cz.cas.lib.arclib.utils.ArclibUtils.*;
import static cz.cas.lib.core.util.Utils.isNull;

@Slf4j
@Service
public class ArclibXmlExtractorDelegate extends ArclibDelegate {

    @Getter
    private String toolName = "ARCLib_" + IngestToolFunction.metadata_extraction;
    public static final String SIP_PROFILE_CONFIG_ENTRY = "sipProfile";

    private ArclibXmlXsltExtractor arclibXmlXsltExtractor;
    private ProducerProfileStore producerProfileStore;
    private ArclibXmlValidator validator;
    private Map<String, String> uris;
    private SystemWideValidationMissingNodesBpmHandler systemWideValidationHandler;

    @Override
    public void executeArclibDelegate(DelegateExecution execution) throws TransformerException, IOException, ParserConfigurationException, SAXException, DocumentException, XPathExpressionException, IncidentException {
        String usedSipProfile = getStringVariable(execution, BpmConstants.MetadataExtraction.usedSipProfile);
        isNull(usedSipProfile, () -> new BpmnError(BpmConstants.ErrorCodes.ProcessFailure, ArclibUtils.trimBpmnErrorMsg(
                "Attempting to start ArclibXmlExtractor but the usedSipProfile variable is already filled. Workflow definition can contain only one instance of the extractor.")));

        //extract metadata from original SIP using XSLT
        JsonNode configRoot = getConfigRoot(execution);
        String sipProfileExternalId;
        JsonNode sipProfileConfigEntry = configRoot.at("/" + SIP_PROFILE_CONFIG_ENTRY);
        if (sipProfileConfigEntry.isMissingNode()) {
            String producerProfileExternalId = getProducerProfileExternalId(execution);
            ProducerProfile producerProfile = producerProfileStore.findByExternalId(producerProfileExternalId);
            sipProfileExternalId = producerProfile.getSipProfile().getExternalId();
        } else {
            sipProfileExternalId = sipProfileConfigEntry.textValue();
        }

        String uglyPrintedExtractedMetadata = arclibXmlXsltExtractor.extractMetadata(sipProfileExternalId, execution.getVariables());

        //change 'mets' namespace prefix to upper case 'METS'
        SAXReader reader = new SAXReader();
        Document doc = reader.read(new ByteArrayInputStream(uglyPrintedExtractedMetadata.getBytes(StandardCharsets.UTF_8)));
        Namespace oldNs = Namespace.get(uris.get(METS));
        Namespace newNs = Namespace.get("METS", uris.get(METS));
        Visitor visitor = new NamespaceChangingVisitor(oldNs, newNs);
        doc.accept(visitor);
        String prettyPrintedExtractedMetadata = prettyPrint(doc);

        IngestWorkflow iw = ingestWorkflowService.findByExternalId(getIngestWorkflowExternalId(execution));
        Tool tool = toolService.getByNameAndVersion(getToolName(), getToolVersion());

        List<SystemWideValidationNodeConfig> missingNodes = validator.validateXsltResult(prettyPrintedExtractedMetadata);
        if (!missingNodes.isEmpty()) {
            systemWideValidationHandler.handleMissingNodes(configRoot, true, missingNodes, iw, tool);
        }

        ingestEventStore.save(new IngestEvent(iw, tool, true, null));
        execution.setVariable(BpmConstants.MetadataExtraction.result, prettyPrintedExtractedMetadata.getBytes());
        execution.setVariable(BpmConstants.MetadataExtraction.usedSipProfile, sipProfileExternalId);
    }

    @Autowired
    public void setProducerProfileStore(ProducerProfileStore producerProfileStore) {
        this.producerProfileStore = producerProfileStore;
    }

    @Autowired
    public void setArclibXmlXsltExtractor(ArclibXmlXsltExtractor arclibXmlXsltExtractor) {
        this.arclibXmlXsltExtractor = arclibXmlXsltExtractor;
    }

    @Autowired
    public void setValidator(ArclibXmlValidator validator) {
        this.validator = validator;
    }

    @Autowired
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

    @Autowired
    public void setSystemWideValidationHandler(SystemWideValidationMissingNodesBpmHandler systemWideValidationHandler) {
        this.systemWideValidationHandler = systemWideValidationHandler;
    }
}
