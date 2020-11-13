package cz.cas.lib.arclib.bpm;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cas.lib.arclib.domain.IngestToolFunction;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestEvent;
import cz.cas.lib.arclib.domain.profiles.SipProfile;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import cz.cas.lib.arclib.service.SipProfileService;
import cz.cas.lib.arclib.service.fixity.BagitFixityChecker;
import cz.cas.lib.arclib.service.fixity.CommonChecksumFilesChecker;
import cz.cas.lib.arclib.service.fixity.FixityChecker;
import cz.cas.lib.arclib.service.fixity.MetsFixityChecker;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static cz.cas.lib.arclib.bpm.ArclibXmlExtractorDelegate.SIP_PROFILE_CONFIG_ENTRY;
import static cz.cas.lib.arclib.bpm.BpmConstants.FixityCheck;
import static cz.cas.lib.arclib.bpm.BpmConstants.ProcessVariables;
import static cz.cas.lib.core.util.Utils.listFilesMatchingGlobPattern;

@Slf4j
@Service
public class FixityCheckerDelegate extends ArclibDelegate {
    public static final String FIXITY_CHECK_TOOL = "/fixityCheck";
    public static final String CONFIG_INVALID_CHECKSUMS = "/continueOnInvalidChecksums";
    public static final String CONFIG_UNSUPPORTED_CHECKSUM_TYPE = "/continueOnUnsupportedChecksumType";
    public static final String CONFIG_MISSING_FILES = "/continueOnMissingFiles";

    private MetsFixityChecker metsFixityVerifier;
    private BagitFixityChecker bagitFixityVerifier;
    private CommonChecksumFilesChecker commonChecksumFilesChecker;
    private SipProfileService sipProfileService;
    @Getter
    private String toolName = "ARCLib_"+ IngestToolFunction.fixity_check;

    /**
     * Checks fixity of files specified in SIP META XML and sets BPM variable `filePathsAndFixities`.
     * <p>
     * Expects <i>sipMetaPath</i> String variable to be set in process. This variable should contain path to SIP META XML.
     * </p>
     */
    @Override
    public void executeArclibDelegate(DelegateExecution execution, String ingestWorkflowExternalId) throws IOException, IncidentException {
        JsonNode config = getConfigRoot(execution);
        String sipProfileExternalId = config.get(SIP_PROFILE_CONFIG_ENTRY).textValue();
        SipProfile sipProfile = sipProfileService.findByExternalId(sipProfileExternalId);
        Path sipFolderWorkspacePath = Paths.get((String) execution.getVariable(ProcessVariables.sipFolderWorkspacePath));

        int fixityCheckToolCounter = (int) execution.getVariable(FixityCheck.fixityCheckToolCounter);
        FixityChecker fixityChecker;

        String sipMetadataPathGlobPattern = sipProfile.getSipMetadataPathGlobPattern();
        List<File> matchingFiles = listFilesMatchingGlobPattern(new File(sipFolderWorkspacePath.toAbsolutePath().toString()), sipMetadataPathGlobPattern);

        if (matchingFiles.size() == 0)
            throw new GeneralException("File with metadata for ingest workflow with external id "
                    + ingestWorkflowExternalId + " does not exist at path given by glob pattern: " + sipMetadataPathGlobPattern);

        if (matchingFiles.size() > 1) throw new GeneralException("Multiple files found " +
                "at the path given by glob pattern: " + sipMetadataPathGlobPattern);

        File metsFile = matchingFiles.get(0);

        switch (sipProfile.getPackageType()) {
            case METS:
                fixityChecker = metsFixityVerifier;
                fixityChecker.setFixityCheckToolCounter(fixityCheckToolCounter);
                fixityChecker.verifySIP(sipFolderWorkspacePath, metsFile.toPath(), ingestWorkflowExternalId,
                        config, getFormatIdentificationResult(execution));
                break;
            case BAGIT:
                fixityChecker = bagitFixityVerifier;
                fixityChecker.setFixityCheckToolCounter(fixityCheckToolCounter);
                fixityChecker.verifySIP(sipFolderWorkspacePath, sipFolderWorkspacePath,
                        ingestWorkflowExternalId, config, getFormatIdentificationResult(execution));
                break;
            default:
                throw new GeneralException("Unsupported package type: " + sipProfile.getPackageType());
        }
        commonChecksumFilesChecker.verifySIP(sipFolderWorkspacePath, sipFolderWorkspacePath, ingestWorkflowExternalId, config, getFormatIdentificationResult(execution));
        IngestEvent fixityCheckEvent = new IngestEvent(ingestWorkflowService.findByExternalId(ingestWorkflowExternalId), toolService.getByNameAndVersion(fixityChecker.getToolName(), fixityChecker.getToolVersion()), true, null);
        ingestEventStore.save(fixityCheckEvent);

        execution.setVariable(FixityCheck.fixityCheckToolCounter, fixityCheckToolCounter + 1);
    }

    @Inject
    public void setSipProfileService(SipProfileService sipProfileService) {
        this.sipProfileService = sipProfileService;
    }

    @Inject
    public void setMetsFixityVerifier(MetsFixityChecker metsFixityVerifier) {
        this.metsFixityVerifier = metsFixityVerifier;
    }

    @Inject
    public void setBagitFixityVerifier(BagitFixityChecker bagitFixityVerifier) {
        this.bagitFixityVerifier = bagitFixityVerifier;
    }

    @Inject
    public void setCommonChecksumFilesChecker(CommonChecksumFilesChecker commonChecksumFilesChecker) {
        this.commonChecksumFilesChecker = commonChecksumFilesChecker;
    }
}