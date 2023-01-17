package cz.cas.lib.arclib.service.arclibxml;

import com.google.common.io.Resources;
import cz.cas.lib.arclib.domain.Batch;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.ingestWorkflow.WorkflowDefinition;
import cz.cas.lib.arclib.domain.packages.Sip;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.exception.validation.MissingNode;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Instant;

import static helper.ThrowableAssertion.assertThrown;

public class ArclibXmlValidatorTest {

    private static final String ARCLIB_XML_SYSTEM_WIDE_VALIDATION_CFG = "arclibXmlSystemWideValidationConfig.csv";

    private static final String ARCLIB_XML = "arclibXmls/arclibXml.xml";

    private static final String INVALID_ARCLIB_XML_MISSING_METS_HDR =
            "arclibXmls/invalidArclibXmlMissingMetsHdr.xml";
    private static final String INVALID_ARCLIB_XML_INVALID_TAG = "arclibXmls/invalidArclibXmlInvalidTag.xml";
    private static final String INVALID_ARCLIB_XML_NOT_METS =
            "arclibXmls/invalidArclibXmlNotMets.xml";
    private static final String ARCLIB_SCHEMA = "xmlSchemas/arclibXml.xsd";
    private static final Integer SIP_VERSION_NUMBER = 1;
    private static final Integer XML_VERSION_NUMBER = 1;
    private static final String SIP_ID = "b8959a84-3625-4ced-9f7f-c26fe46cbdb4";
    private static final String USED_SIP_PROFILE = "sp123";
    private static final String USED_VALIDATION_PROFILE = "vp123";

    private ArclibXmlValidator validator;

    @Before
    public void setUp() throws IOException {
        validator = new ArclibXmlValidator();
        validator.setUris("http://www.loc.gov/METS/",
                "http://www.w3.org/2001/XMLSchema-instance",
                "http://arclib.lib.cas.cz/ARCLIB_XSD",
                "info:lc/xmlns/premis-v2",
                "http://www.openarchives.org/OAI/2.0/oai_dc/",
                "http://purl.org/dc/elements/1.1/",
                "http://www.w3.org/1999/xlink");

        validator.setSystemWideValidationItems(new ClassPathResource(ARCLIB_XML_SYSTEM_WIDE_VALIDATION_CFG));

        validator.setArclibXmlSchema(new ClassPathResource(ARCLIB_SCHEMA));
        String metsSchema = IOUtils.toString(new FileInputStream(Paths.get("src/main/resources/xmlSchemas/mets.xsd").toFile())).replace("classpath:/xmlSchemas/xlink.xsd", "./src/main/resources/xmlSchemas/xlink.xsd");
        String premisSchema = IOUtils.toString(new FileInputStream(Paths.get("src/main/resources/xmlSchemas/premis-v2-2.xsd").toFile())).replace("classpath:/xmlSchemas/xlink.xsd", "./src/main/resources/xmlSchemas/xlink.xsd");

        validator.setMetsSchema(new ByteArrayResource(metsSchema.getBytes()));
        validator.setPremisSchema(new ByteArrayResource(premisSchema.getBytes()));
    }

    @Test
    public void validateArclibXmlSuccess() throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
        URL arclibXml = Resources.getResource(ARCLIB_XML);

        validator.validateFinalXml(Resources.toString(arclibXml, StandardCharsets.UTF_8), mockIw(),
                USED_SIP_PROFILE, USED_VALIDATION_PROFILE, true, true);
    }

    /**
     * Tests that the {@link GeneralException} exception is thrown when the ARCLib XML does not conform to METS.
     */
    @Test
    public void validateArclibXmlWithValidatorNotMets() {
        URL arclibXml = Resources.getResource(INVALID_ARCLIB_XML_NOT_METS);
        assertThrown(() -> validator.validateFinalXml(Resources.toString(arclibXml, StandardCharsets.UTF_8), mockIw(),
                USED_SIP_PROFILE, USED_VALIDATION_PROFILE, true, true)
        ).isInstanceOf
                (GeneralException.class);
    }

    /**
     * Tests that the {@link MissingNode} exception is thrown when the ARCLib XML is missing the element <i>metsHdr</i>.
     */
    @Test
    public void validateArclibXmlWithValidatorMissingNode() {
        URL arclibXml = Resources.getResource(INVALID_ARCLIB_XML_MISSING_METS_HDR);
        assertThrown(() -> validator.validateFinalXml(Resources.toString(arclibXml, StandardCharsets.UTF_8), mockIw(),
                USED_SIP_PROFILE, USED_VALIDATION_PROFILE, true, true)
        ).isInstanceOf
                (MissingNode.class);
    }

    /**
     * Tests that the {@link MissingNode} exception is thrown when the ARCLib XML contains an invalid tag
     */
    @Test
    public void validateArclibXmlWithValidatorInvalidTag() {
        URL arclibXml = Resources.getResource(INVALID_ARCLIB_XML_INVALID_TAG);
        assertThrown(() -> validator.validateFinalXml(Resources.toString(arclibXml, StandardCharsets.UTF_8), mockIw(),
                USED_SIP_PROFILE, USED_VALIDATION_PROFILE, true, true)
        ).isInstanceOf
                (GeneralException.class);
    }

    private IngestWorkflow mockIw() {
        Sip newSip = new Sip();
        newSip.setId(SIP_ID);
        newSip.setVersionNumber(SIP_VERSION_NUMBER);

        WorkflowDefinition wd = new WorkflowDefinition();
        wd.setExternalId("wd123");

        ProducerProfile pp = new ProducerProfile();
        pp.setExternalId("pp123");
        pp.setWorkflowDefinition(wd);

        Batch batch = new Batch();
        batch.setProducerProfile(pp);

        IngestWorkflow newIw = new IngestWorkflow();
        newIw.setExternalId("ARCLIB_000000002");
        newIw.setSip(newSip);
        newIw.setCreated(Instant.parse("2019-05-15T20:59:38Z"));
        newIw.setUpdated(Instant.parse("2019-05-15T20:59:39Z"));
        newIw.setBatch(batch);
        newIw.setXmlVersionNumber(XML_VERSION_NUMBER);

        return newIw;
    }
}
