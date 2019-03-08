package cz.cas.lib.arclib.bpm;

import cz.cas.lib.arclib.domain.IngestToolFunction;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestEvent;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.service.fixity.Crc32Counter;
import cz.cas.lib.arclib.service.fixity.Md5Counter;
import cz.cas.lib.arclib.service.fixity.Sha512Counter;
import cz.cas.lib.core.exception.GeneralException;
import cz.cas.lib.core.store.Transactional;
import lombok.Getter;
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
    @Getter
    private String toolName="ARCLib_"+ IngestToolFunction.message_digest_calculation;

    /**
     * Computes 3 types of fixity for whole SIP (CRC32,SHA512,MD5)
     */
    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        String ingestWorkflowExternalId = getIngestWorkflowExternalId(execution);
        log.debug("Execution of Fixity generator delegate started for ingest workflow " + ingestWorkflowExternalId + ".");
        Path sipZipPath = getSipZipPath(execution);
        try {
            execution.setVariable(BpmConstants.MessageDigestCalculation.checksumMd5, bytesToHexString(md5Counter.computeDigest(sipZipPath)));
            execution.setVariable(BpmConstants.MessageDigestCalculation.checksumSha512, bytesToHexString(sha512Counter.computeDigest(sipZipPath)));
            execution.setVariable(BpmConstants.MessageDigestCalculation.checksumCrc32, bytesToHexString(crc32Counter.computeDigest(sipZipPath)));
        } catch (IOException e) {
            throw new GeneralException("error occurred during computation of checksum: " + e.getMessage());
        }
        execution.setVariable(BpmConstants.MessageDigestCalculation.success, true);
        ingestEventStore.save(new IngestEvent(ingestWorkflowStore.findByExternalId(ingestWorkflowExternalId), toolService.findByNameAndVersion(getToolName(), getToolVersion()), true, null));
        log.debug("Execution of Fixity generator delegate finished for ingest workflow " + ingestWorkflowExternalId + ".");
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
