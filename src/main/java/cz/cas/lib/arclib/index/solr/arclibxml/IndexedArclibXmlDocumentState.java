package cz.cas.lib.arclib.index.solr.arclibxml;

import lombok.Getter;

/**
 * Stav indexovaného XML zodpovedajci stavu XML na archival storage
 */
@Getter
public enum IndexedArclibXmlDocumentState {
    PROCESSED,
    PERSISTED,
    REMOVED,
    DELETED
}
