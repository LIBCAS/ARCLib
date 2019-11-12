package cz.cas.lib.arclib.index.solr.arclibxml;

/**
 * State of the AIP at archival storage stored in the ARCLib XML index
 */
public enum IndexedAipState {
    //AIP has been successfully archived
    ARCHIVED,
    //AIP has been physically deleted from archival storage
    DELETED,
    //AIP has been logically removed from archival storage
    REMOVED,
}
