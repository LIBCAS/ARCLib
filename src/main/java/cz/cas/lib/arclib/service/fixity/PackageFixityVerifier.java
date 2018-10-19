package cz.cas.lib.arclib.service.fixity;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.bpm.FixityCheckerDelegate;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestIssue;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.packages.PackageType;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import cz.cas.lib.arclib.store.IngestIssueStore;
import cz.cas.lib.arclib.store.IngestWorkflowStore;
import cz.cas.lib.arclib.utils.ArclibUtils;
import cz.cas.lib.core.store.Transactional;
import cz.cas.lib.core.util.Utils;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Service
public abstract class PackageFixityVerifier {

    Md5Counter md5Counter;
    Sha512Counter sha512Counter;
    Sha256Counter sha256Counter;
    Sha1Counter sha1Counter;
    private IngestIssueStore ingestIssueStore;
    private IngestWorkflowStore ingestWorkflowStore;

    /**
     * Verifies fixity of every file specified in metadata file(s) of the package.
     * The logic of implementing class is tied with {@link PackageType}
     * Currently supports MD5, SHA-1, SHA-256 and SHA-512.
     * May invoke three types of issue in following order:
     * <ol>
     * <li>unsupported checksum type: description contains {@link Map<String,List<Path>} with checksum type and corresponding files (even if the file does not exist)</li>
     * <li>files not found: description contains {@link List<Path>} of files which does not exist</li>
     * <li>invalid checksums: description contains {@link List<Path>} of files with invalid fixites</li>
     * </ol>
     * The first issue which is not automatically solved by config stops process and invokes new Incident
     *
     * @param pathToFixityFile Path to a file which contains fixity information of files of the package.
     * @param externalId       used in case of issue
     * @param configRoot       used in case of issue
     * @return list of associated values in triplets: file path, type of fixity, fixity value.
     */
    public abstract List<Utils.Triplet<String, String, String>> verifySIP(Path pathToFixityFile, String externalId, JsonNode configRoot)
            throws IOException, IncidentException;

    @Transactional
    public void invokeUnsupportedChecksumTypeIssue(Map<String, List<Path>> files, String externalId, JsonNode configRoot)
            throws IncidentException {
        StringBuilder issueMessage = new StringBuilder();
        for (String algorithm : files.keySet()) {
            issueMessage.append("unsupported checksum algorithm: " + algorithm + " used for files: " + Utils.toString(files.get(algorithm)) + "; ");
        }
        IngestWorkflow ingestWorkflow = ingestWorkflowStore.findByExternalId(externalId);
        IngestIssue issue = new IngestIssue(
                ingestWorkflow,
                BpmConstants.FixityCheck.class, issueMessage.toString()
        );
        Boolean value = ArclibUtils.parseBooleanConfig(configRoot, FixityCheckerDelegate.CONFIG_UNSUPPORTED_CHECKSUM_TYPE, issue);
        ingestIssueStore.save(issue);
        if (value == null)
            throw new IncidentException(issue);
        if (!value)
            throw new BpmnError(BpmConstants.ErrorCodes.ProcessFailure, issue.toString());
    }

    @Transactional
    public void invokeInvalidChecksumsIssue(List<Path> files, String externalId, JsonNode configRoot)
            throws IncidentException {
        IngestWorkflow ingestWorkflow = ingestWorkflowStore.findByExternalId(externalId);
        IngestIssue issue = new IngestIssue(
                ingestWorkflow,
                BpmConstants.FixityCheck.class, "invalid checksum of files: " + Utils.toString(files)
        );
        Boolean value = ArclibUtils.parseBooleanConfig(configRoot, FixityCheckerDelegate.CONFIG_INVALID_CHECKSUMS, issue);
        ingestIssueStore.save(issue);
        if (value == null)
            throw new IncidentException(issue);
        if (!value)
            throw new BpmnError(BpmConstants.ErrorCodes.ProcessFailure, issue.toString());
    }

    @Transactional
    public void invokeMissingFilesIssue(List<Path> files, String externalId, JsonNode configRoot)
            throws IncidentException {
        IngestWorkflow ingestWorkflow = ingestWorkflowStore.findByExternalId(externalId);
        IngestIssue issue = new IngestIssue(
                ingestWorkflow,
                BpmConstants.FixityCheck.class, "missing files: " + Utils.toString(files)
        );
        Boolean value = ArclibUtils.parseBooleanConfig(configRoot, FixityCheckerDelegate.CONFIG_MISSING_FILES, issue);
        ingestIssueStore.save(issue);
        if (value == null)
            throw new IncidentException(issue);
        if (!value)
            throw new BpmnError(BpmConstants.ErrorCodes.ProcessFailure, issue.toString());
    }

    @Inject
    public void setIngestWorkflowStore(IngestWorkflowStore ingestWorkflowStore) {
        this.ingestWorkflowStore = ingestWorkflowStore;
    }

    @Inject
    public void setMd5Counter(Md5Counter md5Counter) {
        this.md5Counter = md5Counter;
    }

    @Inject
    public void setSha512Counter(Sha512Counter sha512Counter) {
        this.sha512Counter = sha512Counter;
    }

    @Inject
    public void setSha256Counter(Sha256Counter sha256Counter) {
        this.sha256Counter = sha256Counter;
    }

    @Inject
    public void setSha1Counter(Sha1Counter sha1Counter) {
        this.sha1Counter = sha1Counter;
    }

    @Inject
    public void setIngestIssueStore(IngestIssueStore ingestIssueStore) {
        this.ingestIssueStore = ingestIssueStore;
    }
}
