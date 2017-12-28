package cz.cas.lib.arclib.arclibxmlgeneration;

import com.google.common.io.Resources;
import cz.cas.lib.arclib.exception.validation.MissingNode;
import cz.cas.lib.arclib.service.ArclibXmlValidator;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;

import static helper.ThrowableAssertion.assertThrown;

public class ArclibXmlValidatorTest {

    private static String VALIDATION_CHECKS = "arclibXmlValidationChecks.txt";

    private static String ARCLIB_XML = "arclibxmlgeneration/arclibXmls/arclibXml.xml";
    private static String INVALID_ARCLIB_XML_MISSING_METS_HDR =
            "arclibxmlgeneration/arclibXmls/invalidArclibXmlMissingMetsHdr.xml";
    private static String INVALID_ARCLIB_XML_INVALID_TAG = "            arclibxmlgeneration/arclibXmls/invalidArclibXmlInvalidTag.xml";


    private static String ARCLIB_SCHEMA = "xmlSchemas/arclibXml.xsd";
    private static String METS_SCHEMA = "xmlSchemas/mets.xsd";
    private static String PREMIS_SCHEMA = "xmlSchemas/premis-v2-2.xsd";

    private ArclibXmlValidator validator;

    @Before
    public void setUp() {
        validator = new ArclibXmlValidator();

        validator.setArclibXmlValidationChecks(new ClassPathResource(VALIDATION_CHECKS));
        validator.setArclibXmlSchema(new ClassPathResource(ARCLIB_SCHEMA));
        validator.setMetsSchema(new ClassPathResource(METS_SCHEMA));
        validator.setPremisSchema(new ClassPathResource(PREMIS_SCHEMA));
    }

    @Test
    public void validateArclibXmlSuccess() throws IOException, XPathExpressionException, SAXException, ParserConfigurationException {
        URL arclibXml = Resources.getResource(ARCLIB_XML);
        validator.validateArclibXml(new ByteArrayInputStream(Resources.toByteArray(arclibXml)));
    }

    /**
     * Tests that the {@link MissingNode} exception is thrown when the ARCLib XML is missing the element <i>metsHdr</i>.
     */
    @Test
    public void validateArclibXmlWithValidatorMissingNode() {
        URL arclibXml = Resources.getResource(INVALID_ARCLIB_XML_MISSING_METS_HDR);
        assertThrown(() -> validator.validateArclibXml(new ByteArrayInputStream(Resources.toByteArray(arclibXml)))).isInstanceOf
                (MissingNode.class);
    }

    /**
     * Tests that the {@link MissingNode} exception is thrown when the ARCLib XML contains an invalid tag
     */
    @Test
    public void validateArclibXmlWithValidatorInvalidTag() {
        URL arclibXml = Resources.getResource(INVALID_ARCLIB_XML_INVALID_TAG);
        assertThrown(() -> validator.validateArclibXml(new ByteArrayInputStream(Resources.toByteArray(arclibXml)))).isInstanceOf
                (SAXException.class);
    }
}
