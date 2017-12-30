package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.service.ArclibXmlGenerator;
import lombok.Getter;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/arclib_xml_generator")
public class ArclibXmlGeneratorApi {
    @Getter
    private ArclibXmlGenerator generator;

    /**
     * Generates ARCLib XML from the SIP package using SIP profile.
     *
     * @param sipPath path to the SIP package
     * @param sipProfileId id of the SIP profile
     * @param response response with the ARCLib XML
     *
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     * @throws XPathExpressionException
     * @throws TransformerException
     */
    @RequestMapping(value = "/generate", method = RequestMethod.PUT)
    public void generateArclibXml(
            @RequestParam("sipPath") String sipPath, @RequestParam("sipProfileId") String sipProfileId, HttpServletResponse response)
            throws ParserConfigurationException, IOException, SAXException, XPathExpressionException, TransformerException {

        String xml = generator.generateArclibXml(sipPath, sipProfileId);
        response.setContentType("application/xml");
        response.setStatus(200);
        response.addHeader("Content-Disposition", "attachment; sipPath=" + sipPath + "_sipProfileId_" + sipProfileId);

        ByteArrayInputStream xmlInputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8.name()));

        IOUtils.copyLarge(xmlInputStream, response.getOutputStream());
        IOUtils.closeQuietly(xmlInputStream);
    }

    @Inject
    public void setGenerator(ArclibXmlGenerator generator) {
        this.generator = generator;
    }
}
