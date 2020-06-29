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
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class ArclibXmlValidator {

    private Resource arclibXmlDefinition;
    private Resource arclibXmlSchema;
    private Resource metsSchema;
    private Resource premisSchema;

    private String pathToAipId;
    private String pathToAuthorialId;
    private String pathToSipVersionNumber;
    private String pathToPhysicalVersionOf;

    /**
     * Validates the structure of ARCLib XML.
     * 1. Validator checks presence of the given nodes in ARCLib XML according to the XPaths specified
     * in the compulsory elements of ARCLib XML in the file <i>index/arclibXmlDefinition.csv</i>.
     * 2. ARCLib XML is validated against XML schemas: <i>arclibXmlSchema, metsSchema, premisSchema</i>
     * 3.
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
    public void validateArclibXml(ByteArrayInputStream xml, String aipId, String authorialId, Integer sipVersionNumber,
                                  String sipVersionOf) throws IOException, SAXException, ParserConfigurationException {
        log.debug("Starting validation of ARCLib XML.");

        log.debug("Validating using XML schemas.");
        xml.reset();
        InputStream[] xsdSchemas = new InputStream[]{
                arclibXmlSchema.getInputStream(),
                metsSchema.getInputStream(),
                premisSchema.getInputStream()};
        XmlUtils.validateWithXMLSchema(xml, xsdSchemas);

        log.debug("Checking existence of required nodes.");
        BufferedReader br = new BufferedReader(new InputStreamReader(arclibXmlDefinition.getInputStream(), StandardCharsets.UTF_8));
        Iterable<CSVRecord> records = CSVFormat.EXCEL.withDelimiter(',').withHeader().withSkipHeaderRecord(true).parse(br);

        for (CSVRecord record : records) {
            xml.reset();
            if (record.get(4).equalsIgnoreCase("true")) {
                String xPath = record.get(1);
                XmlUtils.checkNodeExists(xml, XmlUtils.removeNamespacesFromXPathExpr(xPath));
            }
        }

        log.debug("Checking content of node with AIP id at XPath " + pathToAipId + ".");
        xml.reset();
        NodeList list1 = XmlUtils.findWithXPath(xml, pathToAipId);
        Utils.ne(list1.getLength(), 0, () -> new MissingNode(pathToAipId));
        Utils.eq(list1.item(0).getTextContent(), aipId, () -> new InvalidXmlNodeValue(aipId,
                list1.item(0).getTextContent(), pathToAipId));

        log.debug("Checking content of node with authorial id at XPath " + pathToAuthorialId + ".");
        xml.reset();
        NodeList list2 = XmlUtils.findWithXPath(xml, pathToAuthorialId);
        Utils.ne(list2.getLength(), 0, () -> new MissingNode(pathToAuthorialId));
        Utils.eq(list2.item(0).getTextContent(), authorialId, () -> new InvalidXmlNodeValue(authorialId,
                list2.item(0).getTextContent(), pathToAuthorialId));

        log.debug("Checking content of node with sipVersionNumber id at XPath " + pathToSipVersionNumber + ".");
        xml.reset();
        NodeList list3 = XmlUtils.findWithXPath(xml, pathToSipVersionNumber);
        Utils.ne(list3.getLength(), 0, () -> new MissingNode(pathToSipVersionNumber));
        Utils.eq(Integer.valueOf(list3.item(0).getTextContent()), sipVersionNumber, () -> new InvalidXmlNodeValue(
                String.valueOf(sipVersionNumber), list3.item(0).getTextContent(), pathToSipVersionNumber));

        log.debug("Checking content of node with sipVersionOf id at XPath " + pathToPhysicalVersionOf + ".");
        xml.reset();
        NodeList list4 = XmlUtils.findWithXPath(xml, pathToPhysicalVersionOf);
        Utils.ne(list4.getLength(), 0, () -> new MissingNode(pathToPhysicalVersionOf));
        Utils.eq(list4.item(0).getTextContent(), sipVersionOf, () -> new InvalidXmlNodeValue(sipVersionOf,
                list4.item(0).getTextContent(), pathToPhysicalVersionOf));

        log.debug("Validation of ArclibXml succeeded.");
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
    public void setPathToAipId(@Value("${arclib.arclibXmlValidator.pathToAipId}") String pathToAipId) {
        this.pathToAipId = pathToAipId;
    }

    @Inject
    public void setPathToAuthorialId(@Value("${arclib.arclibXmlValidator.pathToAuthorialId}")
                                             String pathToAuthorialId) {
        this.pathToAuthorialId = pathToAuthorialId;
    }

    @Inject
    public void setPathToSipVersionNumber(@Value("${arclib.arclibXmlValidator.pathToSipVersionNumber}")
                                                  String pathToSipVersionNumber) {
        this.pathToSipVersionNumber = pathToSipVersionNumber;
    }

    @Inject
    public void setPathToPhysicalVersionOf(@Value("${arclib.arclibXmlValidator.pathToPhysicalVersionOf}")
                                                   String pathToPhysicalVersionOf) {
        this.pathToPhysicalVersionOf = pathToPhysicalVersionOf;
    }
}
