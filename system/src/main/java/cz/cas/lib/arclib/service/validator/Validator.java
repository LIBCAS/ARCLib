package cz.cas.lib.arclib.service.validator;

import cz.cas.lib.arclib.domain.profiles.ValidationProfile;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.exception.validation.InvalidSipNodeValue;
import cz.cas.lib.arclib.exception.validation.MissingFile;
import cz.cas.lib.arclib.exception.validation.SchemaValidationError;
import cz.cas.lib.arclib.exception.validation.WrongNodeValue;
import cz.cas.lib.arclib.store.ValidationProfileStore;
import cz.cas.lib.arclib.utils.XmlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cz.cas.lib.arclib.utils.XmlUtils.createDomAndXpath;
import static cz.cas.lib.core.util.Utils.listFilesMatchingGlobPattern;
import static cz.cas.lib.core.util.Utils.notNull;

@Slf4j
@Service
public class Validator {

    private ValidationProfileStore validationProfileStore;

    /**
     * Validates SIP using the given validation profile. If the validation has failed, corresponding exception is thrown
     * with the reason of the validation failure. There are three types of checks in a validation profile:
     * <p>
     * 1. checks for existence of specified files
     * 2. validation against XSD schemas of specified XML files
     * 3. checks of values of some nodes specified by the XPath in XML files on the specified filePathGlobPattern
     *
     * @param sipId               id of the SIP
     * @param sipPath             file path to the sip package to validate
     * @param validationProfileExternalId id of the validation profile
     * @throws SAXException             if validation profile cannot be parsed
     * @throws XPathExpressionException if there is error in the XPath expression
     */
    public void validateSip(String sipId, String sipPath, String validationProfileExternalId)
            throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        log.debug("Validation of SIP " + sipId + " with profile " + validationProfileExternalId + " started.");

        ValidationProfile profile = validationProfileStore.findByExternalId(validationProfileExternalId);
        notNull(profile, () -> new MissingObject(ValidationProfile.class, validationProfileExternalId));

        String validationProfileXml = profile.getXml();
        Pair<Document, XPath> domAndXpath = createDomAndXpath(new ByteArrayInputStream(validationProfileXml.getBytes()), null);
        Document validationProfileDom = domAndXpath.getKey();
        XPath xpath = domAndXpath.getRight();
        validationProfileDom.getDocumentElement().normalize();

        performFileExistenceChecks(sipPath, validationProfileDom, xpath, validationProfileExternalId, sipId);
        performValidationSchemaChecks(sipPath, validationProfileDom, xpath, validationProfileExternalId, sipId);
        performNodeValueChecks(sipPath, validationProfileDom, xpath, validationProfileExternalId, sipId);

