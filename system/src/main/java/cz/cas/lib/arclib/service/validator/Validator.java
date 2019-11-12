package cz.cas.lib.arclib.service.validator;

import cz.cas.lib.arclib.domain.profiles.ValidationProfile;
import cz.cas.lib.arclib.exception.validation.InvalidSipNodeValue;
import cz.cas.lib.arclib.exception.validation.MissingFile;
import cz.cas.lib.arclib.exception.validation.SchemaValidationError;
import cz.cas.lib.arclib.exception.validation.WrongNodeValue;
import cz.cas.lib.arclib.store.ValidationProfileStore;
import cz.cas.lib.arclib.utils.XmlUtils;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
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
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     * @param validationProfileId id of the validation profile
     * @throws SAXException             if validation profile cannot be parsed
     * @throws XPathExpressionException if there is error in the XPath expression
     */
    public void validateSip(String sipId, String sipPath, String validationProfileId)
            throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        log.debug("Validation of SIP " + sipId + " with profile " + validationProfileId + " started.");

        ValidationProfile profile = validationProfileStore.find(validationProfileId);
        notNull(profile, () -> new MissingObject(ValidationProfile.class, validationProfileId));

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        dBuilder = dbFactory.newDocumentBuilder();

        String validationProfileXml = profile.getXml();
        Document validationProfileDoc = dBuilder.parse(new ByteArrayInputStream(validationProfileXml.getBytes()));
        validationProfileDoc.getDocumentElement().normalize();

        performFileExistenceChecks(sipPath, validationProfileDoc, validationProfileId, sipId);
        performValidationSchemaChecks(sipPath, validationProfileDoc, validationProfileId, sipId);
        performNodeValueChecks(sipPath, validationProfileDoc, validationProfileId, sipId);

        log.debug("Validation of SIP " + sipId + " with profile " + validationProfileId + " succeeded.");
    }

    /**
     * Performs all file existence checks contained in the validation profile. If the validation has failed
     * (some of the files specified in the validation profile do not exist), {@link MissingFile} exception is thrown.
     *
     * @param sipPath              path to the SIP
     * @param validationProfileDoc document with the validation profile
     * @param validationProfileId  id of the validation profile
     * @param sipId                id of the SIP
     * @throws XPathExpressionException if there is an error in the XPath expression
     */
    private void performFileExistenceChecks(String sipPath, Document validationProfileDoc, String validationProfileId,
                                            String sipId) throws XPathExpressionException, IOException {
        XPath xPath = XPathFactory.newInstance().newXPath();

        NodeList nodes = (NodeList) xPath.compile("/profile/rule/fileExistenceCheck")
                .evaluate(validationProfileDoc, XPathConstants.NODESET);
        for (int i = 0; i < nodes.getLength(); i++) {

            Element element = (Element) nodes.item(i);
            String relativePath = element.getElementsByTagName("filePathGlobPattern").item(0).getTextContent();

            List<File> matchingFiles = listFilesMatchingGlobPattern(new File(sipPath), relativePath);
            if (matchingFiles.size() == 0) {
                log.debug("Validation of SIP " + sipId + " with profile " + validationProfileId +
                        " failed. File at \"" + relativePath + "\" is missing.");
                throw new MissingFile(sipId, validationProfileId, relativePath);
            }
        }
    }

    /**
     * Performs all validation schema checks contained in the validation profile. If the validation has failed (some of the XMLs
     * specified in the validation profile do not match their respective validation schemas) {@link SchemaValidationError} is thrown.
     *
     * @param sipPath              path to the SIP
     * @param validationProfileDoc document with the validation profile
     * @param validationProfileId  id of the validation profile
     * @param sipId                id of the SIP
     * @throws XPathExpressionException if there is an error in the XPath expression
     * @throws SAXException             if the XSD schema is invalid
     * @throws IOException              if the validated file is inaccessible or the XSD schema is unreadable
     */
    private void performValidationSchemaChecks(String sipPath, Document validationProfileDoc, String validationProfileId,
                                               String sipId) throws SAXException, XPathExpressionException, IOException {
        XPath xPath = XPathFactory.newInstance().newXPath();

        NodeList nodes = (NodeList) xPath.compile("/profile/rule/validationSchemaCheck")
                .evaluate(validationProfileDoc,
                        XPathConstants.NODESET);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            String filePathGlobPattern = element.getElementsByTagName("filePathGlobPattern").item(0).getTextContent();
            String schema = element.getElementsByTagName("schema").item(0).getTextContent();

            List<File> matchingFiles = listFilesMatchingGlobPattern(new File(sipPath), filePathGlobPattern);
            if (matchingFiles.size() == 0) throw new MissingFile(sipId, validationProfileId, filePathGlobPattern);

            for (File file: matchingFiles) {
                try (FileInputStream fos = new FileInputStream(file)) {
                    XmlUtils.validateWithXMLSchema(fos, new InputStream[]{
                            new ByteArrayInputStream(schema.getBytes(StandardCharsets.UTF_8.name()))});
                } catch (GeneralException e) {
                    log.debug("Validation of SIP " + sipId + " with profile " + validationProfileId + " failed. File at \"" +
                            filePathGlobPattern + "\" is not valid against its corresponding schema.");
                    throw new SchemaValidationError(sipId, validationProfileId, filePathGlobPattern, schema, e.getMessage());
                }
            }
        }
    }

    /**
     * Performs all attribute value checks contained in the validation profile. If the validation has failed (some of the attributes
     * specified in the validation profile do not match their specified values or regex) {@link WrongNodeValue} or
     * {@link InvalidSipNodeValue} is thrown.
     *
     * @param sipPath              path to the SIP
     * @param validationProfileDoc document with the validation profile
     * @param validationProfileId  id of the validation profile
     * @param sipId                id of the SIP
     * @throws XPathExpressionException if there is an error in the XPath expression
     * @throws SAXException             if the validationProfileDoc cannot be parsed
     * @throws IOException              if some file addressed from the validation profile is inaccessible
     */
    private void performNodeValueChecks(String sipPath, Document validationProfileDoc, String validationProfileId,
                                        String sipId)
            throws ParserConfigurationException, XPathExpressionException, SAXException, IOException {
        XPath xPath = XPathFactory.newInstance().newXPath();

        NodeList nodes = (NodeList) xPath.compile("/profile/rule/nodeCheck")
                .evaluate(validationProfileDoc, XPathConstants.NODESET);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);

            String filePathGlobPattern = element.getElementsByTagName("filePathGlobPattern").item(0).getTextContent();
            List<File> matchingFiles = listFilesMatchingGlobPattern(new File(sipPath), filePathGlobPattern);
            if (matchingFiles.size() == 0) throw new MissingFile(sipId, validationProfileId, filePathGlobPattern);

            String expression = element.getElementsByTagName("xPath").item(0).getTextContent();

            for (File file: matchingFiles) {
                try (FileInputStream fos = new FileInputStream(file)) {
                    String actualValue = XmlUtils.findWithXPath(fos, expression).item(0).getTextContent();

                    Node valueElement = element.getElementsByTagName("value").item(0);
                    if (valueElement != null) {
                        String expectedValue = valueElement.getTextContent();
                        //compare with value
                        if (!expectedValue.equals(actualValue)) {
                            log.debug("Validation of SIP " + sipId + " with profile " + validationProfileId +
                                    " failed. Expected value of node at path \"" + expression + "\" is " + expectedValue +
                                    ". Actual value is " + actualValue + ".");
                            throw new WrongNodeValue(sipId, validationProfileId, expectedValue, actualValue,
                                    file.getPath(), expression);
                        }
                    } else {
                        //compare with regex
                        Node regexElement = element.getElementsByTagName("regex").item(0);
                        String regex = regexElement.getTextContent();
                        Pattern pattern = Pattern.compile(regex);
                        Matcher m = pattern.matcher(actualValue);
                        if (!m.matches()) {
                            log.debug("Validation of SIP " + sipId + " with profile " + validationProfileId +
                                    " failed. Value " + actualValue + " of node at " + "path \"" + expression +
                                    "\" does not match regex " + regex + ".");
                            throw new InvalidSipNodeValue(sipId, validationProfileId, regex, actualValue,
                                    file.getPath(), expression);
                        }
                    }
                }
            }
        }
    }

    @Inject
    public void setValidationProfileStore(ValidationProfileStore validationProfileStore) {
        this.validationProfileStore = validationProfileStore;
    }
}
