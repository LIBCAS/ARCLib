package cz.cas.lib.arclib.arclibxmlgeneration;

import com.google.common.io.Resources;
import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.cas.lib.arclib.domain.SipProfile;
import cz.cas.lib.arclib.service.ArclibXmlGenerator;
import cz.cas.lib.arclib.store.SipProfileStore;
import cz.inqool.uas.exception.MissingObject;
import helper.DbTest;
import org.dom4j.InvalidXPathException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static helper.ThrowableAssertion.assertThrown;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ArclibXmlGeneratorTest extends DbTest {

    private static final String SIP_ID = "KPW01169310";
    private static final String SIP_PATH = "SIP_packages/" + SIP_ID;

    private static String VALIDATION_CHECKS = "xmlSchemas/sipProfile.xsd";

    private ArclibXmlGenerator generator;
    private SipProfileStore store;

    @Before
    public void setUp() {
        store = new SipProfileStore();
        store.setEntityManager(getEm());
        store.setQueryFactory(new JPAQueryFactory(getEm()));

        generator = new ArclibXmlGenerator();
        generator.setStore(store);
        generator.setUris("http://www.loc.gov/METS/",
                "http://arclib.lib.cas.cz/ARCLIB_XML",
                "http://www.loc.gov/premis/v3"
        );
        generator.setSipProfileSchema(new ClassPathResource(VALIDATION_CHECKS));

    }

    /**
     * Tests that the attribute is created at the destination xPath
     */
    @Test
    public void generateArclibXmlAttributeMapping() throws IOException, SAXException, ParserConfigurationException,
            XPathExpressionException, TransformerException {
        SipProfile profile = new SipProfile();
        String sipProfileXml = Resources.toString(this.getClass().getResource(
                "/arclibxmlgeneration/sipProfiles/sipProfileAttributeMapping.xml"), StandardCharsets.UTF_8);
        profile.setXml(sipProfileXml);

        store.save(profile);

        String arclibXml = generator.generateArclibXml(SIP_PATH, profile.getId());
        assertThat(arclibXml, is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<METS:mets xmlns:METS=\"http://www.loc.gov/METS/\" LABEL=\"Z dějin malenovického hradu , [1953]\"/>"));
    }

    /**
     * Tests that multiple elements have been created at the destination xpath when there are multiple elements at the source xpath
     */
    @Test
    public void generateArclibXmlMultipleElementsMapping() throws SAXException, ParserConfigurationException, XPathExpressionException,
            IOException,
            TransformerException {
        SipProfile profile = new SipProfile();
        String sipProfileXml = Resources.toString(this.getClass().getResource(
                "/arclibxmlgeneration/sipProfiles/sipProfileMultipleElementsMapping.xml"), StandardCharsets.UTF_8);
        profile.setXml(sipProfileXml);

        store.save(profile);

        String arclibXml = generator.generateArclibXml(SIP_PATH, profile.getId());
        assertThat(arclibXml, is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<METS:mets xmlns:METS=\"http://www.loc.gov/METS/\"><METS:metsHdr xmlns:METS=\"http://arclib.lib.cas.cz/ARCLIB_XML\"><METS:agent><METS:name>ZLG001</METS:name>\r\n</METS:agent></METS:metsHdr><METS:metsHdr xmlns:METS=\"http://arclib.lib.cas.cz/ARCLIB_XML\"><METS:agent><METS:name>Exon s.r.o.</METS:name>\r\n</METS:agent></METS:metsHdr></METS:mets>"));
    }

    /**
     * Tests that only single element has been created at the destination xPath when specifying the position of the source element
     */
    @Test
    public void generateArclibXmlElementAtPositionMapping() throws SAXException, ParserConfigurationException, XPathExpressionException,
            IOException,
            TransformerException {
        SipProfile profile = new SipProfile();
        String sipProfileXml = Resources.toString(this.getClass().getResource(
                "/arclibxmlgeneration/sipProfiles/sipProfileElementAtPositionMapping.xml"), StandardCharsets
                .UTF_8);
        profile.setXml(sipProfileXml);

        store.save(profile);

        String arclibXml = generator.generateArclibXml(SIP_PATH, profile.getId());
        assertThat(arclibXml, is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<METS:mets xmlns:METS=\"http://www.loc.gov/METS/\"><METS:metsHdr xmlns:METS=\"http://arclib.lib.cas.cz/ARCLIB_XML\"><METS:agent ROLE=\"CREATOR\" TYPE=\"ORGANIZATION\"> \r\n\t\t\t<METS:name>Exon s.r.o.</METS:name>\r\n\t\t</METS:agent>\r\n</METS:metsHdr></METS:mets>"));
    }

    /**
     * Tests that also the child elements have been created at the destination xpath when the source element is nested
     */
    @Test
    public void generateArclibXmlNestedElementMapping() throws SAXException, ParserConfigurationException, XPathExpressionException,
            IOException,
            TransformerException {
        SipProfile profile = new SipProfile();
        String sipProfileXml = Resources.toString(this.getClass().getResource(
                "/arclibxmlgeneration/sipProfiles/sipProfileNestedElementMapping.xml"), StandardCharsets
                .UTF_8);
        profile.setXml(sipProfileXml);

        store.save(profile);

        String arclibXml = generator.generateArclibXml(SIP_PATH, profile.getId());
        assertThat(arclibXml, is(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<METS:mets xmlns:METS=\"http://www.loc.gov/METS/\"><METS:metsHdr CREATEDATE=\"2013-01-22T10:55:20Z\" ID=\"kpw01169310\" LASTMODDATE=\"2013-01-22T10:55:20Z\" RECORDSTATUS=\"COMPLETE\">\r\n\t\t<METS:agent ROLE=\"CREATOR\" TYPE=\"ORGANIZATION\"> \r\n\t\t\t<METS:name>Exon s.r.o.</METS:name>\r\n\t\t</METS:agent>\r\n\t\t<METS:agent ROLE=\"ARCHIVIST\" TYPE=\"ORGANIZATION\"> \r\n\t\t\t<METS:name>ZLG001</METS:name>\r\n\t\t</METS:agent>\r\n\t</METS:metsHdr>\r\n</METS:mets>"));
    }

    /**
     * Tests that multiple mappings in a SIP profile are supported
     */
    @Test
    public void generateArclibXmlMultipleMappings() throws SAXException, ParserConfigurationException, XPathExpressionException,
            IOException,
            TransformerException {
        SipProfile profile = new SipProfile();
        String sipProfileXml = Resources.toString(this.getClass().getResource(
                "/arclibxmlgeneration/sipProfiles/sipProfileMultipleMappings.xml"), StandardCharsets.UTF_8);
        profile.setXml(sipProfileXml);

        store.save(profile);

        String arclibXml = generator.generateArclibXml(SIP_PATH, profile.getId());
        assertThat(arclibXml, is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<METS:mets xmlns:METS=\"http://www.loc.gov/METS/\" LABEL=\"Z dějin malenovického hradu , [1953]\"><METS:metsHdr CREATEDATE=\"2013-01-22T10:55:20Z\" ID=\"kpw01169310\">\r\n<METS:agent ROLE=\"CREATOR\" TYPE=\"INDIVIDUAL\"> \r\n<METS:name>Administrator</METS:name>\r\n</METS:agent>\r\n</METS:metsHdr>\r\n</METS:mets>"));
    }

    /**
     * Tests that the {@link MissingObject} exception is thrown when the specified sip profile does not exist
     */
    @Test
    public void generateArclibXmlNonExistentProfile() {
        assertThrown(() -> generator.generateArclibXml(SIP_PATH, "A%#$@")).isInstanceOf(MissingObject.class);
    }

    /**
     * Tests that the {@link IllegalArgumentException} exception is thrown when the SIP package at the specified path does not exist
     */
    @Test
    public void generateArclibXmlNonExistentSip() throws IOException {
        SipProfile profile = new SipProfile();
        String sipProfileXml = Resources.toString(this.getClass().getResource
                ("/arclibxmlgeneration/sipProfiles/sipProfileAttributeMapping.xml"), StandardCharsets.UTF_8);
        profile.setXml(sipProfileXml);

        store.save(profile);

        assertThrown(() -> generator.generateArclibXml("%@#@%@!", profile.getId())).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Tests that the {@link InvalidXPathException} exception is thrown when the SIP profile contains an invalid xpath
     */
    @Test
    public void generateArclibInvalidXPath() throws IOException {
        SipProfile profile = new SipProfile();
        String sipProfileXml = Resources.toString(this.getClass().getResource(
                "/arclibxmlgeneration/sipProfiles/sipProfileInvalidXPath.xml"), StandardCharsets.UTF_8);
        profile.setXml(sipProfileXml);

        store.save(profile);

        assertThrown(() -> generator.generateArclibXml(SIP_PATH, profile.getId())).isInstanceOf(InvalidXPathException.class);
    }

    /**
     * Tests the aggregation mapping for generation of the <i>ARCLIB:eventAgent</i> element.
     */
    @Test
    public void generateArclibEventAgent() throws IOException, SAXException, ParserConfigurationException, XPathExpressionException,
            TransformerException {
        SipProfile profile = new SipProfile();
        String sipProfileXml = Resources.toString(this.getClass().getResource(
                "/arclibxmlgeneration/sipProfiles/sipProfileEventCount.xml"), StandardCharsets.UTF_8);
        profile.setXml(sipProfileXml);

        store.save(profile);

        String arclibXml = generator.generateArclibXml(SIP_PATH, profile.getId());
        assertThat(arclibXml, is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<METS:mets xmlns:METS=\"http://www.loc.gov/METS/\"><METS:amdSec xmlns:METS=\"http://arclib.lib.cas.cz/ARCLIB_XML\"><METS:digiprovMD><METS:mdWrap><METS:xmlData><ARCLIB:eventAgents xmlns:ARCLIB=\"http://arclib.lib.cas.cz/ARCLIB_XML\"><ARCLIB:eventAgent><premis:eventType>capture</premis:eventType>\r\n</ARCLIB:eventAgent></ARCLIB:eventAgents></METS:xmlData></METS:mdWrap></METS:digiprovMD></METS:amdSec><METS:amdSec xmlns:METS=\"http://arclib.lib.cas.cz/ARCLIB_XML\"><METS:digiprovMD><METS:mdWrap><METS:xmlData><ARCLIB:eventAgents xmlns:ARCLIB=\"http://arclib.lib.cas.cz/ARCLIB_XML\"><ARCLIB:eventAgent><premis:eventType>migration</premis:eventType>\r\n</ARCLIB:eventAgent></ARCLIB:eventAgents></METS:xmlData></METS:mdWrap></METS:digiprovMD></METS:amdSec></METS:mets>"));
    }
}
