package cz.cas.lib.arclib.bpm;

import cz.cas.lib.arclib.service.fixity.Crc32Counter;
import cz.cas.lib.arclib.service.fixity.Md5Counter;
import cz.cas.lib.arclib.service.fixity.Sha512Counter;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static cz.cas.lib.core.util.Utils.bytesToHexString;

@Slf4j
@Service
public class FixityGeneratorDelegate extends ArclibDelegate implements JavaDelegate {

    private Md5Counter md5Counter;
    private Crc32Counter crc32Counter;
    private Sha512Counter sha512Counter;

    /**
     * Computes 3 types of fixity for whole SIP (CRC32,SHA512,MD5)
     */
    @Override
    public void execute(DelegateExecution execution) {
        String ingestWorkflowExternalId = getIngestWorkflowExternalId(execution);
        log.info("Execution of Fixity generator delegate started for ingest workflow " + ingestWorkflowExternalId + ".");
        Path sipZipPath = getSipZipPath(execution);
        try {
            execution.setVariable(BpmConstants.MessageDigestCalculation.checksumMd5, bytesToHexString(md5Counter.computeDigest(sipZipPath)));
            execution.setVariable(BpmConstants.MessageDigestCalculation.checksumSha512, bytesToHexString(sha512Counter.computeDigest(sipZipPath)));
            execution.setVariable(BpmConstants.MessageDigestCalculation.checksumCrc32, bytesToHexString(crc32Counter.computeDigest(sipZipPath)));
        } catch (IOException e) {
            log.error("error occurred during computation of checksum: " + e.getMessage());
        }
        execution.setVariable(BpmConstants.MessageDigestCalculation.success, true);
        execution.setVariable(BpmConstants.MessageDigestCalculation.dateTime,
                Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
        log.info("Execution of Fixity generator delegate finished for ingest workflow " + ingestWorkflowExternalId + ".");
    }

    @Inject
    public void setMd5Counter(Md5Counter md5Counter) {
        this.md5Counter = md5Counter;
    }

    @Inject
    public void setCrc32Counter(Crc32Counter crc32Counter) {
        this.crc32Counter = crc32Counter;
    }

    @Inject
    public void setSha512Counter(Sha512Counter sha512Counter) {
        this.sha512Counter = sha512Counter;
    }
}
