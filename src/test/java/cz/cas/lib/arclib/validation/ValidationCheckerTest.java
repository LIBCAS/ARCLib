package cz.cas.lib.arclib.validation;

import cz.inqool.uas.exception.GeneralException;
import helper.ThrowableAssertion;
import org.junit.Test;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ValidationCheckerTest {

    private static final String SIP_ID = "KPW01169310";
    private static final String SIP_PATH = "SIP_packages/" + SIP_ID;

    @Test
    public void validationSchemaCheckSuccess() throws IOException, SAXException {
        String xsdPath = getClass().getResource("/validation/validationProfileSchema.xsd").getPath();

        String validationProfilePath = getClass().getResource("/validation/validationProfileMixedChecks.xml").getPath();

        ValidationChecker.validateWithXMLSchema(new FileInputStream(validationProfilePath), new InputStream[]{new FileInputStream
                (xsdPath)});
    }

    @Test
    public void validationSchemaCheckInvalidXml() throws IOException, SAXException {
        String xsdPath = getClass().getResource("/validation/validationProfileSchema.xsd").getPath();

        String validationProfilePath = getClass().getResource("/validation/validationProfileInvalidProfile.xml").getPath();

        ThrowableAssertion.assertThrown(() -> ValidationChecker.validateWithXMLSchema(new FileInputStream(validationProfilePath),
                new InputStream[]{new FileInputStream(xsdPath)})).isInstanceOf(GeneralException.class);
    }

    @Test
    public void validationSchemaCheckInvalidXsd() throws IOException, SAXException {
        String xsdPath = getClass().getResource("/validation/schemaInvalid.xsd").getPath();

        String validationProfilePath = getClass().getResource("/validation/validationProfileMixedChecks.xml").getPath();

        ThrowableAssertion.assertThrown(() -> ValidationChecker.validateWithXMLSchema(new FileInputStream(validationProfilePath),
                new InputStream[]{new FileInputStream(xsdPath)})).isInstanceOf(SAXException.class);
    }

    @Test
    public void xPathCheckExistentNode() throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        String pathToValidationProfile = getClass().getResource("/validation/validationProfileMixedChecks.xml").getPath();
        NodeList nodeList = ValidationChecker.findWithXPath(new FileInputStream(pathToValidationProfile),"/profile/rule");

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
        NodeList nodeList = ValidationChecker.findWithXPath(new FileInputStream(pathToValidationProfile),
                "/profile/nonExistentTag");

        assertThat(nodeList.getLength(), is(0));
    }

    @Test
    public void filePresenceCheckExistentFileTest() {
        boolean success = ValidationChecker.fileExists(SIP_PATH + "/METS_KPW01169310.xml");
        assertThat(success, is(true));
    }

    @Test
    public void filePresenceCheckNonExistentFileTest() {
        boolean success = ValidationChecker.fileExists("/nonExistentPath");
        assertThat(success, is(false));
    }
}
