package cz.cas.lib.arclib.bpm;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cas.lib.arclib.domain.IngestToolFunction;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestEvent;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.domain.profiles.SipProfile;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.exception.bpm.ConfigParserException;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import cz.cas.lib.arclib.service.SipProfileService;
import cz.cas.lib.arclib.service.fixity.BagitFixityChecker;
import cz.cas.lib.arclib.service.fixity.CommonChecksumFilesChecker;
import cz.cas.lib.arclib.service.fixity.FixityCheckMethod;
import cz.cas.lib.arclib.service.fixity.MetsFixityChecker;
import cz.cas.lib.arclib.store.ProducerProfileStore;
import cz.cas.lib.arclib.store.SipProfileStore;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cz.cas.lib.arclib.bpm.ArclibXmlExtractorDelegate.SIP_PROFILE_CONFIG_ENTRY;
import static cz.cas.lib.arclib.bpm.BpmConstants.FixityCheck;
import static cz.cas.lib.core.util.Utils.listFilesMatchingRegex;

@Slf4j
@Service
public class FixityCheckerDelegate extends ArclibDelegate {
    public static final String FIXITY_CHECK_TOOL = "/fixityCheck";
    public static final String CONFIG_INVALID_CHECKSUMS = "/continueOnInvalidChecksums";
    public static final String CONFIG_UNSUPPORTED_CHECKSUM_TYPE = "/continueOnUnsupportedChecksumType";
    public static final String CONFIG_MISSING_FILES = "/continueOnMissingFiles";
    public static final String CHECK_METHODS = "/methods";

    private MetsFixityChecker metsFixityVerifier;
    private BagitFixityChecker bagitFixityVerifier;
    private CommonChecksumFilesChecker commonChecksumFilesChecker;
    private SipProfileService sipProfileService;
    private SipProfileStore sipProfileStore;
    private ProducerProfileStore producerProfileStore;
    @Getter
    private String toolName = "ARCLib_" + IngestToolFunction.fixity_check;