        log.debug("Validation of SIP " + sipId + " with profile " + validationProfileExternalId + " succeeded.");
    }

    /**
     * Performs all file existence checks contained in the validation profile. If the validation has failed
     * (some of the files specified in the validation profile do not exist), {@link MissingFile} exception is thrown.
     *
     * @param sipPath                     path to the SIP
     * @param validationProfileDom        document with the validation profile
     * @param validationProfileExternalId id of the validation profile
     * @param sipId                       id of the SIP
     * @throws XPathExpressionException if there is an error in the XPath expression
     */
    private void performFileExistenceChecks(String sipPath, Document validationProfileDom, XPath xPath, String validationProfileExternalId,
                                            String sipId) throws XPathExpressionException, IOException {
        NodeList nodes = (NodeList) xPath.compile("/profile/rule/fileExistenceCheck")
                .evaluate(validationProfileDom, XPathConstants.NODESET);
        for (int i = 0; i < nodes.getLength(); i++) {

            Element element = (Element) nodes.item(i);
            String relativePath = element.getElementsByTagName("filePathGlobPattern").item(0).getTextContent();

            List<File> matchingFiles = listFilesMatchingGlobPattern(new File(sipPath), relativePath);
            if (matchingFiles.size() == 0) {
                log.debug("Validation of SIP " + sipId + " with profile " + validationProfileExternalId +
                        " failed. File at \"" + relativePath + "\" is missing.");
                throw new MissingFile(sipId, validationProfileExternalId, relativePath);
            }
        }
    }

    /**
     * Performs all validation schema checks contained in the validation profile. If the validation has failed (some of the XMLs
     * specified in the validation profile do not match their respective validation schemas) {@link SchemaValidationError} is thrown.
     *
     * @param sipPath                     path to the SIP
     * @param validationProfileDom        document with the validation profile
     * @param validationProfileExternalId id of the validation profile
     * @param sipId                       id of the SIP
     * @throws XPathExpressionException if there is an error in the XPath expression
     * @throws SAXException             if the XSD schema is invalid
     * @throws IOException              if the validated file is inaccessible or the XSD schema is unreadable
     */
    private void performValidationSchemaChecks(String sipPath, Document validationProfileDom, XPath xPath, String validationProfileExternalId,
                                               String sipId) throws XPathExpressionException, IOException {
        NodeList nodes = (NodeList) xPath.compile("/profile/rule/validationSchemaCheck")
                .evaluate(validationProfileDom,
                        XPathConstants.NODESET);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            String filePathGlobPattern = element.getElementsByTagName("filePathGlobPattern").item(0).getTextContent();
            String schema = element.getElementsByTagName("schema").item(0).getTextContent().trim();

            List<File> matchingFiles = listFilesMatchingGlobPattern(new File(sipPath), filePathGlobPattern);
            if (matchingFiles.size() == 0) throw new MissingFile(sipId, validationProfileExternalId, filePathGlobPattern);

            for (File file: matchingFiles) {
                try {
                    XmlUtils.validateWithXMLSchema(Files.readString(file.toPath()), new InputStream[]{
                            new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8.name()))}, "validationSchemaCheck rule");
                } catch (SchemaValidationError e) {
                    log.error("Validation of SIP " + sipId + " with profile " + validationProfileExternalId + " failed. File at \"" +
                            filePathGlobPattern + "\" is not valid against its corresponding schema.");
                    throw e;
                }
            }
        }
    }

    /**
     * Performs all attribute value checks contained in the validation profile. If the validation has failed (some of the attributes
     * specified in the validation profile do not match their specified values or regex) {@link WrongNodeValue} or
     * {@link InvalidSipNodeValue} is thrown.
     *
     * @param sipPath                     path to the SIP
     * @param validationProfileDom        document with the validation profile
     * @param validationProfileExternalId id of the validation profile
     * @param sipId                       id of the SIP
     * @throws XPathExpressionException if there is an error in the XPath expression
     * @throws SAXException             if the validationProfileDom cannot be parsed
     * @throws IOException              if some file addressed from the validation profile is inaccessible
     */
    private void performNodeValueChecks(String sipPath, Document validationProfileDom, XPath xPath, String validationProfileExternalId,
                                        String sipId)
            throws ParserConfigurationException, XPathExpressionException, SAXException, IOException {
        NodeList nodes = (NodeList) xPath.compile("/profile/rule/nodeCheck")
                .evaluate(validationProfileDom, XPathConstants.NODESET);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);

            String filePathGlobPattern = element.getElementsByTagName("filePathGlobPattern").item(0).getTextContent();
            List<File> matchingFiles = listFilesMatchingGlobPattern(new File(sipPath), filePathGlobPattern);
            if (matchingFiles.size() == 0)
                throw new MissingFile(sipId, validationProfileExternalId, filePathGlobPattern);

            String expression = element.getElementsByTagName("xPath").item(0).getTextContent();

            for (File file : matchingFiles) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    String actualValue = XmlUtils.findSingleNodeWithXPath(fis, expression, null).getTextContent();

                    Node valueElement = element.getElementsByTagName("value").item(0);
                    if (valueElement != null) {
                        String expectedValue = valueElement.getTextContent();
                        //compare with value
                        if (!expectedValue.equals(actualValue)) {
                            log.debug("Validation of SIP " + sipId + " with profile " + validationProfileExternalId +
                                    " failed. Expected value of node at path \"" + expression + "\" is " + expectedValue +
                                    ". Actual value is " + actualValue + ".");
                            throw new WrongNodeValue(sipId, validationProfileExternalId, expectedValue, actualValue,
                                    file.getPath(), expression);
                        }
                    } else {
                        //compare with regex
                        Node regexElement = element.getElementsByTagName("regex").item(0);
                        String regex = regexElement.getTextContent();
                        Pattern pattern = Pattern.compile(regex);
                        Matcher m = pattern.matcher(actualValue);
                        if (!m.matches()) {
                            log.debug("Validation of SIP " + sipId + " with profile " + validationProfileExternalId +
                                    " failed. Value " + actualValue + " of node at " + "path \"" + expression +
                                    "\" does not match regex " + regex + ".");
                            throw new InvalidSipNodeValue(sipId, validationProfileExternalId, regex, actualValue,
                                    file.getPath(), expression);
                        }
                    }
                }
            }
        }
    }

    @Autowired
    public void setValidationProfileStore(ValidationProfileStore validationProfileStore) {
        this.validationProfileStore = validationProfileStore;
    }
}
