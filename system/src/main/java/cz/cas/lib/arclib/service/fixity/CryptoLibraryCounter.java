package cz.cas.lib.arclib.service.fixity;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public abstract class CryptoLibraryCounter extends FixityCounter {
    /**
     * Computes digest from a file using cryptographic library.
     *
     * @param fileStream Stream of file which digest has to be computed.
     * @return byte array with computed digest
     * @throws IOException
     */
    @Override
    public byte[] computeDigest(InputStream fileStream) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(fileStream)) {
            byte[] buffer = new byte[1024];
            MessageDigest complete = MessageDigest.getInstance(getType());
            int numRead;
            do {
                numRead = bis.read(buffer);
                if (numRead > 0) {
                    complete.update(buffer, 0, numRead);
                }
            } while (numRead != -1);
            return complete.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public abstract String getType();
}
