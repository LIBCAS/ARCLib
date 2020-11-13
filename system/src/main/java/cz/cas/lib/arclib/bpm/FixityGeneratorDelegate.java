package cz.cas.lib.arclib.bpm;

import cz.cas.lib.arclib.domain.IngestToolFunction;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestEvent;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.service.fixity.Crc32Counter;
import cz.cas.lib.arclib.service.fixity.Md5Counter;
import cz.cas.lib.arclib.service.fixity.MetsChecksumType;
import cz.cas.lib.arclib.service.fixity.Sha512Counter;
import cz.cas.lib.arclib.utils.ArclibUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static cz.cas.lib.arclib.bpm.BpmConstants.FixityGeneration;
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
     * Computes 3 types of fixity for whole SIP (CRC32,SHA512,MD5) and also fetches file sizes and computes SHA512 checksum of all SIP files
     */
    @Override
    public void executeArclibDelegate(DelegateExecution execution, String ingestWorkflowExternalId) {
        Path sipZipPath = getSipZipPath(execution);
        try {
            String md5 = bytesToHexString(md5Counter.computeDigest(sipZipPath));
            String sha512 = bytesToHexString(sha512Counter.computeDigest(sipZipPath));
            String crc32 = bytesToHexString(crc32Counter.computeDigest(sipZipPath));

            IngestEvent fixityGenerationEvent = new IngestEvent(ingestWorkflowService.findByExternalId(ingestWorkflowExternalId),
                    toolService.getByNameAndVersion(getToolName(), getToolVersion()), true, null);
            ingestEventStore.save(fixityGenerationEvent);

            Map<String, String> mapOfEventIdsToMd5Calculations = (Map<String, String>)
                    execution.getVariable(FixityGeneration.mapOfEventIdsToSipMd5);
            Map<String, String> mapOfEventIdsToSha512Calculations = (Map<String, String>)
                    execution.getVariable(FixityGeneration.mapOfEventIdsToSipSha512);
            Map<String, String> mapOfEventIdsToCrc32Calculations = (Map<String, String>)
                    execution.getVariable(FixityGeneration.mapOfEventIdsToSipCrc32);

            mapOfEventIdsToMd5Calculations.put(fixityGenerationEvent.getId(), md5);
            mapOfEventIdsToSha512Calculations.put(fixityGenerationEvent.getId(), sha512);
            mapOfEventIdsToCrc32Calculations.put(fixityGenerationEvent.getId(), crc32);
            execution.setVariable(FixityGeneration.mapOfEventIdsToSipMd5, mapOfEventIdsToMd5Calculations);
            execution.setVariable(FixityGeneration.mapOfEventIdsToSipCrc32, mapOfEventIdsToCrc32Calculations);
            execution.setVariable(FixityGeneration.mapOfEventIdsToSipSha512, mapOfEventIdsToSha512Calculations);

            Path sipFolderWsPathStr = Paths.get((String) execution.getVariable(BpmConstants.ProcessVariables.sipFolderWorkspacePath));
            Map<String, Map<String, Triple<Long, String, String>>> mapOfEventIdsToSipContentFixityData = (Map<String, Map<String, Triple<Long, String, String>>>)
                    execution.getVariable(FixityGeneration.mapOfEventIdsToSipContentFixityData);
            Map<String, Triple<Long, String, String>> sipContentFixityData = new HashMap<>();
            for (String filePathStr : ArclibUtils.listFilePaths(sipFolderWsPathStr)) {
                Path wsFilePath = sipFolderWsPathStr.resolve(filePathStr);
                Triple<Long, String, String> fileFixity = Triple.of(wsFilePath.toFile().length(), MetsChecksumType.SHA512.toString(), bytesToHexString(sha512Counter.computeDigest(wsFilePath)));
                sipContentFixityData.put(filePathStr, fileFixity);
            }
            mapOfEventIdsToSipContentFixityData.put(fixityGenerationEvent.getId(), sipContentFixityData);
            execution.setVariable(FixityGeneration.mapOfEventIdsToSipContentFixityData, mapOfEventIdsToSipContentFixityData);

            if (mapOfEventIdsToMd5Calculations.size() == 1) {
                execution.setVariable(FixityGeneration.preferredFixityGenerationEventId, fixityGenerationEvent.getId());
            }
        } catch (IOException e) {
            throw new GeneralException("error occurred during computation of checksum: " + e.getMessage());
        }
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
