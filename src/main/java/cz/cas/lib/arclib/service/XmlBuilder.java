package cz.cas.lib.arclib.service;

import cz.inqool.uas.exception.GeneralException;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.XPath;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static cz.inqool.uas.util.Utils.nodeToString;


@Slf4j
public class XmlBuilder {
    private Map<String, String> uris;

    /**
     * Constructor for XmlBuilder
     *
     * @param uris map of namespaces for the generated XML, format: ["namespaceIdentifier", "namespaceValue"]
     */
    public XmlBuilder(Map uris) {
        this.uris = uris;
    }

    /**
     * Recursive method to create a node and, if necessary, its parents and siblings
     *
     * @param doc         document
     * @param targetXPath to single node
     * @param value       if null an empty node will be created
     * @return the created Node
     */

    public Node addNode(Document doc, String targetXPath, String value, String namespaceUri) throws TransformerException {
        log.info("adding Node: " + targetXPath + " -> " + value);

        String elementName = XPathUtils.getChildElementName(targetXPath);
        String parentXPath = XPathUtils.getParentXPath(targetXPath);

        //add value as text to the root element and return
        if (("/").equals(parentXPath)) {
            Element rootElement = doc.getRootElement();
            //the root on the xpath is different from the root of the document
//            if (!rootElement.getName().equals(elementName)) throw new BadArgument(targetXPath);

            if (value != null) {
                rootElement.addText(value);
            }
            return rootElement;
        }

        if (parentXPath == null) {
            Element rootElement = doc.getRootElement();

            org.w3c.dom.Node firstChild;
            try {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                org.w3c.dom.Document valueDoc = dBuilder.parse(new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8.name())));
                firstChild = valueDoc.getFirstChild();
            } catch (Exception e) {
                throw new GeneralException(e);
            }

            NodeList childNodes = firstChild.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                rootElement.addText(nodeToString(childNodes.item(i)));
            }

            NamedNodeMap attributes = firstChild.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                org.w3c.dom.Node item = attributes.item(i);
                rootElement.addAttribute(item.getNodeName(), item.getTextContent());
            }
            return rootElement;
        }

        XPath xpath = doc.createXPath(parentXPath);
        xpath.setNamespaceURIs(uris);

        Node parentNode = xpath.selectSingleNode(doc);
        if (parentNode == null) {
            parentNode = addNode(doc, parentXPath, null, namespaceUri);
        }

        //add value as attribute to the parent node and return
        if (elementName.startsWith("@")) {
            return ((Element) parentNode).addAttribute(elementName.substring(1), value);
        }

        // create younger siblings if needed
        Integer childIndex = XPathUtils.getChildElementIndex(targetXPath);
        if (childIndex > 1) {
            List<?> nodelist = doc.selectNodes(XPathUtils.createPositionXpath(targetXPath, childIndex));
            // how many to create = (index wanted - existing - 1 to account for the new element we will create)
            int nodesToCreate = childIndex - nodelist.size() - 1;
            for (int i = 0; i < nodesToCreate; i++) {
                ((Element) parentNode).addElement(elementName);
            }
        }

        //add new element to the parent node
        Element created = ((Element) parentNode).addElement(elementName, uris.get("ARCLIB"));
        if (null != value) {
            created.addText(value);
        }

        return created;
    }
}
