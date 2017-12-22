package cz.inqool.uas.sign;

import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.inqool.uas.file.FileRef;
import cz.inqool.uas.file.FileRefStore;
import cz.inqool.uas.file.FileRepository;
import cz.inqool.uas.sign.dto.MessageDigest;
import cz.inqool.uas.util.Utils;
import eu.europa.esig.dss.DSSUtils;
import eu.europa.esig.dss.DigestAlgorithm;
import eu.europa.esig.dss.SignatureAlgorithm;
import eu.europa.esig.dss.x509.CertificateToken;
import helper.DbTest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;

import javax.persistence.EntityManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

import static cz.inqool.uas.util.Utils.resource;
import static cz.inqool.uas.util.Utils.resourceBytes;

@Slf4j
public class PdfSignerTest extends DbTest {

    private FileRepository repository;

    private PdfSigner signer;

    @Mock
    private ElasticsearchTemplate template;

    private InternalSigner internalSigner;

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);

        internalSigner = new InternalSigner();

        EntityManager entityManager = getEm();

        FileRefStore store = new FileRefStore();
        store.setEntityManager(entityManager);
        store.setQueryFactory(new JPAQueryFactory(entityManager));
        store.setTemplate(template);

        Files.createDirectories(Paths.get("testfiles"));

        repository = new FileRepository();
        repository.setStore(store);
        repository.setBasePath("testfiles");

        DSSProvider dssProvider = new DSSProvider();
        signer = new PdfSigner();
        signer.setRepository(repository);
        signer.setpAdESService(dssProvider.pAdESService());
        signer.setDigestAlgorithm(DigestAlgorithm.SHA256);
        signer.setSuffix("Podepsany");
    }

    @After
    public void tearDown() throws IOException {
        Path testfiles = Paths.get("testfiles");

        if (Files.isDirectory(testfiles)) {
            Files.walkFileTree(testfiles, new SimpleFileVisitor<Path>() {
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }
            });

            Files.delete(testfiles);
        }
    }

    @Test
    public void serverSign() throws IOException, GeneralSecurityException {
        FileRef pdfFile;
        try (InputStream stream = resource("cz/inqool/uas/sign/test.pdf")) {
            pdfFile = repository.create(stream, "test.pdf", "application/pdf", false);
        }

        byte[] cert = resourceBytes("cz/inqool/uas/sign/cert.pem");
        String certBase64 = Base64.encodeBase64String(cert);

        String key = Utils.resourceString("cz/inqool/uas/sign/key8.pem");

        FileRef ref = signer.serverSign(pdfFile.getId(), certBase64, key);

        File targetFile = new File("test.pdf");

        FileRef tmp = repository.get(ref.getId());

        FileUtils.copyInputStreamToFile(tmp.getStream(), targetFile);
    }

    @Test
    public void prepareTest() throws IOException, GeneralSecurityException {
        FileRef pdfFile;
        try (InputStream stream = resource("cz/inqool/uas/sign/test.pdf")) {
            pdfFile = repository.create(stream, "test.pdf", "application/pdf", false);
        }

        byte[] cert = resourceBytes("cz/inqool/uas/sign/cert.pem");
        String certBase64 = Base64.encodeBase64String(cert);

        String key = Utils.resourceString("cz/inqool/uas/sign/key8.pem");

        MessageDigest digest = signer.prepare(pdfFile.getId(), certBase64);

        String signedMessage = internalSigner.sign(cert, key, digest.getMessage());

        FileRef signedDocument = signer.finalize(pdfFile.getId(), new cz.inqool.uas.sign.dto.Signature(
                digest.getCreated(), signedMessage, certBase64
        ));
        System.err.println(signedDocument.getId());
    }

    private class InternalSigner {
        public String sign(byte[] cert, String keyString, String message) throws IOException, GeneralSecurityException {
            CertificateToken token = DSSUtils.loadCertificate(cert);
            RSAPrivateKey key = getPrivateKeyFromString(keyString);

            SignatureAlgorithm algo = SignatureAlgorithm.getAlgorithm(token.getEncryptionAlgorithm(), DigestAlgorithm.SHA256);

            final Signature signature = Signature.getInstance(algo.getJCEId());
            signature.initSign(key);
            signature.update(Base64.decodeBase64(message));
            final byte[] signatureValue = signature.sign();

            return Base64.encodeBase64String(signatureValue);
        }

        protected RSAPrivateKey getPrivateKeyFromString(String key) throws IOException, GeneralSecurityException {
            String privateKeyPEM = key;
            privateKeyPEM = privateKeyPEM.replace("-----BEGIN PRIVATE KEY-----\n", "");
            privateKeyPEM = privateKeyPEM.replace("-----END PRIVATE KEY-----", "");
            byte[] encoded = Base64.decodeBase64(privateKeyPEM);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            return (RSAPrivateKey) kf.generatePrivate(keySpec);
        }
    }
}
