package cz.cas.lib.arclib.fixity;

import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static cz.inqool.uas.util.Utils.notNull;
import static cz.inqool.uas.util.Utils.bytesToHexString;

@Slf4j
public abstract class FixityCounter {

    /**
     * Abstract method to compute digest from a file. The type of digest depends on subclass implementation. Eg. MD5, CRC32, SHA-512 etc.
     *
     * @param fileStream Stream of file which digest has to be computed.
     * @return byte array with computed digest
     * @throws IOException
     */
    public abstract byte[] computeDigest(InputStream fileStream) throws IOException;

    /**
     * Computes digest from a file. The type of digest depends on subclass implementation. Eg. MD5, CRC32, SHA-512 etc.
     *
     * @param pathToFile Path to file which digest has to be computed.
     * @return byte array with computed digest
     * @throws IOException
     */
    public byte[] computeDigest(Path pathToFile) throws IOException {
        notNull(pathToFile, () -> {
            throw new IllegalArgumentException();
        });
        try (FileInputStream is = new FileInputStream(pathToFile.toAbsolutePath().toString())) {
            return computeDigest(is);
        }
    }

    /**
     * Computes digest for specified file and compares it with provided digest. The type of computed digest depends on subclass instance.
     *
     * @param pathToFile     Path to file which digest has to be computed.
     * @param expectedDigest Digest provided for comparison.
     * @return true if digests matches, false otherwise
     * @throws IOException
     */
    public boolean verifyFixity(Path pathToFile, String expectedDigest) throws IOException {
        log.info("verifying fixity of file with path: " + pathToFile);
        notNull(pathToFile, () -> {
            throw new IllegalArgumentException();
        });
        notNull(expectedDigest, () -> {
            throw new IllegalArgumentException();
        });
        return compare(expectedDigest, computeDigest(pathToFile));
    }

    /**
     * Computes digest for specified file and compares it with provided digest. The type of computed digest depends on subclass instance.
     *
     * @param fileStream     Stream of file which digest has to be computed.
     * @param expectedDigest Digest provided for comparison.
     * @return true if digests matches, false otherwise
     * @throws IOException
     */
    public boolean verifyFixity(InputStream fileStream, String expectedDigest) throws IOException {
        notNull(fileStream, () -> {
            throw new IllegalArgumentException();
        });
        notNull(expectedDigest, () -> {
            throw new IllegalArgumentException();
        });
        return compare(expectedDigest, computeDigest(fileStream));
    }

    private boolean compare(String expectedDigest, byte[] computedDigest) {
        String computedDigestStr = bytesToHexString(computedDigest);
        log.info("expected digest: " + expectedDigest);
        log.info("computed digest: " + computedDigestStr);
        expectedDigest = expectedDigest.toLowerCase();
        boolean matches = computedDigestStr.equals(expectedDigest);
        log.info("digests matches: " + matches);
        return matches;
    }
}