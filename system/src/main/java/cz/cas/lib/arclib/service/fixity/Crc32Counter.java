package cz.cas.lib.arclib.service.fixity;


import cz.cas.lib.arclib.domain.HashType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

@Slf4j
@Service
public class Crc32Counter extends FixityCounter {

    @Getter
    private final HashType hashType = HashType.Crc32;

    @Override
    public byte[] computeDigest(InputStream fileStream) throws IOException {
        Checksum checksum = new CRC32();
        try (BufferedInputStream bis = new BufferedInputStream(fileStream)) {
            byte[] buffer = new byte[1024];
            int numRead;
            do {
                numRead = bis.read(buffer);
                if (numRead > 0) {
                    checksum.update(buffer, 0, numRead);
                }
            } while (numRead != -1);
            String s = Long.toHexString(checksum.getValue());
            if (s.length() % 2 != 0)
                s = "0" + s;
            return Hex.decode(s);
        }
    }
}
