package cz.cas.lib.arclib.service.fixity;

import cz.cas.lib.arclib.domain.HashType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FixityCounterFacade {

    @Getter
    private Map<HashType, FixityCounter> fixityCounters;

    public byte[] computeDigest(HashType hashType, InputStream fileStream) throws IOException {
        return fixityCounters.get(hashType).computeDigest(fileStream);
    }

    public byte[] computeDigest(HashType hashType, Path pathToFile) throws IOException {
        return fixityCounters.get(hashType).computeDigest(pathToFile);
    }

    public boolean verifyFixity(HashType hashType, InputStream fileStream, String expectedDigest) throws IOException {
        return fixityCounters.get(hashType).verifyFixity(fileStream, expectedDigest);
    }

    @Inject
    public void setFixityCheckers(List<FixityCounter> fixityCounters) {
        this.fixityCounters = fixityCounters.stream().collect(Collectors.toMap(f -> f.getHashType(), f -> f));
    }
}
