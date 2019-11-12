package cz.cas.lib.arclib.bpm;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cas.lib.arclib.domain.IngestToolFunction;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestEvent;
import cz.cas.lib.arclib.domain.profiles.SipProfile;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import cz.cas.lib.arclib.service.SipProfileService;
import cz.cas.lib.arclib.service.fixity.BagitFixityChecker;
import cz.cas.lib.arclib.service.fixity.FixityChecker;
import cz.cas.lib.arclib.service.fixity.MetsFixityChecker;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.core.util.Utils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
    public void executeArclibDelegate(DelegateExecution execution) throws IOException, IncidentException {
        String ingestWorkflowExternalId = getIngestWorkflowExternalId(execution);
        log.debug("Execution of Fixity checker delegate started for ingest workflow " + ingestWorkflowExternalId + ".");

        String sipProfileId = getStringVariable(execution, ProcessVariables.sipProfileId);
        JsonNode config = getConfigRoot(execution);
        SipProfile sipProfile = sipProfileService.find(sipProfileId);
        Path sipFolderWorkspacePath = Paths.get((String) execution.getVariable(ProcessVariables.sipFolderWorkspacePath));

        //map that captures fixity check events and the respective filePathsAndFixities
        HashMap<String,  List<Utils.Triplet<String, String, String>>> mapOfEventIdsToFilePathsAndFixities =
                (HashMap<String,  List<Utils.Triplet<String, String, String>>>)
                        execution.getVariable(FixityCheck.mapOfEventIdsToFilePathsAndFixities);

        //counter specifies the position of the fixity check among other fixity checks in the given BPM workflow
        int fixityCheckerCounter = mapOfEventIdsToFilePathsAndFixities.size();
        FixityChecker fixityChecker;

        String sipMetadataPathGlobPattern = sipProfile.getSipMetadataPathGlobPattern();
        List<File> matchingFiles = listFilesMatchingGlobPattern(new File(sipFolderWorkspacePath.toAbsolutePath().toString()), sipMetadataPathGlobPattern);

        if (matchingFiles.size() == 0) throw new GeneralException("File with metadata for ingest workflow with external id "
                + ingestWorkflowExternalId + " does not exist at path given by glob pattern: " + sipMetadataPathGlobPattern);

        if (matchingFiles.size() > 1) throw new GeneralException("Multiple files found " +
                "at the path given by glob pattern: " + sipMetadataPathGlobPattern);

        File metsFile = matchingFiles.get(0);
        List<Utils.Triplet<String, String, String>> filePathsAndFixities = new ArrayList<>();

        switch (sipProfile.getPackageType()) {
            case METS:
                fixityChecker = metsFixityVerifier;
                fixityChecker.setFixityCheckToolCounter(fixityCheckerCounter);
                filePathsAndFixities.addAll(fixityChecker
                        .verifySIP(sipFolderWorkspacePath, metsFile.toPath(), ingestWorkflowExternalId,
                                config, getFormatIdentificationResult(execution)));
                break;
            case BAGIT:
                fixityChecker = bagitFixityVerifier;
                fixityChecker.setFixityCheckToolCounter(fixityCheckerCounter);
                filePathsAndFixities.addAll(fixityChecker
                        .verifySIP(sipFolderWorkspacePath, metsFile.toPath().getParent(),
                                ingestWorkflowExternalId, config, getFormatIdentificationResult(execution)));
                break;
            default:
                throw new GeneralException("Unsupported package type: " + sipProfile.getPackageType());
        }
        IngestEvent fixityCheckEvent = new IngestEvent(ingestWorkflowStore.findByExternalId(ingestWorkflowExternalId), toolService.findByNameAndVersion(fixityChecker.getToolName(), fixityChecker.getToolVersion()), true, null);
        ingestEventStore.save(fixityCheckEvent);

        mapOfEventIdsToFilePathsAndFixities.put(fixityCheckEvent.getId(), filePathsAndFixities);
        execution.setVariable(FixityCheck.mapOfEventIdsToFilePathsAndFixities, mapOfEventIdsToFilePathsAndFixities);

        if (fixityCheckerCounter == 0) execution.setVariable(FixityCheck.preferredFixityCheckEventId, fixityCheckEvent.getId());

        log.debug("Execution of Fixity check delegate finished for ingest workflow " + ingestWorkflowExternalId + ".");
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
}