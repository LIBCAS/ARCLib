package cz.cas.lib.arclib.utils;

import cz.cas.lib.arclib.exception.validation.MissingNode;
import cz.cas.lib.core.exception.GeneralException;
import cz.cas.lib.core.util.Utils;
import org.dom4j.InvalidXPathException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

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
            throw new InvalidXPathException(expression, e);
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
        TransformerFactory.newInstance().newTransformer().transform(new DOMSource(doc), new StreamResult(stringWriter));
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

        Transformer t = TransformerFactory.newInstance().newTransformer();
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
     * @throws SAXException if the XSD schema is invalid
     * @throws IOException  if the XML at the specified path is missing
     */
    public static void validateWithXMLSchema(InputStream xml, InputStream[] schemas) throws IOException, SAXException {
        SchemaFactory factory =
                SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        Source[] sources = new Source[schemas.length];
        for (int i = 0; i < schemas.length; i++) {
            sources[i] = new StreamSource(schemas[i]);
        }

        Schema schema = factory.newSchema(sources);
        Validator validator = schema.newValidator();

        try {
            validator.validate(new StreamSource(xml));
        } catch (SAXException e) {
            throw new GeneralException(e);
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
}
