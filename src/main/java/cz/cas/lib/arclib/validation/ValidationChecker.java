package cz.cas.lib.arclib.validation;

import cz.inqool.uas.exception.GeneralException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class ValidationChecker {

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
     * Searches XML element with XPath and returns list of nodes found
     *
     * @param xml        XML in which the element is being searched
     * @param expression XPath expression used in search
     * @return {@link NodeList} of elements matching the XPath in the XML
     * @throws XPathExpressionException     if there is an error in the XPath expression
     * @throws IOException                  if the XML at the specified path is missing
     * @throws SAXException                 if the XML cannot be parsed
     * @throws ParserConfigurationException
     */
    public static NodeList findWithXPath(InputStream xml, String expression)
            throws XPathExpressionException, IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;

        dBuilder = dbFactory.newDocumentBuilder();

        Document doc = dBuilder.parse(xml);
        doc.getDocumentElement().normalize();

        XPath xPath = XPathFactory.newInstance().newXPath();

        return (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
    }

    /**
     * Checks whether the file at the path exists
     *
     * @param path path to the file
     * @return true if the file exists, false otherwise
     */
    public static boolean fileExists(String path) {
        File file = new File(path);
        return file.exists();
    }
}
