package cz.cas.lib.arclib.bpm;

public class BpmConstants {

    public static class ProcessVariables {
        public static final String ingestWorkflowId = "ingestWorkflowId";
        public static final String ingestWorkflowExternalId = "ingestWorkflowExternalId";
        public static final String batchId = "batchId";
        public static final String sipId = "sipId";
        public static final String sipFolderWorkspacePath = "sipFolderWorkspacePath";

        public static final String sipVersion = "sipVersion";
        public static final String xmlVersion = "xmlVersion";

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
    }

    public static class Ingestion {
        public static final String dateTime = "ingestionDateTime";
        public static final String sipFileName = "sipFileName";
        public static final String authorialId = "authorialId";
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
        public static final String aipSavedCheckRetries = "aipSavedCheckRetries";
        public static final String aipSavedCheckTimeout = "aipSavedCheckTimeout";
        public static final String aipStoreRetries = "aipStoreRetries";
        public static final String aipStoreTimeout = "aipStoreTimeout";
        /**
         * state of the object (AIP in case of data versioning, AIP XML in case of metadata versioning) in Archival Storage
         */
        public static final String stateInArchivalStorage = "aipState";
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
