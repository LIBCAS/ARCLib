package cz.cas.lib.arclib.exception.bpm;

import cz.cas.lib.arclib.domain.ingestWorkflow.IngestIssue;

/**
 * Base exception class for all errors which may be solved by changing the JSON config. Every extending class MUST
 * call a super constructor to attach {@link #INCIDENT_MSG_PREFIX} prefix to the exception message.
 */
public class IncidentException extends Exception {
    public static String INCIDENT_MSG_PREFIX = "ARCLib incident: ";

    public IncidentException(String message) {
        super(INCIDENT_MSG_PREFIX + message);
    }

    public IncidentException(IngestIssue issue) {
        super(INCIDENT_MSG_PREFIX + issue.toString());
    }

    public IncidentException(String message, Throwable cause) {
        super(INCIDENT_MSG_PREFIX + message, cause);
    }
}
