package cz.cas.lib.arclib.service.formatIdentification.droid;

import lombok.Getter;

/**
 * Columns of the CSV file with exported profile produced by the DROID
 */
@Getter
public enum CsvResultColumn {
    ID,
    PARENT_ID,
    URI,
    FILE_PATH,
    NAME,
    METHOD,
    STATUS,
    SIZE,
    TYPE,
    EXT,
    LAST_MODIFIED,
    EXTENSION_MISMATCH,
    HASH,
    FORMAT_COUNT,
    PUID,
    MIME_TYPE,
    FORMAT_NAME,
    FORMAT_VERSION
}
