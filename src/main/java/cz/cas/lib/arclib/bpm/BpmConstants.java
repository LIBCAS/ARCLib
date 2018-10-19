package cz.cas.lib.arclib.bpm;

public class BpmConstants {

    public static class ProcessVariables {
        public static final String ingestWorkflowId = "ingestWorkflowId";
        public static final String ingestWorkflowExternalId = "ingestWorkflowExternalId";
        public static final String batchId = "batchId";
        public static final String sipId = "sipId";

        public static final String sipVersion = "sipVersion";
        public static final String xmlVersion = "xmlVersion";

        public static final String assignee = "assignee";
        public static final String producerId = "producerId";

        public static final String latestConfig = "latestConfig";
        public static final String errorCode = "errorCode";
        public static final String sipProfileId = "sipProfileId";
        public static final String debuggingModeActive = "debuggingModeActive";
    }

    public static class Ingestion {
        public static final String dateTime = "ingestionDateTime";
        public static final String success = "ingestionSuccess";
        public static final String sizeInBytes = "sizeInBytes";
        public static final String filePathsAndFileSizes = "filePathsAndFileSizes";
        public static final String originalSipFileName = "originalSipFileName";
        public static final String authorialId = "authorialId";
        public static final String rootDirFilesAndFixities = "rootDirFilesAndFixities";
    }

    public static class Validation {
        public static final String dateTime = "validationDateTime";
        public static final String success = "validationSuccess";
        public static final String validationProfileId = "validationProfileId";
    }

    public static class FixityCheck {
        public static final String dateTime = "fixityCheckDateTime";
        public static final String success = "fixityCheckSuccess";
        public static final String filePathsAndFixities = "filePathsAndFixities";
    }

    public static class FormatIdentification {
        public static final String dateTime = "formatIdentificationDateTime";
        public static final String success = "formatIdentificationSuccess";
        public static final String toolName = "formatIdentificationToolName";
        public static final String toolVersion = "formatIdentificationToolVersion";
        public static final String mapOfFilesToFormats = "mapOfFilesToFormats";
    }

    public static class MetadataExtraction {
        public static final String dateTime = "metadataExtractionDateTime";
        public static final String success = "metadataExtractionSuccess";
    }

    public static class Quarantine {
        public static final String dateTime = "quarantineDateTime";
        public static final String success = "quarantineSuccess";
    }

    public static class MessageDigestCalculation {
        public static final String dateTime = "messageDigestDateTime";
        public static final String success = "messageDigestSuccess";
        public static final String checksumMd5 = "checksumMd5";
        public static final String checksumSha512 = "checksumSha512";
        public static final String checksumCrc32 = "checksumCrc32";
    }

    public static class VirusCheck {
        public static final String dateTime = "virusCheckDateTime";
        public static final String success = "virusCheckSuccess";
        public static final String toolName = "virusCheckToolName";
        public static final String toolVersion = "virusCheckToolVersion";
    }

    public static class ArchivalStorage {
        public static final String aipSavedCheckRetries = "aipSavedCheckRetries";
        public static final String aipSavedCheckTimeout = "aipSavedCheckTimeout";
        public static final String aipStoreRetries = "aipStoreRetries";
        public static final String aipStoreTimeout = "aipStoreTimeout";
        public static final String aipState = "aipState";
    }

    /**
     * error codes used when throwing {@link org.camunda.bpm.engine.delegate.BpmnError}
     */
    public static class ErrorCodes {
        public static final String ProcessFailure = "processFailure";
    }
}
