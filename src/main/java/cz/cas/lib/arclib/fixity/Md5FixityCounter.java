package cz.cas.lib.arclib.fixity;

import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class Md5FixityCounter extends FixityCounter {

    /**
     * Computes MD5 digest from a file.
     *
     * @param fileStream Stream of file which digest has to be computed.
     * @return byte array with computed digest
     * @throws IOException
     */
    @Override
    public byte[] computeDigest(InputStream fileStream) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(fileStream)) {
            byte[] buffer = new byte[1024];
            MessageDigest complete = MessageDigest.getInstance("MD5");
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
}
