package cz.cas.lib.arclib.service.fixity;

import cz.cas.lib.arclib.domain.HashType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class Md5Counter extends CryptoLibraryCounter {

    @Getter
    private final HashType hashType = HashType.MD5;

    @Getter
    public final String messageDigestType = "MD5";
}
