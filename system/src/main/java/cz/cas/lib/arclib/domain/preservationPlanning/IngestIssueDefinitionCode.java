package cz.cas.lib.arclib.domain.preservationPlanning;

/**
 * Aplikační kód chyby z číselníku chyb
 */
public enum IngestIssueDefinitionCode {
    CONFIG_PARSE_ERROR,
    FILE_VIRUS_FOUND,
    FILE_MISSING,
    FILE_INVALID_CHECKSUM,
    FILE_UNSUPPORTED_CHECKSUM_TYPE,
    FILE_FORMAT_RESOLVED_BY_CONFIG,
    FILE_FORMAT_UNRESOLVABLE,
    AUTHORIAL_PACKAGE_LOCKED,
    INTERNAL_ERROR,
    INVALID_CHECKSUM,
    BATCH_CANCELLATION,
    INCIDENT_CANCELLATION,
    BPM_ERROR
}
