package cz.cas.lib.arclib.service.fixity;

import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static cz.cas.lib.core.util.Utils.bytesToHexString;
import static cz.cas.lib.core.util.Utils.notNull;

/**
 * Parent class for fixity counters which computation results in byte array which is then stored and represented as a hexadecimal string.
 */
@Slf4j
public abstract class FixityCounter {
    /**
     * Abstract method to compute digest from a file. The type of digest depends on subclass implementation eg. MD5, SHA-512...
     *
     * @param fileStream Stream of file which digest has to be computed.
     * @return byte array with computed digest
     * @throws IOException
     */
    public abstract byte[] computeDigest(InputStream fileStream) throws IOException;

    /**
     * Computes digest from a file. The type of digest depends on subclass implementation eg. MD5, SHA-512...
     *
     * @param pathToFile Path to file which digest has to be computed.
     * @return byte array with computed digest
     * @throws IOException
     */
    public byte[] computeDigest(Path pathToFile) throws IOException {
        notNull(pathToFile, () -> {
            throw new IllegalArgumentException();
        });
        log.debug(getClass().getSimpleName() + " computing checksum");
        if (pathToFile.toFile().isDirectory()) {
            throw new IllegalArgumentException("trying to compute digest on a folder");
        }
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
        log.debug(getClass().getSimpleName() + ": verifying fixity of file with path: " + pathToFile);
        notNull(pathToFile, () -> {
            throw new IllegalArgumentException();
        });
        notNull(expectedDigest, () -> {
            throw new IllegalArgumentException();
        });
        return checkIfDigestsMatches(expectedDigest, computeDigest(pathToFile));
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
        return checkIfDigestsMatches(expectedDigest, computeDigest(fileStream));
    }

    public boolean checkIfDigestsMatches(String expectedDigest, byte[] computedDigest) {
        String computedDigestStr = bytesToHexString(computedDigest);
        log.debug("expected digest: " + expectedDigest);
        log.debug("computed digest: " + computedDigestStr);
        expectedDigest = expectedDigest.toLowerCase();
        boolean matches = computedDigestStr.equals(expectedDigest);
        log.debug("digests matches: " + matches);
        return matches;
    }
}
