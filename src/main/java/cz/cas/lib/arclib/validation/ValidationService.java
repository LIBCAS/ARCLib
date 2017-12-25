package cz.cas.lib.arclib.validation;

import cz.cas.lib.arclib.domain.ValidationProfile;
import cz.cas.lib.arclib.store.ValidationProfileStore;
import cz.cas.lib.arclib.validation.exception.InvalidNodeValue;
import cz.cas.lib.arclib.validation.exception.MissingFile;
import cz.cas.lib.arclib.validation.exception.SchemaValidationError;
import cz.cas.lib.arclib.validation.exception.WrongNodeValue;
import cz.inqool.uas.exception.GeneralException;
import cz.inqool.uas.exception.MissingObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cz.inqool.uas.util.Utils.notNull;

@Slf4j
@Service
public class ValidationService {

    private ValidationProfileStore validationProfileStore;

    /**
     * Validates SIP using the given validation profile. If the validation has failed, specific exception is thrown describing the reason
     * of the validation failure. There are three types of checks in a validation profile:
     *
     * 1. checks for existence of specified files
     * 2. validation against respective XSD schemas of specified XML files
     * 3. checks of the values of some nodes specified by the XPath in XML files on the specified filePath
     *
     * @param sipPath file path to the sip package to validate
     * @param validationProfileId id of the validation profile
     * @throws IOException if some of the files addressed from the validation profile is not found
     * @throws SAXException if the validation profile cannot be parsed
     * @throws XPathExpressionException if there is an error in the XPath expression
     * @throws ParserConfigurationException
     */
    public void validateSip(String sipId, String sipPath, String validationProfileId)
            throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        log.info("Validation of SIP " + sipId + " with profile " + validationProfileId + " started.");

        ValidationProfile profile = validationProfileStore.find(validationProfileId);
        notNull(profile, () -> new MissingObject(ValidationProfile.class, validationProfileId));

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        dBuilder = dbFactory.newDocumentBuilder();

        String validationProfileXml = profile.getXml();
        Document validationProfileDoc = dBuilder.parse(new ByteArrayInputStream(validationProfileXml.getBytes()));
        validationProfileDoc.getDocumentElement().normalize();

        performFileExistenceChecks(sipPath, validationProfileDoc, sipId);
        performValidationSchemaChecks(sipPath, validationProfileDoc, sipId);
        performNodeValueChecks(sipPath, validationProfileDoc, sipId);

