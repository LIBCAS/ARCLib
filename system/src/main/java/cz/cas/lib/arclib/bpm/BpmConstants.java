package cz.cas.lib.arclib.bpm;

import cz.cas.lib.arclib.domain.packages.AuthorialPackage;

public class BpmConstants {

    public static class ProcessVariables {
        public static final String ingestWorkflowId = "ingestWorkflowId";
        public static final String ingestWorkflowExternalId = "ingestWorkflowExternalId";
        public static final String batchId = "batchId";
        public static final String sipId = "sipId";
        public static final String sipFolderWorkspacePath = "sipFolderWorkspacePath";

        public static final String sipVersion = "sipVersion";
        public static final String xmlVersion = "xmlVersion";

        //milliseconds
        public static final String idleTime = "idleTime";
        //epoch milliseconds
        public static final String idlePoint = "idlePoint";

        /**
         * id of the user
         */
        public static final String responsiblePerson = "responsiblePerson";
        public static final String producerId = "producerId";

        public static final String latestConfig = "latestConfig";
        public static final String errorCode = "errorCode";
        public static final String errorMessage = "errorMessage";
        public static final String debuggingModeActive = "debuggingModeActive";
        public static final String producerProfileExternalId = "producerProfileExternalId";
        public static final String randomPriority = "randomPriority";

        public static final String sipFileName = "sipFileName";
        /**
         * note that the actual {@link AuthorialPackage#authorialId} in DB may be different (the old one) during the ingest,
         * if authorial ID is changed during the process.. the change is written to the at the very end of the ingest
         */
        public static final String extractedAuthorialId = "extractedAuthorialId";
    }

    public static class FixityCheck {
        public static final String fixityCheckToolCounter = "fixityCheckToolCounter";
    }

    public static class FormatIdentification {
        public static final String preferredFormatIdentificationEventId = "preferredFormatIdentificationEventId";
        public static final String mapOfEventIdsToMapsOfFilesToFormats = "mapOfEventIdsToMapsOfFilesToFormats";
    }

    public static class MetadataExtraction {
        public static final String result = "metadataExtractionResult";
        public static final String usedSipProfile = "usedSipProfile";
    }

    public static class Validation {
        public static final String usedValidationProfile = "usedValidationProfile";
    }

    public static class FixityGeneration {
        public static final String preferredFixityGenerationEventId = "preferredFixityGenerationEventId";
        public static final String mapOfEventIdsToSipMd5 = "mapOfEventIdsToSipMd5";
        public static final String mapOfEventIdsToSipSha512 = "mapOfEventIdsToSipSha512";
        public static final String mapOfEventIdsToSipCrc32 = "mapOfEventIdsToSipCrc32";
        /**
         * <pre>{@code
         * variable of type:
         * Map<String, Map<String, Triple<Long, String, String>>>
         *
         * filled as:
         * Map<eventId, Map<pathToFile, Triple<sizeInBytes, metsChecksumType, checksum>>>
         * }</pre>
         */
        public static final String mapOfEventIdsToSipContentFixityData = "mapOfEventIdsToSipContentFixityData";
    }

    public static class ArchivalStorage {
        public static final String aipSavedCheckAttempts = "aipSavedCheckAttempts";
        public static final String aipSavedCheckAttemptsInterval = "aipSavedCheckAttemptsInterval";
        public static final String aipStoreAttempts = "aipStoreAttempts";
        public static final String aipStoreAttemptsInterval = "aipStoreAttemptsInterval";
        /**
         * state of the object (AIP in case of data versioning, AIP XML in case of metadata versioning) in Archival Storage
         */
        public static final String archivalStorageResult = "archivalStorageResult";

        public enum ArchivalStorageResultEnum {FAIL, SUCCESS, PROCESSING}
    }

    public static class Antivirus {
        public static final String antivirusToolCounter = "antivirusToolCounter";
    }

    /**
     * error codes used when throwing {@link org.camunda.bpm.engine.delegate.BpmnError}
     */
    public static class ErrorCodes {
        public static final String ProcessFailure = "processFailure";
        public static final String StorageFailure = "storageFailure";
    }
}
