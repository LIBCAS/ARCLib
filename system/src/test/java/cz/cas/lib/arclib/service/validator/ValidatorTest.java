package cz.cas.lib.arclib.service.validator;

import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.cas.lib.arclib.domain.profiles.ValidationProfile;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.exception.validation.InvalidSipNodeValue;
import cz.cas.lib.arclib.exception.validation.MissingFile;
import cz.cas.lib.arclib.exception.validation.SchemaValidationError;
import cz.cas.lib.arclib.exception.validation.WrongNodeValue;
import cz.cas.lib.arclib.store.ValidationProfileStore;
import cz.cas.lib.core.sequence.Generator;
import helper.DbTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import static helper.ThrowableAssertion.assertThrown;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ValidatorTest extends DbTest {

    private Validator service;

    private ValidationProfileStore store;
    @Mock
    private Generator generator;

    private static final String INGEST_WORKFLOW_ID = "7033d800-0935-11e4-beed-5ef3fc9ae867";
    public static final Path SIP_PATH = Paths.get("src/test/resources/SIP_package").resolve(INGEST_WORKFLOW_ID);
    public static final String SIP_ID = "d523cb06-6e3e-407f-9054-0ee9b191b927";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        store = new ValidationProfileStore();
        store.setEntityManager(getEm());
        store.setQueryFactory(new JPAQueryFactory(getEm()));
        store.setGenerator(generator);
        when(generator.generate(any())).thenReturn(String.valueOf(new Random().nextInt()));

        service = new Validator();
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

        service.validateSip(SIP_ID, SIP_PATH.toString(), validationProfile.getExternalId());
    }

    @Test
    public void validateSipProfileMissing() {
        assertThrown(() -> service.validateSip(SIP_ID, SIP_PATH.toString(), "nonExistentId")).isInstanceOf(MissingObject.class);
    }

    @Test
    public void validateSipFileExistenceChecksSuccess() throws ParserConfigurationException, SAXException, XPathExpressionException, IOException {
        InputStream inputStream = getClass().getResourceAsStream("/validation/validationProfileFileExistenceChecks.xml");
        String xml = readFromInputStream(inputStream);

        ValidationProfile validationProfile = new ValidationProfile();
        validationProfile.setXml(xml);

        store.save(validationProfile);
        flushCache();

        service.validateSip(SIP_ID, SIP_PATH.toString(), validationProfile.getExternalId());
    }

    @Test
    public void validateSipFileExistenceCheckMissingFile() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/validation/validationProfileMissingFile.xml");
        String xml = readFromInputStream(inputStream);

        ValidationProfile validationProfile = new ValidationProfile();
        validationProfile.setXml(xml);

        store.save(validationProfile);
        flushCache();

        assertThrown(() -> service.validateSip(SIP_ID, SIP_PATH.toString(), validationProfile.getExternalId())).isInstanceOf(MissingFile.class);
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

        service.validateSip(SIP_ID, SIP_PATH.toString(), validationProfile.getExternalId());
    }

    @Test
    public void validateSipValidationSchemaCheckFailure() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/validation/validationProfileInvalidSchema.xml");
        String xml = readFromInputStream(inputStream);

        ValidationProfile validationProfile = new ValidationProfile();
        validationProfile.setXml(xml);

        store.save(validationProfile);
        flushCache();

        assertThrown(() -> service.validateSip(SIP_ID, SIP_PATH.toString(), validationProfile.getExternalId())).isInstanceOf(SchemaValidationError.class);
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

        service.validateSip(SIP_ID, SIP_PATH.toString(), validationProfile.getExternalId());
    }

    @Test
    public void validateSipWrongNodeValue() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/validation/validationProfileWrongNodeValue.xml");
        String xml = readFromInputStream(inputStream);

        ValidationProfile validationProfile = new ValidationProfile();
        validationProfile.setXml(xml);

        store.save(validationProfile);
        flushCache();

        assertThrown(() -> service.validateSip(SIP_ID, SIP_PATH.toString(), validationProfile.getExternalId())).isInstanceOf(WrongNodeValue.class);
    }

    @Test
    public void validateSipInvalidNodeValue() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/validation/validationProfileInvalidNodeValue.xml");
        String xml = readFromInputStream(inputStream);

        ValidationProfile validationProfile = new ValidationProfile();
        validationProfile.setXml(xml);

        store.save(validationProfile);
        flushCache();

        assertThrown(() -> service.validateSip(SIP_ID, SIP_PATH.toString(), validationProfile.getExternalId())).isInstanceOf(InvalidSipNodeValue.class);
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
