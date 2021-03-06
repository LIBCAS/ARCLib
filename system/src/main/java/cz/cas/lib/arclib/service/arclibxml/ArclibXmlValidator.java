package cz.cas.lib.arclib.service.arclibxml;

import cz.cas.lib.arclib.exception.validation.InvalidXmlNodeValue;
import cz.cas.lib.arclib.exception.validation.MissingNode;
import cz.cas.lib.arclib.utils.XmlUtils;
import cz.cas.lib.core.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static cz.cas.lib.arclib.utils.ArclibUtils.*;

@Slf4j
@Service
public class ArclibXmlValidator {

    /**
     * @see ArclibXmlGenerator
     */
    public static final String PATH_TO_AIP_ID = "/mets/@OBJID";
    public static final String PATH_TO_AUTHORIAL_ID = "/mets/metsHdr/altRecordID[@TYPE='original SIP identifier']";
    public static final String PATH_TO_SIP_VERSION_NUMBER = "/mets/amdSec/digiprovMD[@ID='ARCLIB_SIP_INFO']/mdWrap/xmlData/sipInfo/sipVersionNumber";
    public static final String PATH_TO_PHYSICAL_VERSION_OF = "/mets/amdSec/digiprovMD[@ID='ARCLIB_SIP_INFO']/mdWrap/xmlData/sipInfo/sipVersionOf";

    private Resource arclibXmlDefinition;
    private Resource arclibXmlSchema;
    private Resource metsSchema;
    private Resource premisSchema;

    private Map<String, String> uris;

