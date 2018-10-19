package cz.cas.lib.arclib.service.fixity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class Md5Counter extends CryptoLibraryCounter {

    @Override
    public String getType() {
        return "MD5";
    }
}
