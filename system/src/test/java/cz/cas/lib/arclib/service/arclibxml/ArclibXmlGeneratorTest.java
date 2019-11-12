package cz.cas.lib.arclib.service.arclibxml;

import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.assertThat;


public class ArclibXmlGeneratorTest {

    private Map<String, String> uris;

    @Test
    public void metadataUpdate() throws IOException, DocumentException {
        ArclibXmlGenerator generator = new ArclibXmlGenerator();
        SAXReader reader = new SAXReader();
        generator.setUris("http://www.loc.gov/METS/",
                "http://arclib.lib.cas.cz/ARCLIB_XSD",
                "info:lc/xmlns/premis-v2",
                "http://www.openarchives.org/OAI/2.0/oai_dc/",
                "http://purl.org/dc/elements/1.1/",
                "http://purl.org/dc/terms/");
        String originalXml = new String(Files.readAllBytes(Paths.get("src/test/resources/arclibXmls/simplifiedArclibXml.xml")), StandardCharsets.UTF_8);
        IngestWorkflow originalIw = new IngestWorkflow();
        originalIw.setExternalId("fst");
        IngestWorkflow fstUpdateIw = new IngestWorkflow();
        fstUpdateIw.setExternalId("snd");
        fstUpdateIw.setRelatedWorkflow(originalIw);
        IngestWorkflow sndUpdateIw = new IngestWorkflow();
        sndUpdateIw.setExternalId("thi");
        sndUpdateIw.setRelatedWorkflow(fstUpdateIw);


        String fstUpdateReason = "first update";
        String fstUser = "first user";
        String updatedXml = generator.addUpdateMetadata(originalXml, fstUpdateReason, fstUser, fstUpdateIw);
        String sndUpdateReason = "snd update";
        String sndUser = "snd user";
        updatedXml = generator.addUpdateMetadata(updatedXml, sndUpdateReason, sndUser, fstUpdateIw);

        Document updatedDoc = reader.read(new ByteArrayInputStream(updatedXml.getBytes()));
        Node fstGeneratedEvent = updatedDoc.createXPath("//METS:digiprovMD[@ID='EVENT_003']").selectSingleNode(updatedDoc);
        assertThat(fstGeneratedEvent.asXML(), containsString(fstUpdateReason));
        assertThat(fstGeneratedEvent.asXML(), containsString(fstUser));
        assertThat(fstGeneratedEvent.createXPath(".//premis:eventIdentifierValue").selectSingleNode(fstGeneratedEvent).getText(), endsWith("1"));
        Node sndGeneratedEvent = updatedDoc.createXPath("//METS:digiprovMD[@ID='EVENT_004']").selectSingleNode(updatedDoc);
        assertThat(sndGeneratedEvent.asXML(), containsString(sndUpdateReason));
        assertThat(sndGeneratedEvent.asXML(), containsString(sndUser));
        assertThat(sndGeneratedEvent.createXPath(".//premis:eventIdentifierValue").selectSingleNode(sndGeneratedEvent).getText(), endsWith("2"));
    }
}
