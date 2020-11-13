package cz.cas.lib.arclib.bpm;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cas.lib.arclib.domain.IngestToolFunction;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestEvent;
import cz.cas.lib.arclib.service.arclibxml.ArclibXmlValidator;
import cz.cas.lib.arclib.service.arclibxml.ArclibXmlXsltExtractor;
import cz.cas.lib.arclib.utils.NamespaceChangingVisitor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.dom4j.*;
import org.dom4j.io.SAXReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static cz.cas.lib.arclib.utils.ArclibUtils.*;

@Slf4j
@Service
public class ArclibXmlExtractorDelegate extends ArclibDelegate {

    @Getter
    private String toolName = "ARCLib_" + IngestToolFunction.metadata_extraction;
    public static final String SIP_PROFILE_CONFIG_ENTRY = "sipProfile";

    private ArclibXmlXsltExtractor arclibXmlXsltExtractor;
    private ArclibXmlValidator validator;
    private Map<String, String> uris;

    @Override
    public void executeArclibDelegate(DelegateExecution execution, String ingestWorkflowExternalId) throws TransformerException, IOException, ParserConfigurationException, SAXException, DocumentException {
        //extract metadata from original SIP using XSLT
        JsonNode configRoot = getConfigRoot(execution);
        String sipProfileExternalId = configRoot.get(SIP_PROFILE_CONFIG_ENTRY).textValue();

        String uglyPrintedExtractedMetadata = arclibXmlXsltExtractor.extractMetadata(sipProfileExternalId, execution.getVariables());

        //change 'mets' namespace prefix to upper case 'METS'
        SAXReader reader = new SAXReader();
        Document doc = reader.read(new ByteArrayInputStream(uglyPrintedExtractedMetadata.getBytes(StandardCharsets.UTF_8)));
        Namespace oldNs = Namespace.get(uris.get(METS));
        Namespace newNs = Namespace.get("METS", uris.get(METS));
        Visitor visitor = new NamespaceChangingVisitor(oldNs, newNs);
        doc.accept(visitor);
        String prettyPrintedExtractedMetadata = prettyPrint(doc);

        validator.validateXsltResult(prettyPrintedExtractedMetadata);

        execution.setVariable(BpmConstants.MetadataExtraction.result, prettyPrintedExtractedMetadata.getBytes());
        ingestEventStore.save(new IngestEvent(ingestWorkflowService.findByExternalId(ingestWorkflowExternalId), toolService.getByNameAndVersion(getToolName(), getToolVersion()), true, null));
    }

    @Inject
    public void setArclibXmlXsltExtractor(ArclibXmlXsltExtractor arclibXmlXsltExtractor) {
        this.arclibXmlXsltExtractor = arclibXmlXsltExtractor;
    }

    @Inject
    public void setValidator(ArclibXmlValidator validator) {
        this.validator = validator;
    }

    @Inject
    public void setUris(@Value("${namespaces.mets}") String mets, @Value("${namespaces.xsi}") String xsi, @Value("${namespaces.arclib}") String arclib, @Value("${namespaces" +
            ".premis}") String premis, @Value("${namespaces.oai_dc}") String oai_dc, @Value("${namespaces.dc}") String dc) {
        Map<String, String> uris = new HashMap<>();
        uris.put(METS, mets);
        uris.put(ARCLIB, arclib);
        uris.put(PREMIS, premis);
        uris.put(XSI, xsi);
        uris.put(OAIS_DC, oai_dc);
        uris.put(DC, dc);

        this.uris = uris;
    }
}
