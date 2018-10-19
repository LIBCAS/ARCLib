package cz.cas.lib.arclib.service.arclibxml;

import com.google.common.io.Resources;
import cz.cas.lib.arclib.exception.validation.MissingNode;
import cz.cas.lib.core.exception.GeneralException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;

import static helper.ThrowableAssertion.assertThrown;

public class ArclibXmlValidatorTest {

    private static String VALIDATION_CHECKS = "arclibXmlValidationChecks.txt";

    private static String ARCLIB_XML = "sampleData/arclibXml4.xml";

    private static String INVALID_ARCLIB_XML_MISSING_METS_HDR =
            "arclibXmls/invalidArclibXmlMissingMetsHdr.xml";
    private static String INVALID_ARCLIB_XML_INVALID_TAG = "arclibXmls/invalidArclibXmlInvalidTag.xml";

    private static String ARCLIB_SCHEMA = "xmlSchemas/arclibXml.xsd";
    private static String METS_SCHEMA = "xmlSchemas/mets.xsd";
    private static String PREMIS_SCHEMA = "xmlSchemas/premis-v2-2.xsd";
    private static Integer SIP_VERSION_NUMBER = 2;
    private static String SIP_VERSION_OF = "4b66655a-819a-474f-8203-6c432815df1f";
    private static String AUTHORIAL_ID = "authorialId3";
    private static String SIP_ID = "8b2efafd-b637-4b97-a8f7-1b97dd4ee622";

    private static String PATH_TO_AIP_ID = "/mets/@OBJID";
    private static String PATH_TO_AUTHORIAL_ID = "/mets/metsHdr/altRecordID[@TYPE='original SIP identifier']";
    private static String PATH_TO_SIP_VERSION_NUMBER = "/mets/dmdSec/mdWrap/xmlData/sipVersionNumber";
    private static String PATH_TO_SIP_VERSION_OF = "/mets/dmdSec/mdWrap/xmlData/sipVersionOf";

    private ArclibXmlValidator validator;

    @Before
    public void setUp() {
        validator = new ArclibXmlValidator();

        validator.setArclibXmlValidationChecks(new ClassPathResource(VALIDATION_CHECKS));
        validator.setArclibXmlSchema(new ClassPathResource(ARCLIB_SCHEMA));
        validator.setMetsSchema(new ClassPathResource(METS_SCHEMA));
        validator.setPremisSchema(new ClassPathResource(PREMIS_SCHEMA));

        validator.setPathToAipId(PATH_TO_AIP_ID);
        validator.setPathToAuthorialId(PATH_TO_AUTHORIAL_ID);
        validator.setPathToSipVersionNumber(PATH_TO_SIP_VERSION_NUMBER);
        validator.setPathToPhysicalVersionOf(PATH_TO_SIP_VERSION_OF);
    }

    @Test
    public void validateArclibXmlSuccess() throws IOException, SAXException, ParserConfigurationException {
        URL arclibXml = Resources.getResource(ARCLIB_XML);
        validator.validateArclibXml(new ByteArrayInputStream(Resources.toByteArray(arclibXml)), SIP_ID, AUTHORIAL_ID,
                SIP_VERSION_NUMBER, SIP_VERSION_OF);
    }

    /**
     * Tests that the {@link MissingNode} exception is thrown when the ARCLib XML is missing the element <i>metsHdr</i>.
     */
    @Test
    public void validateArclibXmlWithValidatorMissingNode() {
        URL arclibXml = Resources.getResource(INVALID_ARCLIB_XML_MISSING_METS_HDR);
        assertThrown(() -> validator.validateArclibXml(new ByteArrayInputStream(Resources.toByteArray(arclibXml)),
                SIP_ID, AUTHORIAL_ID, SIP_VERSION_NUMBER, SIP_VERSION_OF)).isInstanceOf
                (MissingNode.class);
    }

    /**
     * Tests that the {@link MissingNode} exception is thrown when the ARCLib XML contains an invalid tag
     */
    @Test
    public void validateArclibXmlWithValidatorInvalidTag() {
        URL arclibXml = Resources.getResource(INVALID_ARCLIB_XML_INVALID_TAG);
        assertThrown(() -> validator.validateArclibXml(new ByteArrayInputStream(Resources.toByteArray(arclibXml)),
                SIP_ID, AUTHORIAL_ID, SIP_VERSION_NUMBER, SIP_VERSION_OF)).isInstanceOf
                (GeneralException.class);
    }
}
