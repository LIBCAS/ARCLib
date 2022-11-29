package cz.cas.lib.arclib.service.fixity;

import cz.cas.lib.arclib.domain.HashType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class Sha1Counter extends CryptoLibraryCounter {

    @Getter
    private final HashType hashType = HashType.Sha1;

    @Getter
    public final String messageDigestType = "SHA-1";
}
