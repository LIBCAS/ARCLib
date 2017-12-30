package cz.inqool.uas.sign;

import cz.inqool.uas.file.FileRef;
import cz.inqool.uas.file.FileRepository;
import cz.inqool.uas.sign.dto.MessageDigest;
import cz.inqool.uas.sign.dto.Signature;
import eu.europa.esig.dss.*;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.x509.CertificateToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@ConditionalOnProperty(prefix = "sign", name = "enabled", havingValue = "true")
@Service
@Slf4j
public class PdfSigner {
    private FileRepository repository;

    private PAdESService pAdESService;

    private DigestAlgorithm digestAlgorithm;

    private String suffix;

    FileRef serverSign(String fileId, String certificate, String key) throws IOException, GeneralSecurityException {
        byte[] certBytes = Base64.getDecoder().decode(certificate);

        PAdESSignatureParameters parameters = getParameters(certificate);

        FileRef file = repository.get(fileId);

        try (InputStream stream = file.getStream()) {
            DSSDocument document = new InMemoryDocument(stream);

            ToBeSigned toSign = pAdESService.getDataToSign(document, parameters);
            String message = Base64.getEncoder().encodeToString(toSign.getBytes());

            String signedMessage = sign(certBytes, key, message);

            SignatureAlgorithm algorithm = SignatureAlgorithm.getAlgorithm(parameters.getEncryptionAlgorithm(), digestAlgorithm);
            SignatureValue signatureValue = new SignatureValue(algorithm, Base64.getDecoder().decode(signedMessage));

            DSSDocument signedDocument = pAdESService.signDocument(document, parameters, signatureValue);

            try (InputStream signedStream = signedDocument.openStream()) {
                return repository.create(signedStream, generateSignedFileName(file.getName()), file.getContentType(), file.getIndexedContent());
            }
        }
    }

    public MessageDigest prepare(String fileId, String certificate) throws IOException {
        PAdESSignatureParameters parameters = getParameters(certificate);

        FileRef file = repository.get(fileId);
        try (InputStream stream = file.getStream()) {
            DSSDocument document = new InMemoryDocument(stream);

            ToBeSigned toSign = pAdESService.getDataToSign(document, parameters);
            String message = Base64.getEncoder().encodeToString(toSign.getBytes());

            return new MessageDigest(parameters.getBLevelParams().getSigningDate().toInstant(),
                    message, parameters.getEncryptionAlgorithm());
        }
    }

    public FileRef finalize(String fileId, Signature signature) throws IOException {

        PAdESSignatureParameters parameters = getParameters(signature.getCertificate(), signature.getCreated());

        FileRef file = repository.get(fileId);
        try (InputStream stream = file.getStream()) {
            DSSDocument document = new InMemoryDocument(stream);

            SignatureAlgorithm algorithm = SignatureAlgorithm.getAlgorithm(parameters.getEncryptionAlgorithm(), digestAlgorithm);
            SignatureValue signatureValue = new SignatureValue(algorithm, Base64.getDecoder().decode(signature.getSignature()));

            DSSDocument signedDocument = pAdESService.signDocument(document, parameters, signatureValue);

            try (InputStream signedStream = signedDocument.openStream()) {
                return repository.create(signedStream, generateSignedFileName(file.getName()), file.getContentType(), file.getIndexedContent());
            }
        }
    }

    /**
     * Private method to set up parameters of signature
     *
     * @param certificateBase64 certificate string encoded in base64
     * @return PAdESSignatureParameters parameters
     */
    private PAdESSignatureParameters getParameters(String certificateBase64) {
        byte[] certificate = Base64.getDecoder().decode(certificateBase64);

        CertificateToken token = DSSUtils.loadCertificate(certificate);

        PAdESSignatureParameters parameters = new PAdESSignatureParameters();
        parameters.setSignatureLevel(SignatureLevel.PAdES_BASELINE_B);
        parameters.setSignaturePackaging(SignaturePackaging.ENVELOPED);
        parameters.setDigestAlgorithm(digestAlgorithm);
        parameters.setEncryptionAlgorithm(token.getEncryptionAlgorithm());
        parameters.setSigningCertificate(token);
        parameters.bLevel().setSigningDate(Date.from(Instant.now()));

        return parameters;
    }

