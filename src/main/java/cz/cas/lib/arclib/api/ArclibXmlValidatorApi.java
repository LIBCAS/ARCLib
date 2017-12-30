package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.service.ArclibXmlValidator;
import lombok.Getter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@RestController
@RequestMapping("/api/arclib_xml_validator")
public class ArclibXmlValidatorApi {
    @Getter
    private ArclibXmlValidator validator;

    /**
     * Validates ARCLib XML.
     * In case the ARCLib XML is not valid, a corresponding exception is thrown.
     *
     * @param xml ARCLib XML to validate
     *
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     * @throws IOException
     */
    @RequestMapping(value = "/validate", method = RequestMethod.POST)
    public void validateArclibXml(@RequestParam("xml") MultipartFile xml)
            throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        validator.validateArclibXml(new ByteArrayInputStream(xml.getBytes()));
    }

    @Inject
    public void setValidator(ArclibXmlValidator validator) {
        this.validator = validator;
    }
}
