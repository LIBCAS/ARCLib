package cz.cas.lib.arclib.bpm;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cas.lib.arclib.domain.IngestToolFunction;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestEvent;
import cz.cas.lib.arclib.domain.profiles.SipProfile;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import cz.cas.lib.arclib.service.fixity.BagitPackageFixityVerifier;
import cz.cas.lib.arclib.service.fixity.MetsPackageFixityVerifier;
import cz.cas.lib.arclib.service.fixity.PackageFixityVerifier;
import cz.cas.lib.arclib.store.SipProfileStore;
import cz.cas.lib.core.exception.GeneralException;
import cz.cas.lib.core.store.Transactional;
import cz.cas.lib.core.util.Utils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static cz.cas.lib.core.util.Utils.listFilesMatchingGlobPattern;

@Slf4j
@Service
public class FixityCheckerDelegate extends ArclibDelegate implements JavaDelegate {
    public static final String CONFIG_INVALID_CHECKSUMS = "/fixityCheck/continueOnInvalidChecksums";
    public static final String CONFIG_UNSUPPORTED_CHECKSUM_TYPE = "/fixityCheck/continueOnUnsupportedChecksumType";
    public static final String CONFIG_MISSING_FILES = "/fixityCheck/continueOnMissingFiles";

    private MetsPackageFixityVerifier metsFixityVerifier;
    private BagitPackageFixityVerifier bagitFixityVerifier;
    private SipProfileStore sipProfileStore;
    @Getter
    private String toolName="ARCLib_"+ IngestToolFunction.fixity_check;

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
        log.debug("Execution of Fixity checker delegate started for ingest workflow " + ingestWorkflowExternalId + ".");

        String sipProfileId = getStringVariable(execution, BpmConstants.ProcessVariables.sipProfileId);
        JsonNode config = getConfigRoot(execution);
        SipProfile sipProfile = sipProfileStore.find(sipProfileId);
        Path sipFolderWorkspacePath = Paths.get((String) execution.getVariable(BpmConstants.ProcessVariables.sipFolderWorkspacePath));

        List<Utils.Triplet<String, String, String>> filePathsAndFixities;
        //support for multiple bpm tasks for fixity check
        List<Utils.Triplet<String, String, String>> previousResult = (List<Utils.Triplet<String, String, String>>) execution.getVariable(BpmConstants.FixityCheck.filePathsAndFixities);
        filePathsAndFixities = previousResult == null ? new ArrayList<>() : previousResult;

        PackageFixityVerifier verifier;

        String sipMetadataPathGlobPattern = sipProfile.getSipMetadataPathGlobPattern();
        List<File> matchingFiles = listFilesMatchingGlobPattern(new File(sipFolderWorkspacePath.toAbsolutePath().toString()), sipMetadataPathGlobPattern);

        if (matchingFiles.size() == 0) throw new GeneralException("File with metadata for ingest workflow with external id "
                + ingestWorkflowExternalId + " does not exist at path given by glob pattern: " + sipMetadataPathGlobPattern);

        if (matchingFiles.size() > 1) throw new GeneralException("Multiple files found " +
                "at the path given by glob pattern: " + sipMetadataPathGlobPattern);

        File metsFile = matchingFiles.get(0);

        switch (sipProfile.getPackageType()) {
            case METS:
                verifier = metsFixityVerifier;
                filePathsAndFixities.addAll(verifier
                        .verifySIP(sipFolderWorkspacePath, metsFile.toPath(), ingestWorkflowExternalId,
                                config, getFormatIdentificationResult(execution)));
                break;
            case BAGIT:
                verifier = bagitFixityVerifier;
                filePathsAndFixities.addAll(verifier
                        .verifySIP(sipFolderWorkspacePath, metsFile.toPath().getParent(),
                                ingestWorkflowExternalId, config, getFormatIdentificationResult(execution)));
                break;
            default:
                throw new GeneralException("Unsupported package type: " + sipProfile.getPackageType());
        }

        execution.setVariable(BpmConstants.FixityCheck.filePathsAndFixities, filePathsAndFixities);
        ingestEventStore.save(new IngestEvent(ingestWorkflowStore.findByExternalId(ingestWorkflowExternalId), toolService.findByNameAndVersion(verifier.getToolName(), verifier.getToolVersion()), true, null));
        execution.setVariable(BpmConstants.FixityCheck.success, true);

        log.debug("Execution of Fixity verifier delegate finished for ingest workflow " + ingestWorkflowExternalId + ".");
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