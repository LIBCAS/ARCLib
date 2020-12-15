package cz.cas.lib.arclib.util;

import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.exception.validation.SchemaValidationError;
import cz.cas.lib.arclib.utils.XmlUtils;
import org.apache.commons.io.IOUtils;
import org.dom4j.InvalidXPathException;
import org.junit.Test;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static helper.ThrowableAssertion.assertThrown;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class XmlUtilsTest {
    private static final String SIP_ID = "KPW01169310";
    private static final String SIP_PATH = "src/test/resources/SIP_package/" + SIP_ID;
    private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\">" +
            "  <bpmn:process id=\"Process_1\" isExecutable=\"true\">" +
            "    <bpmn:startEvent id=\"StartEvent_1\">" +
            "      <bpmn:outgoing>SequenceFlow_0exqja9</bpmn:outgoing>" +
            "    </bpmn:startEvent>" +
            "    <bpmn:endEvent id=\"EndEvent_0xqnz0p\">" +
            "      <bpmn:incoming>SequenceFlow_0exqja9</bpmn:incoming>" +
            "    </bpmn:endEvent>" +
            "    <bpmn:sequenceFlow id=\"SequenceFlow_0exqja9\" sourceRef=\"StartEvent_1\" targetRef=\"EndEvent_0xqnz0p\"/>" +
            "  </bpmn:process>" +
            "</bpmn:definitions>";

    @Test
    public void findWithXPathTest() throws SAXException, ParserConfigurationException, IOException {
        NodeList withXPath = XmlUtils.findWithXPath(new FileInputStream(SIP_PATH + "/info.xml"),
                "/info/created", null);
        assertThat(withXPath.item(0).getTextContent(), is("2013-01-22T10:55:22"));
    }

    @Test
    public void findWithXPathInvalidXPathTest() {
        assertThrown(() -> XmlUtils.findWithXPath(new FileInputStream(SIP_PATH + "/info.xml"),
                "///", null)).isInstanceOf(InvalidXPathException.class);
    }

    @Test
    public void xPathCheckExistentNode() throws SAXException, ParserConfigurationException, IOException {
        String pathToValidationProfile = getClass().getResource("/validation/validationProfileMixedChecks.xml").getPath();
        NodeList nodeList = XmlUtils.findWithXPath(new FileInputStream(pathToValidationProfile), "/profile/rule", null);

        assertThat(nodeList.getLength(), is(4));

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node nNode = nodeList.item(i);
            assertThat(nNode.getNodeName(), is("rule"));
        }
    }

    @Test
    public void xPathCheckNonexistentNodeTest() throws SAXException, ParserConfigurationException, XPathExpressionException,
            IOException {
        String pathToValidationProfile = getClass().getResource("/validation/validationProfileMixedChecks.xml").getPath();
        NodeList nodeList = XmlUtils.findWithXPath(new FileInputStream(pathToValidationProfile),
                "/profile/nonExistentTag", null);

        assertThat(nodeList.getLength(), is(0));
    }

    @Test
    public void validationSchemaCheckSuccess() throws IOException, SAXException {
        String xsdPath = getClass().getResource("/xmlSchemas/validationProfile.xsd").getPath();

        String validationProfilePath = getClass().getResource("/validation/validationProfileMixedChecks.xml").getPath();

        XmlUtils.validateWithXMLSchema(IOUtils.toString(new FileInputStream(validationProfilePath)), new InputStream[]{new FileInputStream
                (xsdPath)}, "");
    }

    @Test
    public void validationSchemaCheckInvalidXml() {
        String xsdPath = getClass().getResource("/xmlSchemas/validationProfile.xsd").getPath();

        String validationProfilePath = getClass().getResource("/validation/validationProfileInvalidProfile.xml").getPath();

        assertThrown(() -> XmlUtils.validateWithXMLSchema(IOUtils.toString(new FileInputStream(validationProfilePath)),
                new InputStream[]{new FileInputStream(xsdPath)}, "")).isInstanceOf(GeneralException.class);
    }

    @Test
    public void validationSchemaCheckInvalidXsd() {
        String xsdPath = getClass().getResource("/validation/schemaInvalid.xsd").getPath();

        String validationProfilePath = getClass().getResource("/validation/validationProfileMixedChecks.xml").getPath();

        assertThrown(() -> XmlUtils.validateWithXMLSchema(IOUtils.toString(new FileInputStream(validationProfilePath)),
                new InputStream[]{new FileInputStream(xsdPath)}, "")).isInstanceOf(SchemaValidationError.class);
    }
}