        log.info("Validation of SIP " + sipId + " with profile succeeded.");
    }

    /**
     * Performs all file existence checks contained in the validation profile. If the validation has failed (some of the files specified
     * in the validation profile do not exist), {@link MissingFile} exception is thrown.
     *
     * @param sipPath path to the SIP
     * @param validationProfileDoc document with the validation profile
     * @param validationProfileId id of the validation profile
     * @throws XPathExpressionException if there is an error in the XPath expression
     */
    private void performFileExistenceChecks(String sipPath, Document validationProfileDoc, String validationProfileId) throws
            XPathExpressionException {
        XPath xPath =  XPathFactory.newInstance().newXPath();

        NodeList nodes = (NodeList) xPath.compile("/profile/rule/fileExistenceCheck")
                .evaluate(validationProfileDoc, XPathConstants.NODESET);
        for (int i = 0; i< nodes.getLength(); i++) {

            Element element = (Element) nodes.item(i);
            String relativePath = element.getElementsByTagName("filePath").item(0).getTextContent();
            String absolutePath = sipPath + relativePath;
            if (!ValidationChecker.fileExists(absolutePath)) {
                log.info("Validation of SIP with profile " + validationProfileId + " failed. File at \"" + relativePath + "\" is missing.");
                throw new MissingFile(relativePath, validationProfileId);
            }
        }
    }

    /**
     * Performs all validation schema checks contained in the validation profile. If the validation has failed (some of the XMLs
     * specified in the validation profile do not match their respective validation schemas) {@link SchemaValidationError} is thrown.
     *
     * @param sipPath path to the SIP
     * @param validationProfileDoc document with the validation profile
     * @param validationProfileId id of the validation profile
     * @throws IOException if some of the XMLs address from the validation profile does not exist
     * @throws XPathExpressionException if there is an error in the XPath expression
     * @throws SAXException if the XSD schema is invalid
     */
    private void performValidationSchemaChecks(String sipPath, Document validationProfileDoc, String validationProfileId) throws
            XPathExpressionException, IOException, SAXException {
        XPath xPath =  XPathFactory.newInstance().newXPath();

        NodeList nodes = (NodeList) xPath.compile("/profile/rule/validationSchemaCheck")
                .evaluate(validationProfileDoc,
                        XPathConstants.NODESET);
        for (int i = 0; i< nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            String filePath = element.getElementsByTagName("filePath").item(0).getTextContent();
            String schema = element.getElementsByTagName("schema").item(0).getTextContent();

            String absoluteFilePath = sipPath + filePath;

            try {
                ValidationChecker.validateWithXMLSchema(new FileInputStream(absoluteFilePath), new InputStream[] {
                        new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8.name()))});
            } catch (GeneralException e) {
                log.info("Validation of SIP with profile " + validationProfileId + " failed. File at \"" + filePath + "\" is not " +
                        "valid against its corresponding schema.");
                throw new SchemaValidationError(filePath, schema, e.getMessage());
            }
        }
    }

    /**
     * Performs all attribute value checks contained in the validation profile. If the validation has failed (some of the attributes
     * specified in the validation profile do not match their specified values or regex) {@link WrongNodeValue} or
     * {@link InvalidNodeValue} is thrown.
     *
     * @param sipPath path to the SIP
     * @param validationProfileDoc document with the validation profile
     * @param validationProfileId id of the validation profile
     * @throws IOException if some of the files addressed from the validation profile is not found
     * @throws XPathExpressionException if there is an error in the XPath expression
     * @throws SAXException if the validationProfileDoc cannot be parsed
     * @throws ParserConfigurationException
     */
    private void performNodeValueChecks(String sipPath, Document validationProfileDoc, String validationProfileId)
            throws IOException, ParserConfigurationException, XPathExpressionException, SAXException {
        XPath xPath =  XPathFactory.newInstance().newXPath();

        NodeList nodes = (NodeList) xPath.compile("/profile/rule/nodeCheck")
                .evaluate(validationProfileDoc, XPathConstants.NODESET);
        for (int i = 0; i< nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);

            String filePath = element.getElementsByTagName("filePath").item(0).getTextContent();
            String absoluteFilePath = sipPath + filePath;

            String expression = element.getElementsByTagName("xPath").item(0).getTextContent();

            String actualValue =  ValidationChecker.findWithXPath(new FileInputStream(absoluteFilePath), expression).item(0).getTextContent();

            Node valueElement = element.getElementsByTagName("value").item(0);
            if (valueElement != null) {
                String expectedValue = valueElement.getTextContent();
                // compare with value
                if (!expectedValue.equals(actualValue)) {
                    log.info("Validation of SIP with profile " + validationProfileId + " failed. Expected value of node at path \"" +
                            expression + "\" is " + expectedValue + ". Actual value is " + actualValue + ".");
                    throw new WrongNodeValue(expectedValue, actualValue, absoluteFilePath, expression);                }
            } else {
                //compare with regex
                Node regexElement = element.getElementsByTagName("regex").item(0);
                String regex = regexElement.getTextContent();
                Pattern pattern = Pattern.compile(regex);
                Matcher m = pattern.matcher(actualValue);
                if (!m.matches()) {
                    log.info("Validation of SIP with profile " + validationProfileId + " failed. Value " + actualValue + " of node at " +
                            "path \"" + expression + "\" does not match regex " + regex + ".");
                    throw new InvalidNodeValue(regex, actualValue, absoluteFilePath, expression);                }
            }
        }
    }

    @Inject
    public void setValidationProfileStore(ValidationProfileStore validationProfileStore) {
        this.validationProfileStore = validationProfileStore;
    }
}
