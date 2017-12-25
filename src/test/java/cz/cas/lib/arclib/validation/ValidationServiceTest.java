package cz.cas.lib.arclib.validation;

import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.cas.lib.arclib.domain.ValidationProfile;
import cz.cas.lib.arclib.store.ValidationProfileStore;
import cz.cas.lib.arclib.validation.exception.InvalidNodeValue;
import cz.cas.lib.arclib.validation.exception.MissingFile;
import cz.cas.lib.arclib.validation.exception.SchemaValidationError;
import cz.cas.lib.arclib.validation.exception.WrongNodeValue;
import cz.inqool.uas.exception.MissingObject;
import helper.DbTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static helper.ThrowableAssertion.assertThrown;

public class ValidationServiceTest extends DbTest {

    private ValidationService service;

    private ValidationProfileStore store;

    private static final String SIP_ID = "KPW01169310";
    private static final String SIP_PATH = "SIP_packages/" + SIP_ID;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        store = new ValidationProfileStore();
        store.setEntityManager(getEm());
        store.setQueryFactory(new JPAQueryFactory(getEm()));

        service = new ValidationService();
        service.setValidationProfileStore(store);
    }

    @Test
    public void validateSipMixedChecksSuccess() throws ParserConfigurationException, SAXException, XPathExpressionException, IOException {
        InputStream inputStream = getClass().getResourceAsStream("/validation/validationProfileMixedChecks.xml");
        String xml = readFromInputStream(inputStream);

        ValidationProfile validationProfile = new ValidationProfile();
        validationProfile.setXml(xml);

        store.save(validationProfile);
        flushCache();

        service.validateSip(SIP_ID, SIP_PATH, validationProfile.getId());
    }

    @Test
    public void validateSipProfileMissing() {
        assertThrown(() -> service.validateSip(SIP_ID, SIP_PATH, "nonExistentId")).isInstanceOf(MissingObject.class);
    }

    @Test
    public void validateSipFileExistenceChecksSuccess() throws ParserConfigurationException, SAXException, XPathExpressionException, IOException {
        InputStream inputStream = getClass().getResourceAsStream("/validation/validationProfileFileExistenceChecks.xml");
        String xml = readFromInputStream(inputStream);

        ValidationProfile validationProfile = new ValidationProfile();
        validationProfile.setXml(xml);

        store.save(validationProfile);
        flushCache();

        service.validateSip(SIP_ID, SIP_PATH, validationProfile.getId());
    }

    @Test
    public void validateSipFileExistenceCheckMissingFile() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/validation/validationProfileMissingFile.xml");
        String xml = readFromInputStream(inputStream);

        ValidationProfile validationProfile = new ValidationProfile();
        validationProfile.setXml(xml);

        store.save(validationProfile);
        flushCache();

        assertThrown(() -> service.validateSip(SIP_ID, SIP_PATH, validationProfile.getId())).isInstanceOf(MissingFile.class);
    }

    @Test
    public void validateSipValidationSchemaChecksSuccess() throws ParserConfigurationException, SAXException, XPathExpressionException,
            IOException {
        InputStream inputStream = getClass().getResourceAsStream("/validation/validationProfileValidationSchemaChecks.xml");
        String xml = readFromInputStream(inputStream);

        ValidationProfile validationProfile = new ValidationProfile();
        validationProfile.setXml(xml);

        store.save(validationProfile);
        flushCache();

        service.validateSip(SIP_ID, SIP_PATH, validationProfile.getId());
    }

    @Test
    public void validateSipValidationSchemaCheckFailure() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/validation/validationProfileInvalidSchema.xml");
        String xml = readFromInputStream(inputStream);

        ValidationProfile validationProfile = new ValidationProfile();
        validationProfile.setXml(xml);

        store.save(validationProfile);
        flushCache();

        assertThrown(() -> service.validateSip(SIP_ID, SIP_PATH, validationProfile.getId())).isInstanceOf(SchemaValidationError.class);
    }

    @Test
    public void validateSipNodeValueChecksSuccess() throws ParserConfigurationException, SAXException, XPathExpressionException,
            IOException {
        InputStream inputStream = getClass().getResourceAsStream("/validation/validationProfileNodeValueChecks.xml");
        String xml = readFromInputStream(inputStream);

        ValidationProfile validationProfile = new ValidationProfile();
        validationProfile.setXml(xml);

        store.save(validationProfile);
        flushCache();

        service.validateSip(SIP_ID, SIP_PATH, validationProfile.getId());
    }

    @Test
    public void validateSipWrongNodeValue() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/validation/validationProfileWrongNodeValue.xml");
        String xml = readFromInputStream(inputStream);

        ValidationProfile validationProfile = new ValidationProfile();
        validationProfile.setXml(xml);

        store.save(validationProfile);
        flushCache();

        assertThrown(() -> service.validateSip(SIP_ID, SIP_PATH, validationProfile.getId())).isInstanceOf(WrongNodeValue.class);
    }

    @Test
    public void validateSipInvalidNodeValue() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/validation/validationProfileInvalidNodeValue.xml");
        String xml = readFromInputStream(inputStream);

        ValidationProfile validationProfile = new ValidationProfile();
        validationProfile.setXml(xml);

        store.save(validationProfile);
        flushCache();

        assertThrown(() -> service.validateSip(SIP_ID, SIP_PATH, validationProfile.getId())).isInstanceOf(InvalidNodeValue.class);
    }

    private String readFromInputStream(InputStream inputStream) throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }
}