    private PAdESSignatureParameters getParameters(String certificateBase64, Instant created) {
        byte[] certificate = Base64.getDecoder().decode(certificateBase64);

        CertificateToken token = DSSUtils.loadCertificate(certificate);

        PAdESSignatureParameters parameters = new PAdESSignatureParameters();
        parameters.setSignatureLevel(SignatureLevel.PAdES_BASELINE_B);
        parameters.setSignaturePackaging(SignaturePackaging.ENVELOPED);
        parameters.setDigestAlgorithm(digestAlgorithm);
        parameters.setEncryptionAlgorithm(token.getEncryptionAlgorithm());
        parameters.setSigningCertificate(token);
        parameters.bLevel().setSigningDate(Date.from(created));

        return parameters;
    }

    /**
     * Private method for proper file name generating
     *
     * @param name name of the file
     * @return name of the file as string
     */
    private String generateSignedFileName(String name) {
        int dotIndex = name.lastIndexOf(".");

        if (dotIndex != -1) {
            return name.substring(0, dotIndex) + suffix + name.substring(dotIndex);
        } else {
            return name + suffix;
        }
    }

    /**
     * Private method for parsing private key from string loaded from file, stolen from test
     * compatibility problems on Windows "-----BEGIN PRIVATE KEY-----" vs. "-----BEGIN PRIVATE KEY-----\n" on Unix systems
     *
     * @param key key loaded as string from file
     * @return RSAPrivate key parsed from string
     * @throws GeneralSecurityException if method was unable to parse key properly
     */
    private RSAPrivateKey getPrivateKeyFromString(String key) throws GeneralSecurityException {
        String privateKeyPEM = key;
        privateKeyPEM = privateKeyPEM.replace("-----BEGIN PRIVATE KEY-----", "");
        privateKeyPEM = privateKeyPEM.replace("-----END PRIVATE KEY-----", "");

        byte[] encoded = org.apache.commons.codec.binary.Base64.decodeBase64(privateKeyPEM);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        return (RSAPrivateKey) kf.generatePrivate(keySpec);
    }

    /**
     * Private method which do the real signing
     *
     * @param cert      byte array with certificate
     * @param keyString string with private key
     * @param message   message to be signed
     * @return signed message as string
     * @throws GeneralSecurityException if there were some security problem
     */
    private String sign(byte[] cert, String keyString, String message) throws GeneralSecurityException {
        CertificateToken token = DSSUtils.loadCertificate(cert);
        RSAPrivateKey key = getPrivateKeyFromString(keyString);

        SignatureAlgorithm algo = SignatureAlgorithm.getAlgorithm(token.getEncryptionAlgorithm(), DigestAlgorithm.SHA256);

        final java.security.Signature signature = java.security.Signature.getInstance(algo.getJCEId());
        signature.initSign(key);
        signature.update(org.apache.commons.codec.binary.Base64.decodeBase64(message));
        final byte[] signatureValue = signature.sign();

        return org.apache.commons.codec.binary.Base64.encodeBase64String(signatureValue);
    }

    @Inject
    public void setDigestAlgorithm(@Value("${sign.digest.algorithm}") DigestAlgorithm digestAlgorithm) {
        this.digestAlgorithm = digestAlgorithm;
    }

    @Inject
    public void setSuffix(@Value("${sign.suffix}") String suffix) {
        this.suffix = suffix;
    }

    @Inject
    public void setRepository(FileRepository repository) {
        this.repository = repository;
    }

    @Inject
    public void setpAdESService(PAdESService pAdESService) {
        this.pAdESService = pAdESService;
    }
}
