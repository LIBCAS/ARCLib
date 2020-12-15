package cz.cas.lib.arclib.utils;

import cz.cas.lib.arclib.exception.validation.MissingNode;
import cz.cas.lib.arclib.exception.validation.MultipleNodesFound;
import cz.cas.lib.arclib.exception.validation.SchemaValidationError;
import net.sf.saxon.jaxp.SaxonTransformerFactory;
import net.sf.saxon.s9api.UnprefixedElementMatchingPolicy;
import net.sf.saxon.xpath.XPathEvaluator;
import org.apache.commons.lang3.tuple.Pair;
import org.dom4j.InvalidXPathException;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map;

import static cz.cas.lib.core.util.Utils.notNull;

public class XmlUtils {

    /**
     * Searches XML element with XPath 3.1 and returns list of nodes found.
     *
     * @param xml        input stream with the XML in which the element is being searched
     * @param expression XPath expression used in search
     * @param uriMap     map of prefix->URI if the document and xpath should be in namespace-aware context, null otherwise
     * @return {@link NodeList} of elements matching the XPath in the XML
     * @throws IOException  if the XML at the specified path is missing
     * @throws SAXException if the XML cannot be parsed
     */
    public static NodeList findWithXPath(InputStream xml, String expression, Map<String, String> uriMap)
            throws IOException, SAXException, ParserConfigurationException {
        Pair<Document, XPath> domAndXpath = createDomAndXpath(xml, uriMap);
        Document doc = domAndXpath.getKey();
        XPath xPath = domAndXpath.getValue();

        doc.getDocumentElement().normalize();

        try {
            return (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new InvalidXPathException(expression, e.getMessage());
        }
    }

    /**
     * Searches XML element with XPath 3.1 and returns single node found or throws exception if there is no or multiple nodes.
     *
     * @param xml        input stream with the XML in which the element is being searched
     * @param expression XPath expression used in search
     * @param uriMap     map of prefix->URI if the document and xpath should be in namespace-aware context, null otherwise
     * @return {@link Node} singe node found
     * @throws IOException        if the XML at the specified path is missing
     * @throws SAXException       if the XML cannot be parsed
     * @throws MissingNode        if the node is not found
     * @throws MultipleNodesFound if multiple nodes were found
     */
    public static Node findSingleNodeWithXPath(InputStream xml, String expression, Map<String, String> uriMap)
            throws IOException, SAXException, ParserConfigurationException {
        Pair<Document, XPath> domAndXpath = createDomAndXpath(xml, uriMap);
        Document doc = domAndXpath.getKey();
        XPath xPath = domAndXpath.getValue();

        doc.getDocumentElement().normalize();

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

    public static StringBuilder extractTextFromAllElements(StringBuilder stringBuilder, Node node) {
        NodeList childNodes = node.getChildNodes();
        if (node.getNodeType() == Node.TEXT_NODE) {
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
     * Returns XML parsed as DOM together with configured {@link XPath} 3.1 evaluator to be used with the DOM.
     * Saxon library is used for xPath evaluation.
     * <p>
     * If no namespace map is provided, then both, the DOM and xPath are set to ignore namespace. Otherwise the parsed DOM
     * is namespace aware and the xPath is aware of the namespaces passed in the map.
     * </p>
     *
     * @param xmlInput                      XML input
     * @param namespacePrefixToNamespaceUri map of prefix->URI if the document and xpath should be in namespace-aware context, null otherwise
     * @return
     */
    public static Pair<Document, XPath> createDomAndXpath(InputStream xmlInput, Map<String, String> namespacePrefixToNamespaceUri) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(namespacePrefixToNamespaceUri != null);
        Document dom = documentBuilderFactory.newDocumentBuilder().parse(xmlInput);

        XPath xPath = new net.sf.saxon.xpath.XPathFactoryImpl().newXPath();
        if (namespacePrefixToNamespaceUri == null)
            ((XPathEvaluator) xPath).getStaticContext().setUnprefixedElementMatchingPolicy(UnprefixedElementMatchingPolicy.ANY_NAMESPACE);
        else
            xPath.setNamespaceContext(new NamespaceContext() {
                public String getNamespaceURI(String prefix) {
                    notNull(prefix, () -> new IllegalArgumentException("No prefix provided!"));
                    String uri = namespacePrefixToNamespaceUri.get(prefix.toLowerCase());
                    return uri == null ? XMLConstants.NULL_NS_URI : uri;
                }

                public String getPrefix(String namespaceURI) {
                    // Not needed in this context.
                    return null;
                }

                public Iterator getPrefixes(String namespaceURI) {
                    // Not needed in this context.
                    return null;
                }
            });
        return Pair.of(dom, xPath);
    }
}
