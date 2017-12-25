package cz.cas.lib.arclib.arclibxmlgeneration;

import cz.cas.lib.arclib.service.XPathUtils;
import org.dom4j.InvalidXPathException;
import org.junit.Test;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.FileInputStream;
import java.io.IOException;

import static helper.ThrowableAssertion.assertThrown;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class XPathUtilsTest {
    private static final String SIP_ID = "KPW01169310";
    private static final String SIP_PATH = "SIP_packages/" + SIP_ID;

    @Test
    public void findWithXPathTest() throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        NodeList withXPath = XPathUtils.findWithXPath(new FileInputStream(SIP_PATH + "/info.xml"),
                "/info/created");
        assertThat(withXPath.item(0).getTextContent(), is("2013-01-22T10:55:22"));
    }

    @Test
    public void findWithXPathInvalidXPathTest() throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        assertThrown(() -> XPathUtils.findWithXPath(new FileInputStream(SIP_PATH + "/info.xml"),
                "///")).isInstanceOf(InvalidXPathException.class);
    }

    @Test
    public void testGetChildElementName() throws Exception {
        String childElement = XPathUtils.getChildElementName("/root/child[3]");
        assertThat(childElement, is("child"));
        assertThat(XPathUtils.getChildElementName(""), is(nullValue()));
        assertThat(XPathUtils.getChildElementName(null), is(nullValue()));
    }

    @Test
    public void testGetParentXPath() throws Exception {
        String parentPath = XPathUtils.getParentXPath("/root[1]/child");
        assertThat(parentPath, is("/root[1]"));
        assertThat(XPathUtils.getParentXPath("/root"), is("/"));
        assertThat(XPathUtils.getParentXPath(""), is(nullValue()));
        assertThat(XPathUtils.getParentXPath(null), is(nullValue()));
    }

    @Test
    public void testGetChildElementIndex() throws Exception {
        Integer childIndex = XPathUtils.getChildElementIndex("/root/child[3]");
        assertThat(childIndex, is(new Integer(3)));
        childIndex = XPathUtils.getChildElementIndex("/root/child");
        assertThat(childIndex, is(new Integer(1)));
        assertThat(XPathUtils.getChildElementIndex(""), is(nullValue()));
        assertThat(XPathUtils.getChildElementIndex(null), is(nullValue()));
    }

    @Test
    public void testCreatePositionXpath() throws Exception {
        String positionXPath = XPathUtils.createPositionXpath("/root/child", 5);
        assertThat(positionXPath, is("/root/child[position()<5]"));

        positionXPath = XPathUtils.createPositionXpath("/root/anotherChild[6]", 6);
        assertThat(positionXPath, is("/root/anotherChild[position()<6]"));
    }
}
