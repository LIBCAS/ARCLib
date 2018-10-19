package cz.cas.lib.arclib.index;

/**
 * Type of ArclibXml field. Do not interchange with {@link cz.cas.lib.core.index.solr.FieldType}
 * <p>
 * The type is parsed from config file with field definitions and used during ArclibXml
 * index record creation to handle specific field types.
 * </p>
 */
public enum FieldType {
    DATE, TIME, DATETIME, OTHER
}
