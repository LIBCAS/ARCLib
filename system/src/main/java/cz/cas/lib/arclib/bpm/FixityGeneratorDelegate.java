package cz.cas.lib.arclib.bpm;

import cz.cas.lib.arclib.domain.HashType;
import cz.cas.lib.arclib.domain.IngestToolFunction;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestEvent;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.service.fixity.FixityCounterFacade;
import cz.cas.lib.arclib.service.fixity.MetsChecksumType;
import cz.cas.lib.arclib.utils.ArclibUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static cz.cas.lib.arclib.bpm.BpmConstants.FixityGeneration;
import static cz.cas.lib.core.util.Utils.bytesToHexString;

@Slf4j
@Service
public class FixityGeneratorDelegate extends ArclibDelegate {

    private FixityCounterFacade fixityCounterFacade;
    @Getter
    private String toolName = "ARCLib_" + IngestToolFunction.message_digest_calculation;

    /**
     * Computes 3 types of fixity for whole SIP (CRC32,SHA512,MD5) and also fetches file sizes and computes SHA512 checksum of all SIP files
     */
    @Override
    public void executeArclibDelegate(DelegateExecution execution) {
        Path sipZipPath = getSipZipPath(execution);
        try {
            String md5 = bytesToHexString(fixityCounterFacade.computeDigest(HashType.MD5, sipZipPath));
            String sha512 = bytesToHexString(fixityCounterFacade.computeDigest(HashType.Sha512, sipZipPath));
            String crc32 = bytesToHexString(fixityCounterFacade.computeDigest(HashType.Crc32, sipZipPath));

            IngestEvent fixityGenerationEvent = new IngestEvent(ingestWorkflowService.findByExternalId(getIngestWorkflowExternalId(execution)),
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

            Path sipFolderWsPathStr = getSipFolderWorkspacePath(execution);
            Map<String, Map<String, Triple<Long, String, String>>> mapOfEventIdsToSipContentFixityData = (Map<String, Map<String, Triple<Long, String, String>>>)
                    execution.getVariable(FixityGeneration.mapOfEventIdsToSipContentFixityData);
            Map<String, Triple<Long, String, String>> sipContentFixityData = new HashMap<>();
            for (String filePathStr : ArclibUtils.listFilePaths(sipFolderWsPathStr)) {
                Path wsFilePath = sipFolderWsPathStr.resolve(filePathStr);
                Triple<Long, String, String> fileFixity = Triple.of(wsFilePath.toFile().length(), MetsChecksumType.SHA512.toString(), bytesToHexString(fixityCounterFacade.computeDigest(HashType.Sha512, wsFilePath)));
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
    public void setFixityCounterFacade(FixityCounterFacade fixityCounterFacade) {
        this.fixityCounterFacade = fixityCounterFacade;
    }
}
