package cz.cas.lib.arclib.service.arclibxml;

import com.google.common.io.Resources;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.exception.validation.MissingNode;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import static helper.ThrowableAssertion.assertThrown;

public class ArclibXmlValidatorTest {

    private static String ARCLIB_XML_DEFINITION = "index/arclibXmlDefinition.csv";

    private static String ARCLIB_XML = "arclibXmls/arclibXml.xml";

    private static String INVALID_ARCLIB_XML_MISSING_METS_HDR =
            "arclibXmls/invalidArclibXmlMissingMetsHdr.xml";
    private static String INVALID_ARCLIB_XML_INVALID_TAG = "arclibXmls/invalidArclibXmlInvalidTag.xml";
    private static String INVALID_ARCLIB_XML_NOT_METS =
            "arclibXmls/invalidArclibXmlNotMets.xml";
    private static String ARCLIB_SCHEMA = "xmlSchemas/arclibXml.xsd";
    private static Integer SIP_VERSION_NUMBER = 2;
    private static String SIP_VERSION_OF = "4b66655a-819a-474f-8203-6c432815df1f";
    private static String AUTHORIAL_ID = "Hlasy ze Siona, 1861-1916";
    private static String SIP_ID = "8b2efafd-b637-4b97-a8f7-1b97dd4ee622";

    private static String PATH_TO_AIP_ID = "/mets/@OBJID";
    private static String PATH_TO_AUTHORIAL_ID = "/mets/metsHdr/altRecordID[@TYPE='original SIP identifier']";
    private static String PATH_TO_SIP_VERSION_NUMBER = "/mets/amdSec/digiprovMD[@ID='ARCLIB_SIP_INFO']/mdWrap/xmlData/sipInfo/sipVersionNumber";
    private static String PATH_TO_SIP_VERSION_OF = "/mets/amdSec/digiprovMD[@ID='ARCLIB_SIP_INFO']/mdWrap/xmlData/sipInfo/sipVersionOf";

    private ArclibXmlValidator validator;

    @Before
    public void setUp() throws IOException {
        validator = new ArclibXmlValidator();

        validator.setArclibXmlDefinition(new ClassPathResource(ARCLIB_XML_DEFINITION));

        validator.setArclibXmlSchema(new ClassPathResource(ARCLIB_SCHEMA));
        String metsSchema = IOUtils.toString(new FileInputStream(Paths.get("src/main/resources/xmlSchemas/mets.xsd").toFile())).replace("classpath:/xmlSchemas/xlink.xsd", "./src/main/resources/xmlSchemas/xlink.xsd");
        String premisSchema = IOUtils.toString(new FileInputStream(Paths.get("src/main/resources/xmlSchemas/premis-v2-2.xsd").toFile())).replace("classpath:/xmlSchemas/xlink.xsd", "./src/main/resources/xmlSchemas/xlink.xsd");

        validator.setMetsSchema(new ByteArrayResource(metsSchema.getBytes()));
        validator.setPremisSchema(new ByteArrayResource(premisSchema.getBytes()));

        validator.setPathToAipId(PATH_TO_AIP_ID);
        validator.setPathToAuthorialId(PATH_TO_AUTHORIAL_ID);
        validator.setPathToSipVersionNumber(PATH_TO_SIP_VERSION_NUMBER);
        validator.setPathToPhysicalVersionOf(PATH_TO_SIP_VERSION_OF);
    }

    @Test
    public void validateArclibXmlSuccess() throws IOException, SAXException, ParserConfigurationException {
        URL arclibXml = Resources.getResource(ARCLIB_XML);
        validator.validateFinalXml(Resources.toString(arclibXml, StandardCharsets.UTF_8), SIP_ID, AUTHORIAL_ID,
                SIP_VERSION_NUMBER, SIP_VERSION_OF);
    }

    /**
     * Tests that the {@link GeneralException} exception is thrown when the ARCLib XML does not conform to METS.
     */
    @Test
    public void validateArclibXmlWithValidatorNotMets() {
        URL arclibXml = Resources.getResource(INVALID_ARCLIB_XML_NOT_METS);
        assertThrown(() -> validator.validateFinalXml(Resources.toString(arclibXml, StandardCharsets.UTF_8),
                SIP_ID, AUTHORIAL_ID, SIP_VERSION_NUMBER, SIP_VERSION_OF)).isInstanceOf
                (GeneralException.class);
    }

    /**
     * Tests that the {@link MissingNode} exception is thrown when the ARCLib XML is missing the element <i>metsHdr</i>.
     */
    @Test
    public void validateArclibXmlWithValidatorMissingNode() {
        URL arclibXml = Resources.getResource(INVALID_ARCLIB_XML_MISSING_METS_HDR);
        assertThrown(() -> validator.validateFinalXml(Resources.toString(arclibXml, StandardCharsets.UTF_8),
                SIP_ID, AUTHORIAL_ID, SIP_VERSION_NUMBER, SIP_VERSION_OF)).isInstanceOf
                (MissingNode.class);
    }

    /**
     * Tests that the {@link MissingNode} exception is thrown when the ARCLib XML contains an invalid tag
     */
    @Test
    public void validateArclibXmlWithValidatorInvalidTag() {
        URL arclibXml = Resources.getResource(INVALID_ARCLIB_XML_INVALID_TAG);
        assertThrown(() -> validator.validateFinalXml(Resources.toString(arclibXml, StandardCharsets.UTF_8),
                SIP_ID, AUTHORIAL_ID, SIP_VERSION_NUMBER, SIP_VERSION_OF)).isInstanceOf
                (GeneralException.class);
    }
}
