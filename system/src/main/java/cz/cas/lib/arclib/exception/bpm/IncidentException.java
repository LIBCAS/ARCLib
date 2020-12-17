package cz.cas.lib.arclib.exception.bpm;

import cz.cas.lib.arclib.bpm.ArclibDelegate;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestIssue;
import cz.cas.lib.arclib.domain.preservationPlanning.IngestIssueDefinitionCode;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * Base exception class for all errors which may be solved by changing the JSON config. {@link ArclibDelegate} will
 * persists {@link IngestIssue}s according to data passed in constructor. Methods throwing the exception should not persist
 * the issues themselves.
 */
public class IncidentException extends Exception {
    public static final String INCIDENT_MSG_PREFIX = "ARCLib incident: ";
    /**
     * throwing method may provide pre-constructed issue objects to be persisted by {@link ArclibDelegate}
     */
    @Getter
    private List<IngestIssue> providedIssues;

    public IncidentException(String message) {
        super(INCIDENT_MSG_PREFIX + message);
    }

    public IncidentException(List<IngestIssue> issues) {
        super(INCIDENT_MSG_PREFIX + (issues != null ? Arrays.toString(issues.toArray()) : ""));
    }

    public IncidentException(String message, Throwable cause) {
        super(INCIDENT_MSG_PREFIX + message, cause);
    }

    public IngestIssueDefinitionCode getDefaultIssueDefinitionCode() {
        return IngestIssueDefinitionCode.INTERNAL_ERROR;
    }
}
