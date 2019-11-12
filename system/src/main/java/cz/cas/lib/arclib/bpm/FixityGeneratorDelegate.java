package cz.cas.lib.arclib.bpm;

import cz.cas.lib.arclib.domain.IngestToolFunction;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestEvent;
import cz.cas.lib.arclib.service.fixity.Crc32Counter;
import cz.cas.lib.arclib.service.fixity.Md5Counter;
import cz.cas.lib.arclib.service.fixity.Sha512Counter;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static cz.cas.lib.arclib.bpm.BpmConstants.*;
import static cz.cas.lib.core.util.Utils.bytesToHexString;

@Slf4j
@Service
public class FixityGeneratorDelegate extends ArclibDelegate {

    private Md5Counter md5Counter;
    private Crc32Counter crc32Counter;
    private Sha512Counter sha512Counter;
    @Getter
    private String toolName="ARCLib_"+ IngestToolFunction.message_digest_calculation;

    /**
     * Computes 3 types of fixity for whole SIP (CRC32,SHA512,MD5)
     */
    @Override
    public void executeArclibDelegate(DelegateExecution execution) {
        String ingestWorkflowExternalId = getIngestWorkflowExternalId(execution);
        log.debug("Execution of Fixity generator delegate started for ingest workflow " + ingestWorkflowExternalId + ".");
        Path sipZipPath = getSipZipPath(execution);
        try {
            String md5 = bytesToHexString(md5Counter.computeDigest(sipZipPath));
            String sha512 = bytesToHexString(sha512Counter.computeDigest(sipZipPath));
            String crc32 = bytesToHexString(crc32Counter.computeDigest(sipZipPath));

            IngestEvent fixityGenerationEvent = new IngestEvent(ingestWorkflowStore.findByExternalId(ingestWorkflowExternalId),
                    toolService.findByNameAndVersion(getToolName(), getToolVersion()), true, null);
            ingestEventStore.save(fixityGenerationEvent);

            Map<String, String> mapOfEventIdsToMd5Calculations = (Map<String, String>)
                    execution.getVariable(MessageDigestCalculation.mapOfEventIdsToMd5Calculations);
            Map<String, String> mapOfEventIdsToSha512Calculations = (Map<String, String>)
                    execution.getVariable(MessageDigestCalculation.mapOfEventIdsToSha512Calculations);
            Map<String, String> mapOfEventIdsToCrc32Calculations = (Map<String, String>)
                    execution.getVariable(MessageDigestCalculation.mapOfEventIdsToCrc32Calculations);

            mapOfEventIdsToMd5Calculations.put(fixityGenerationEvent.getId(), md5);
            mapOfEventIdsToSha512Calculations.put(fixityGenerationEvent.getId(), sha512);
            mapOfEventIdsToCrc32Calculations.put(fixityGenerationEvent.getId(), crc32);

            execution.setVariable(MessageDigestCalculation.mapOfEventIdsToMd5Calculations, mapOfEventIdsToMd5Calculations);
            execution.setVariable(MessageDigestCalculation.mapOfEventIdsToCrc32Calculations, mapOfEventIdsToCrc32Calculations);
            execution.setVariable(MessageDigestCalculation.mapOfEventIdsToSha512Calculations, mapOfEventIdsToSha512Calculations);
            if (mapOfEventIdsToMd5Calculations.size() == 1) {
                execution.setVariable(MessageDigestCalculation.preferredMessageDigestCalculationEventId, fixityGenerationEvent.getId());
            }
        } catch (IOException e) {
            throw new GeneralException("error occurred during computation of checksum: " + e.getMessage());
        }
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