    /**
     * Checks fixity of files specified in SIP META XML and sets BPM variable `filePathsAndFixities`.
     * <p>
     * Expects <i>sipMetaPath</i> String variable to be set in process. This variable should contain path to SIP META XML.
     * </p>
     */
    @Override
    public void executeArclibDelegate(DelegateExecution execution) throws IOException, IncidentException {
        JsonNode config = getConfigRoot(execution);
        Path sipFolderWorkspacePath = getSipFolderWorkspacePath(execution);
        int fixityCheckToolCounter = (int) execution.getVariable(FixityCheck.fixityCheckToolCounter);

        String methodsConfigPath = FIXITY_CHECK_TOOL + "/" + fixityCheckToolCounter + CHECK_METHODS;
        JsonNode jsonNode = config.at(methodsConfigPath);
        if (jsonNode.isMissingNode() || !jsonNode.isTextual())
            throw new ConfigParserException(methodsConfigPath, jsonNode.isMissingNode() ? "missing" : jsonNode.toString(), FixityCheckMethod.class);
        String checkMethodsListFromConfig = jsonNode.textValue();
        Map<String, FixityCheckMethod> supportedCheckMethods = Arrays.stream(FixityCheckMethod.values()).collect(Collectors.toMap(Enum::toString, m -> m));
        List<FixityCheckMethod> requestedCheckMethods = new ArrayList<>();
        for (String s : checkMethodsListFromConfig.split("\\s*,\\s*")) {
            FixityCheckMethod requestedCheckMethod = supportedCheckMethods.get(s.toUpperCase());
            if (requestedCheckMethod == null)
                throw new ConfigParserException(methodsConfigPath, checkMethodsListFromConfig, FixityCheckMethod.class);
            requestedCheckMethods.add(requestedCheckMethod);
        }
        if (requestedCheckMethods.isEmpty())
            throw new ConfigParserException(methodsConfigPath, checkMethodsListFromConfig, FixityCheckMethod.class);

        String ingestWorkflowExternalId = getIngestWorkflowExternalId(execution);
        for (FixityCheckMethod requestedCheckMethod : requestedCheckMethods) {
            switch (requestedCheckMethod) {
                case METS:
                    SipProfile sipProfile;
                    JsonNode sipProfileConfigEntry = config.at("/" + SIP_PROFILE_CONFIG_ENTRY);
                    ProducerProfile producerProfile = producerProfileStore.findByExternalId(getProducerProfileExternalId(execution));
                    if (sipProfileConfigEntry.isMissingNode()) {
                        sipProfile = sipProfileService.findByExternalId(producerProfile.getSipProfile().getExternalId());
                    } else {
                        String sipProfileExternalId = sipProfileConfigEntry.textValue();
                        sipProfile = sipProfileService.findByExternalId(sipProfileExternalId);
                        if (sipProfile.isEditable() &&
                                !isInDebugMode(execution)) {
                            sipProfile.setEditable(false);
                            sipProfileStore.save(sipProfile);
                        }
                    }
                    String sipMetadataPathRegex = sipProfile.getSipMetadataPathRegex();
                    List<File> matchingFiles = listFilesMatchingRegex(new File(sipFolderWorkspacePath.toAbsolutePath().toString()), sipMetadataPathRegex, true);

                    if (matchingFiles.isEmpty())
                        throw new GeneralException(String.format("File with metadata for ingest workflow with external id %s does not exist at path given by regex: %s", ingestWorkflowExternalId, sipMetadataPathRegex));

                    if (matchingFiles.size() > 1)
                        throw new GeneralException(String.format("Multiple files found at the path given by regex: %s", sipMetadataPathRegex));

                    File metsFile = matchingFiles.get(0);
                    metsFixityVerifier.verifySIP(sipFolderWorkspacePath, metsFile.toPath(), ingestWorkflowExternalId,
                            config, getFormatIdentificationResult(execution), fixityCheckToolCounter, this);
                    break;
                case BAGIT:
                    bagitFixityVerifier.verifySIP(sipFolderWorkspacePath, sipFolderWorkspacePath,
                            ingestWorkflowExternalId, config, getFormatIdentificationResult(execution), fixityCheckToolCounter, this);
                    break;
                case COMMON:
                    commonChecksumFilesChecker.verifySIP(sipFolderWorkspacePath, sipFolderWorkspacePath, ingestWorkflowExternalId, config, getFormatIdentificationResult(execution), fixityCheckToolCounter, this);
                    break;
                default:
                    throw new GeneralException("Unsupported check method: " + requestedCheckMethod);
            }
        }

        IngestEvent fixityCheckEvent = new IngestEvent(ingestWorkflowService.findByExternalId(ingestWorkflowExternalId), toolService.getByNameAndVersion(getToolName(), getToolVersion()), true, null);
        ingestEventStore.save(fixityCheckEvent);

        execution.setVariable(FixityCheck.fixityCheckToolCounter, fixityCheckToolCounter + 1);
    }

    @Autowired
    public void setSipProfileService(SipProfileService sipProfileService) {
        this.sipProfileService = sipProfileService;
    }

    @Autowired
    public void setMetsFixityVerifier(MetsFixityChecker metsFixityVerifier) {
        this.metsFixityVerifier = metsFixityVerifier;
    }

    @Autowired
    public void setBagitFixityVerifier(BagitFixityChecker bagitFixityVerifier) {
        this.bagitFixityVerifier = bagitFixityVerifier;
    }

    @Autowired
    public void setCommonChecksumFilesChecker(CommonChecksumFilesChecker commonChecksumFilesChecker) {
        this.commonChecksumFilesChecker = commonChecksumFilesChecker;
    }

    @Autowired
    public void setProducerProfileStore(ProducerProfileStore producerProfileStore) {
        this.producerProfileStore = producerProfileStore;
    }

    @Autowired
    public void setSipProfileStore(SipProfileStore sipProfileStore) {
        this.sipProfileStore = sipProfileStore;
    }
}