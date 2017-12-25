package cz.cas.lib.arclib.service;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.InvalidXPathException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;

public class XPathUtils {

    private static final String SLASH = "/";
    private static final String R_BRACKET = "]";
    private static final String L_BRACKET = "[";

    /**
     * Searches XML element with XPath and returns list of nodes found
     *
     * @param xml        input stream with the XML in which the element is being searched
     * @param expression XPath expression used in search
     * @return {@link NodeList} of elements matching the XPath in the XML
     * @throws XPathExpressionException     if there is an error in the XPath expression
     * @throws IOException                  if the XML at the specified path is missing
     * @throws SAXException                 if the XML cannot be parsed
     * @throws ParserConfigurationException
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
     * Looks for the last '/' and returns the name of the last element
     *
     * @param xpath
     * @return the child element name or null
     */
    public static final String getChildElementName(String xpath) {
        if (StringUtils.isEmpty(xpath)) {
            return null;
        }
        String childName = xpath.substring(xpath.lastIndexOf(SLASH) + 1);
        return stripIndex(childName);
    }

    /**
     * Returns the XPath for the parent node from the XPath
     * i.e. /root/suspension_rec returns /root
     *
     * @param xpath
     * @return XPath to the parent node
     */
    public static final String getParentXPath(String xpath) {
        if (StringUtils.isEmpty(xpath) || xpath.lastIndexOf(SLASH) < 0) {
            return null;
        }

        if (xpath.lastIndexOf(SLASH) == 0) {
            return "/";
        }

        return xpath.substring(0, xpath.lastIndexOf(SLASH));
    }

    /**
     * Returns the index of the child element xpath
     * i.e. /suspension_rec[3] returns 3.  /suspension_rec defaults to 1
     *
     * @param xpath
     * @return 1, the index, or null if the provided xpath is empty
     */
    public static Integer getChildElementIndex(String xpath) {
        if (StringUtils.isEmpty(xpath)) {
            return null;
        }

        if (xpath.endsWith(R_BRACKET)) {
            String value = xpath.substring(xpath.lastIndexOf(L_BRACKET) + 1, xpath.lastIndexOf(R_BRACKET));
            if (StringUtils.isNumeric(value)) {
                return Integer.valueOf(value);
            }
        }
        return 1;
    }

    /**
     * Creates position XPath from XPath and child index
     *
     * @param xpath
     * @param childIndex
     * @return xpath appended with the index of the child
     */
    public static String createPositionXpath(String xpath, Integer childIndex) {
        if (StringUtils.isEmpty(xpath)) {
            return null;
        }
        return stripIndex(xpath) + "[position()<" + childIndex + "]";
    }

    /**
     * Strips the index with the position of child element from the XPath
     *
     * @param xPath
     * @return xpath stripped from the child element position
     */
    private static String stripIndex(String xPath) {
        if (xPath.endsWith(R_BRACKET)) {
            return xPath.substring(0, xPath.lastIndexOf(L_BRACKET));
        } else {
            return xPath;
        }
    }
}
