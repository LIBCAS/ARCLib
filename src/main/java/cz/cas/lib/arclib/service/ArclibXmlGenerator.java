package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.SipProfile;
import cz.cas.lib.arclib.store.SipProfileStore;
import cz.inqool.uas.exception.InvalidAttribute;
import cz.inqool.uas.exception.MissingObject;
import cz.inqool.uas.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.util.ByteArrayInputStream;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static cz.inqool.uas.util.Utils.*;

@Service
@Slf4j
public class ArclibXmlGenerator {

    private SipProfileStore store;
    private Map<String, String> uris = new HashMap<>();
    private Resource sipProfileSchema;

    private static final String ROOT = "mets";
    private static final String MAPPING_ELEMENTS_XPATH = "/profile/mapping";

    /**
     * Generates ARCLib XML from SIP using the SIP profile
     *
     * @param sipPath      path to the SIP package
     * @param sipProfileId id of the SIP profile
     * @return ARCLib XML
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     * @throws TransformerException
     */
    public String generateArclibXml(String sipPath, String sipProfileId)
            throws ParserConfigurationException, TransformerException, SAXException, XPathExpressionException, IOException {
        log.info("Generating ARCLib XML for SIP at path " + sipPath + " using SIP profile with ID " + sipProfileId + ".");

        SipProfile sipProfile = store.find(sipProfileId);
        notNull(sipProfile, () -> new MissingObject(SipProfile.class, sipProfileId));

        ArclibXmlValidator.validateWithXMLSchema(
                new ByteArrayInputStream(sipProfile.getXml().getBytes(StandardCharsets.UTF_8.name())),
                new InputStream[]{sipProfileSchema.getInputStream()});

        String sipProfileXml = sipProfile.getXml();
        notNull(sipProfileXml, () -> new InvalidAttribute(sipProfile, "xml", null));

        Document arclibXmlDoc = DocumentHelper.createDocument();
        arclibXmlDoc.addElement(new QName(ROOT, Namespace.get("METS", uris.get("METS"))));

        NodeList mappingNodes = XPathUtils.findWithXPath(stringToInputStream(sipProfileXml), MAPPING_ELEMENTS_XPATH);
        for (int i = 0; i < mappingNodes.getLength(); i++) {
            Set<Utils.Pair<String, String>> nodesToCreate = nodesToCreateByMapping((Element) mappingNodes.item(i), sipPath);

            XmlBuilder xmlBuilder = new XmlBuilder(uris);
            for (Utils.Pair<String, String> xPathToValue : nodesToCreate) {
                xmlBuilder.addNode(arclibXmlDoc, xPathToValue.getL(), xPathToValue.getR(), uris.get("ARCLIB"));
            }
        }

        String arclibXml = arclibXmlDoc.asXML().replace("&lt;", "<").replace("&gt;", ">");
        log.info("Generated ARCLib XLM: \n" + arclibXml);

        return arclibXml;
    }

    /**
     * Compute nodes to be created in ARCLib XML defined by a mapping.
     * 1. finds files in SIP matching regex from mapping
     * 2. in each file finds nodes matching xPath from mapping
     * 3. returns all these nodes
     *
     * @param mappingElement element with the mapping of nodes from the source SIP to ARCLib XML
     * @param sipPath        path to the SIP package
     * @return set of unique nodes to be created, a node is represented by a pair of xPath to ARCLib XML where it is to be
     * created and its respective value
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws TransformerException
     */
    private Set<Pair<String, String>> nodesToCreateByMapping(Element mappingElement, String sipPath)
            throws IOException, ParserConfigurationException, SAXException, TransformerException {
        Element sourceElement = (Element) mappingElement.getElementsByTagName("source").item(0);
        Element destinationElement = (Element) mappingElement.getElementsByTagName("destination").item(0);

        String sourceRootDirPath = sourceElement.getElementsByTagName("rootDirPath").item(0).getTextContent();
        String sourceFileRegex = sourceElement.getElementsByTagName("fileRegex").item(0).getTextContent();
        String sourceXPath = sourceElement.getElementsByTagName("xPath").item(0).getTextContent();

        String destXPath = destinationElement.getElementsByTagName("xPath").item(0).getTextContent();

        //if the part of the XPath after the last slash does not specify an attribute, trim this part,
        //so that the new element was created at the parent element of the target element
        if (!destXPath.contains("@")) {
            destXPath = XPathUtils.getParentXPath(destXPath);
        }

        Set<Pair<String, String>> destinationXPathsToValues = new HashSet<>();

        File[] files = listFilesMatchingRegex(new File(sipPath + sourceRootDirPath), sourceFileRegex);
        for (int i = 0; i < files.length; i++) {
            NodeList valueNodes = XPathUtils.findWithXPath(new FileInputStream(files[i]), sourceXPath);

            for (int j = 0; j < valueNodes.getLength(); j++) {
                String nodeValue = nodeToString(valueNodes.item(j));
                destinationXPathsToValues.add(new Pair(destXPath, nodeValue));
            }
        }
        return destinationXPathsToValues;
    }

    @Inject
    public void setStore(SipProfileStore store) {
        this.store = store;
    }

    @Inject
    public void setUris(@Value("${namespaces.METS}") String mets, @Value("${namespaces.ARCLIB}") String arclib, @Value("${namespaces" +
            ".PREMIS}") String premis) {
        Map<String, String> uris = new HashMap<>();
        uris.put("METS", mets);
        uris.put("ARCLIB", arclib);
        uris.put("PREMIS", premis);

        this.uris = uris;
    }

    @Inject
    public void setSipProfileSchema(@Value("${arclib.sipProfileSchema}") Resource sipProfileSchema) {
        this.sipProfileSchema = sipProfileSchema;
    }
}
