package cz.cas.lib.arclib.bpm;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cas.lib.arclib.domain.profiles.SipProfile;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import cz.cas.lib.arclib.service.fixity.BagitPackageFixityVerifier;
import cz.cas.lib.arclib.service.fixity.MetsPackageFixityVerifier;
import cz.cas.lib.arclib.store.SipProfileStore;
import cz.cas.lib.arclib.utils.ArclibUtils;
import cz.cas.lib.core.exception.GeneralException;
import cz.cas.lib.core.store.Transactional;
import cz.cas.lib.core.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
public class FixityCheckerDelegate extends ArclibDelegate implements JavaDelegate {
    public static final String CONFIG_INVALID_CHECKSUMS = "/fixityCheck/continueOnInvalidChecksums";
    public static final String CONFIG_UNSUPPORTED_CHECKSUM_TYPE = "/fixityCheck/continueOnUnsupportedChecksumType";
    public static final String CONFIG_MISSING_FILES = "/fixityCheck/continueOnMissingFiles";

    private MetsPackageFixityVerifier metsFixityVerifier;
    private BagitPackageFixityVerifier bagitFixityVerifier;
    private SipProfileStore sipProfileStore;

    /**
     * Checks fixity of files specified in SIP META XML and sets BPM variable `filePathsAndFixities`.
     * <p>
     * Expects <i>sipMetaPath</i> String variable to be set in process. This variable should contain path to SIP META XML.
     * </p>
     */
    @Transactional
    @Override
    public void execute(DelegateExecution execution) throws IOException, IncidentException {
        String ingestWorkflowExternalId = getIngestWorkflowExternalId(execution);
        log.info("Execution of Fixity checker delegate started for ingest workflow " + ingestWorkflowExternalId + ".");

        String sipProfileId = getStringVariable(execution, BpmConstants.ProcessVariables.sipProfileId);
        JsonNode config = getConfigRoot(execution);
        SipProfile sipProfile = sipProfileStore.find(sipProfileId);
        String originalSipFileName = getStringVariable(execution, BpmConstants.Ingestion.originalSipFileName);
        Path sipWsPath = ArclibUtils.getSipFolderWorkspacePath(ingestWorkflowExternalId, workspace, originalSipFileName);
        List<Utils.Triplet<String, String, String>> filePathsAndFixities;
        switch (sipProfile.getPackageType()) {
            case METS:
                filePathsAndFixities = metsFixityVerifier
                        .verifySIP(sipWsPath.resolve(sipProfile.getSipMetadataPath()), ingestWorkflowExternalId, config);
                break;
            case BAGIT:
                filePathsAndFixities = bagitFixityVerifier
                        .verifySIP(sipWsPath.resolve(sipProfile.getSipMetadataPath()).getParent(), ingestWorkflowExternalId, config);
                break;
            default:
                throw new GeneralException("Unknown package type: " + sipProfile.getPackageType());
        }

        execution.setVariable(BpmConstants.FixityCheck.filePathsAndFixities, filePathsAndFixities);
        execution.setVariable(BpmConstants.FixityCheck.success, true);
        execution.setVariable(BpmConstants.FixityCheck.dateTime,
                Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());

        log.info("Execution of Fixity verifier delegate finished for ingest workflow " + ingestWorkflowExternalId + ".");
    }

    @Inject
    public void setSipProfileStore(SipProfileStore sipProfileStore) {
        this.sipProfileStore = sipProfileStore;
    }

    @Inject
    public void setMetsFixityVerifier(MetsPackageFixityVerifier metsFixityVerifier) {
        this.metsFixityVerifier = metsFixityVerifier;
    }

    @Inject
    public void setBagitFixityVerifier(BagitPackageFixityVerifier bagitFixityVerifier) {
        this.bagitFixityVerifier = bagitFixityVerifier;
    }
}