    /**
     * Validates the structure of ARCLib XML.
     * 1. ARCLib XML is validated against XML schemas: <i>arclibXmlSchema, metsSchema, premisSchema</i>
     * 2. Validator checks presence of the given nodes in ARCLib XML according to the XPaths specified
     * in the compulsory elements (only the ones generated by XSLT) of ARCLib XML in the file <i>index/arclibXmlDefinition.csv</i>.
     *
     * @param xml ARCLib XML to validate
     * @throws IOException                  if file with ARCLib XML validation checks does not exist
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public void validateXsltResult(String xml) throws IOException, ParserConfigurationException, SAXException {
        log.debug("Starting validation of ARCLib XML after Sip Profile XSLT.");
        String xmlWithMockedStructMap = xml.replace("</METS:mets>", "<METS:structMap><METS:div/></METS:structMap></METS:mets>");
        validateAgainstXsds(xmlWithMockedStructMap);
        validateNodesRequiredByCsv(xml, true);
        log.debug("Validation of ArclibXml after Sip Profile XSLT succeeded.");
    }

    /**
     * Validates the structure of ARCLib XML.
     * 1. ARCLib XML is validated against XML schemas: <i>arclibXmlSchema, metsSchema, premisSchema</i>
     * 2. Validator checks presence of the given nodes in ARCLib XML according to the XPaths specified
     * in the compulsory elements of ARCLib XML in the file <i>index/arclibXmlDefinition.csv</i>.
     * 3. Values of aipId, authorialId, sipVersionNumber and sipVersionOf nodes are validated
     *
     * @param xml              ARCLib XML to validate
     * @param aipId            id of the AIP
     * @param authorialId      authorial id of the authorial package
     * @param sipVersionNumber number of version of SIP
     * @param sipVersionOf     id of the previous version SIP
     * @throws IOException                  if file with ARCLib XML validation checks does not exist
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public void validateFinalXml(String xml, String aipId, String authorialId, Integer sipVersionNumber,
                                 String sipVersionOf) throws IOException, ParserConfigurationException, SAXException {
        log.debug("Starting final validation of ARCLib XML.");
        validateAgainstXsds(xml);
        validateNodesRequiredByCsv(xml, false);

        ByteArrayInputStream xmlBais = new ByteArrayInputStream(xml.getBytes());
        log.debug("Checking content of node with AIP id at XPath " + PATH_TO_AIP_ID + ".");
        xmlBais.reset();
        Node aipIdNode = XmlUtils.findSingleNodeWithXPath(xmlBais, PATH_TO_AIP_ID, null);
        Utils.eq(aipIdNode.getTextContent(), aipId, () -> new InvalidXmlNodeValue(aipId,
                aipIdNode.getTextContent(), PATH_TO_AIP_ID));

        log.debug("Checking content of node with authorial id at XPath " + PATH_TO_AUTHORIAL_ID + ".");
        xmlBais.reset();
        Node authorialIdNode = XmlUtils.findSingleNodeWithXPath(xmlBais, PATH_TO_AUTHORIAL_ID, null);
        Utils.eq(authorialIdNode.getTextContent(), authorialId, () -> new InvalidXmlNodeValue(authorialId,
                authorialIdNode.getTextContent(), PATH_TO_AUTHORIAL_ID));

        log.debug("Checking content of node with sipVersionNumber id at XPath " + PATH_TO_SIP_VERSION_NUMBER + ".");
        xmlBais.reset();
        Node sipVersionNode = XmlUtils.findSingleNodeWithXPath(xmlBais, PATH_TO_SIP_VERSION_NUMBER, null);
        Utils.eq(Integer.valueOf(sipVersionNode.getTextContent()), sipVersionNumber, () -> new InvalidXmlNodeValue(
                String.valueOf(sipVersionNumber), sipVersionNode.getTextContent(), PATH_TO_SIP_VERSION_NUMBER));

        log.debug("Checking content of node with sipVersionOf id at XPath " + PATH_TO_PHYSICAL_VERSION_OF + ".");
        xmlBais.reset();
        Node sipVersionOfNode = XmlUtils.findSingleNodeWithXPath(xmlBais, PATH_TO_PHYSICAL_VERSION_OF, null);
        Utils.eq(sipVersionOfNode.getTextContent(), sipVersionOf, () -> new InvalidXmlNodeValue(sipVersionOf,
                sipVersionOfNode.getTextContent(), PATH_TO_PHYSICAL_VERSION_OF));
        log.debug("Final validation of ArclibXml succeeded.");
    }

    private void validateNodesRequiredByCsv(String xml, boolean onlyXsltRelatedNodes) throws IOException, ParserConfigurationException, SAXException {
        log.debug("Checking existence of required nodes.");
        BufferedReader br = new BufferedReader(new InputStreamReader(arclibXmlDefinition.getInputStream(), StandardCharsets.UTF_8));
        Iterable<CSVRecord> records = CSVFormat.EXCEL.withDelimiter(',').withHeader().withSkipHeaderRecord(true).parse(br);

        ByteArrayInputStream xmlBais = new ByteArrayInputStream(xml.getBytes());
        for (CSVRecord record : records) {
            xmlBais.reset();
            if (record.get(4).equalsIgnoreCase("true") && (!onlyXsltRelatedNodes || record.get(3).startsWith("SIP profile XSLT"))) {
                String xPath = record.get(1);
                NodeList withXPath = XmlUtils.findWithXPath(new ByteArrayInputStream(xml.getBytes()), xPath, uris);
                Utils.ne(withXPath.getLength(), 0, () -> {
                    log.error(xml);
                    return new MissingNode(xPath);
                });
            }
        }
    }

    private void validateAgainstXsds(String xml) throws IOException {
        log.debug("Validating using XML schemas.");
        InputStream[] xsdSchemas = new InputStream[]{
                arclibXmlSchema.getInputStream(),
                metsSchema.getInputStream(),
                premisSchema.getInputStream()
        };
        XmlUtils.validateWithXMLSchema(xml, xsdSchemas, "Validation of resulting AIP XML");
    }

    @Inject
    public void setArclibXmlDefinition(@Value("${arclib.arclibXmlDefinition}")
                                               Resource arclibXmlDefinition) {
        this.arclibXmlDefinition = arclibXmlDefinition;
    }

    @Inject
    public void setArclibXmlSchema(@Value("${arclib.arclibXmlSchema}") Resource arclibXmlSchema) {
        this.arclibXmlSchema = arclibXmlSchema;
    }

    @Inject
    public void setMetsSchema(@Value("${arclib.metsSchema}") Resource metsSchema) {
        this.metsSchema = metsSchema;
    }

    @Inject
    public void setPremisSchema(@Value("${arclib.premisSchema}") Resource premisSchema) {
        this.premisSchema = premisSchema;
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
}
