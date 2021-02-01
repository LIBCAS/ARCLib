package cz.cas.lib.arclib.domain;

import lombok.Getter;

import java.util.List;

public enum AutoIngestFilePrefix {
    NONE(""),
    TRANSFERRING("TRANSFERRING_"),
    PROCESSING("PROCESSING_"),
    FAILED("FAILED_"),
    ARCHIVED("ARCHIVED_");

    @Getter private final String prefix;

    AutoIngestFilePrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Checks whether given string does not start with one of the prefixes.
     *
     * @param fileName string
     * @return true if given name is not prefixed with one of the declared prefixes.
     */
    public static boolean fileNameIsNotPrefixed(String fileName) {
        final List<AutoIngestFilePrefix> fileProcessingPrefixes = List.of(TRANSFERRING, PROCESSING, FAILED, ARCHIVED);
        String lowerCasedFileName = fileName.toLowerCase();
        for (AutoIngestFilePrefix autoPrefix : fileProcessingPrefixes) {
            if (lowerCasedFileName.startsWith(autoPrefix.prefix.toLowerCase())) {
                return false;
            }
        }
        return true;
    }
}
