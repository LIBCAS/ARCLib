package cz.cas.lib.arclib.service.fixity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class Sha256Counter extends CryptoLibraryCounter {
    @Override
    public String getType() {
        return "SHA-256";
    }
}
