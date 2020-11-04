package cz.cas.lib.arclib.utils;

import cz.cas.lib.arclib.exception.validation.MissingNode;
import cz.cas.lib.arclib.exception.validation.MultipleNodesFound;
import cz.cas.lib.arclib.exception.validation.SchemaValidationError;
import cz.cas.lib.core.util.Utils;
import lombok.SneakyThrows;
import net.sf.saxon.jaxp.SaxonTransformerFactory;
import org.dom4j.InvalidXPathException;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;

public class XmlUtils {

    /**
     * Searches XML element with XPath and returns list of nodes found
     *
     * @param xml        input stream with the XML in which the element is being searched
     * @param expression XPath expression used in search
     * @return {@link NodeList} of elements matching the XPath in the XML
     * @throws IOException                  if the XML at the specified path is missing
     * @throws SAXException                 if the XML cannot be parsed
     */
    public static NodeList findWithXPath(InputStream xml, String expression)
            throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;

        dBuilder = dbFactory.newDocumentBuilder();

        Document doc = dBuilder.parse(xml);
        doc.getDocumentElement().normalize();

        XPath xPath = XPathFactory.newInstance().newXPath();

        try {
            return (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new InvalidXPathException(expression, e.getMessage());
        }
    }

    /**
     * Searches XML element with XPath and returns single node found or throws exception if there is no or multiple nodes
     *
     * @param xml        input stream with the XML in which the element is being searched
     * @param expression XPath expression used in search
     * @return {@link Node} singe node found
     * @throws IOException        if the XML at the specified path is missing
     * @throws SAXException       if the XML cannot be parsed
     * @throws MissingNode        if the node is not found
     * @throws MultipleNodesFound if multiple nodes were found
     */
    public static Node findSingleNodeWithXPath(InputStream xml, String expression)
            throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;

        dBuilder = dbFactory.newDocumentBuilder();

        Document doc = dBuilder.parse(xml);
        doc.getDocumentElement().normalize();

        XPath xPath = XPathFactory.newInstance().newXPath();

        try {
            NodeList elementsFound = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
            switch (elementsFound.getLength()) {
                case 0:
                    throw new MissingNode(expression);
                case 1:
                    return elementsFound.item(0);
                default:
                    throw new MultipleNodesFound(expression);
            }
        } catch (XPathExpressionException e) {
            throw new InvalidXPathException(expression, e.getMessage());
        }
    }

    /**
     * Rewrites values of all nodes that match xpath with specified value
     * XPATH is not namespace aware i.e. you can't specify a namespace
     *
     * @param xml       xml document
     * @param nodeXpath xpath to node
     * @param value     new value
     */
    public static String rewriteValues(String xml, String nodeXpath, String value) throws ParserConfigurationException,
            XPathExpressionException, TransformerException, IOException, SAXException {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        return rewriteValuesOfDoc(doc, nodeXpath, value);
    }

    public static StringBuilder extractTextFromAllElements(StringBuilder stringBuilder, Node node) {
        NodeList childNodes = node.getChildNodes();
        switch (node.getNodeType()) {
            case Node.TEXT_NODE:
            case Node.CDATA_SECTION_NODE:
                String textContent = node.getTextContent().trim();
                if (textContent.isEmpty())
                    return stringBuilder;
                return stringBuilder.append(textContent).append(" ");
        }
        if (childNodes != null) {
            for (int i = 0; i < childNodes.getLength(); i++) {
                extractTextFromAllElements(stringBuilder, childNodes.item(i));
            }
        }
        return stringBuilder;
    }

    /**
     * Rewrites values of all nodes that match xpath with specified value
     * XPATH is not namespace aware i.e. you can't specify a namespace
     *
     * @param xml       xml document
     * @param nodeXpath xpath to node
     * @param value     new value
     */
    public static String rewriteValues(InputStream xml, String nodeXpath, String value) throws
            ParserConfigurationException, IOException, SAXException, TransformerException, XPathExpressionException {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml);
        return rewriteValuesOfDoc(doc, nodeXpath, value);
    }

    private static String rewriteValuesOfDoc(Document doc, String nodeXpath, String value) throws TransformerException,
            XPathExpressionException {
        StringWriter stringWriter = new StringWriter();
        NodeList nodes = (NodeList) XPathFactory.newInstance().newXPath().compile(nodeXpath).evaluate(doc, XPathConstants.NODESET);
        for (int i = 0; i < nodes.getLength(); i++) {
            nodes.item(i).setNodeValue(value);
        }
        SaxonTransformerFactory.newInstance().newTransformer().transform(new DOMSource(doc), new StreamResult(stringWriter));
        return stringWriter.toString();
    }

    /**
     * Transforms {@link org.w3c.dom.Node} to {@link String}
     *
     * @param node node to convert
     * @return result string
     * @throws TransformerException error during the transformation
     */
    public static String nodeToString(org.w3c.dom.Node node) throws TransformerException {
        StringWriter sw = new StringWriter();

        Transformer t = SaxonTransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.transform(new DOMSource(node), new StreamResult(sw));

        return sw.toString();
    }

    /**
     * Validates XML against XSD schema
     *
     * @param xml     XML in which the element is being searched
     * @param schemas XSD schemas against which the XML is validated
     * @param logSpec brief description of the XML and/or context of the validation.. used in log msg
     * @throws SAXException if the XSD schema is invalid
     * @throws IOException  if the XML at the specified path is missing
     */
    public static void validateWithXMLSchema(String xml, InputStream[] schemas, String logSpec) {
        SchemaFactory factory =
                SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Source[] sources = new Source[schemas.length];
        for (int i = 0; i < schemas.length; i++) {
            sources[i] = new StreamSource(schemas[i]);
        }
        try {
            Schema schema = factory.newSchema(sources);
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
        } catch (IOException | SAXException e) {
            LoggerFactory.getLogger(XmlUtils.class).error("Error during schema validation of XML: " + xml);
            throw new SchemaValidationError(logSpec + " - XML schema validation failed. Details: " + e.toString() + ". More details (whole XML) can be found in application log.", e);
        }
    }

    /**
     * Checks if the node at the xPath exists in the XML. If the node does not exist, {@link MissingNode} exception is thrown.
     *
     * @param xml   xml
     * @param xPath xPath
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public static void checkNodeExists(InputStream xml, String xPath) throws IOException, SAXException, ParserConfigurationException {
        NodeList withXPath = XmlUtils.findWithXPath(xml, xPath);

        Utils.ne(withXPath.getLength(), 0, () -> new MissingNode(xPath));
    }

    public static String removeNamespacesFromXPathExpr(String xPathWithNamespaces) {
        return xPathWithNamespaces.replaceAll("[\\w]+:", "");
    }

    public static XPath nsUnawareXPath() {
        return XPathFactory.newInstance().newXPath();
    }

    @SneakyThrows
    public static Document nsUnawareDom(InputStream inputStream) {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
    }
}
