package cz.cas.lib.arclib.bpm;

import cz.cas.lib.arclib.domain.Hash;
import cz.cas.lib.arclib.domain.HashType;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageException;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageService;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageServiceDebug;
import cz.cas.lib.core.util.Utils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import static cz.cas.lib.arclib.bpm.BpmConstants.FixityGeneration;
import static cz.cas.lib.arclib.bpm.BpmConstants.ProcessVariables;

@Slf4j
@Service
public class ArchivalStorageDelegate extends ArclibDelegate {
    private ArchivalStorageService archivalStorageService;
    private ArchivalStorageServiceDebug archivalStorageServiceDebug;
    @Getter
    private String toolName = "ARCLib_archival_storage_transfer";

    /**
     * Stores SIP to archival storage.
     * <p>
     * In case of the debugging mode set to active, an internal debugging version of archival storage is used
     * instead of the production archival storage.
     *
     * @param execution delegate execution storing BPM variables
     */
    @Override
    public void executeArclibDelegate(DelegateExecution execution) throws IOException, ArchivalStorageException {
        String ingestWorkflowExternalId = getIngestWorkflowExternalId(execution);
        log.debug("Execution of Archival storage delegate started for ingest workflow " + ingestWorkflowExternalId + ".");
        IngestWorkflow ingestWorkflow = ingestWorkflowService.findByExternalId(ingestWorkflowExternalId);

        String preferredFixityGenerationEventId = (String) execution.getVariable(FixityGeneration.preferredFixityGenerationEventId);
        Utils.notNull(preferredFixityGenerationEventId, () -> new GeneralException("Failed to retrieve SIP hash because message digest calculation has not been performed."));

        Map<String, String> mapOfEventIdsToSha512Calculations = (Map<String, String>) execution.getVariable(FixityGeneration.mapOfEventIdsToSipSha512);
        String sipHashValue = mapOfEventIdsToSha512Calculations.get(preferredFixityGenerationEventId);

        Hash sipHash = new Hash(sipHashValue, HashType.Sha512);
        String sipId = (String) execution.getVariable(ProcessVariables.sipId);
        boolean debuggingModeActive = isInDebugMode(execution);

        try (FileInputStream sip = new FileInputStream(getSipZipPath(execution).toFile());
             FileInputStream xml = new FileInputStream(getAipXmlPath(execution).toFile())) {
            switch (ingestWorkflow.getVersioningLevel()) {
                case NO_VERSIONING:
                case SIP_PACKAGE_VERSIONING:
                    if (!debuggingModeActive) {
                        archivalStorageService.storeAip(sipId, sip, xml, sipHash, ingestWorkflow.getArclibXmlHash());
                    } else {
                        archivalStorageServiceDebug.storeAip(sipId, sip, xml);
                    }
                    log.info("SIP " + sipId + " has been stored to archival storage.");
                    break;
                case ARCLIB_XML_VERSIONING:
                    Integer xmlVersionNumber = ingestWorkflow.getXmlVersionNumber();
                    if (!debuggingModeActive) {
                        archivalStorageService.updateXml(sipId, xml, ingestWorkflow.getArclibXmlHash(), xmlVersionNumber, false);
                    } else {
                        archivalStorageServiceDebug.updateXml(sipId, xml, xmlVersionNumber);
                    }
                    log.info("Version number " + xmlVersionNumber + " of ArclibXml of SIP " + sipId + " has been stored to archival storage.");
                    break;
                default:
                    throw new GeneralException("Unknown type of ingest workflow versioning level.");
            }
        }
        log.debug("Execution of Archival storage delegate finished for ingest workflow " + ingestWorkflowExternalId + ".");
    }


    @Inject
    public void setArchivalStorageService(ArchivalStorageService archivalStorageService) {
        this.archivalStorageService = archivalStorageService;
    }

    @Inject
    public void setArchivalStorageServiceDebug(ArchivalStorageServiceDebug archivalStorageServiceDebug) {
        this.archivalStorageServiceDebug = archivalStorageServiceDebug;
    }
}
