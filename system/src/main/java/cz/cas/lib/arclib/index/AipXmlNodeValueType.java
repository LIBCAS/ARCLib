package cz.cas.lib.arclib.index;

/**
 * Type of ArclibXml node value.
 * <p>
 * The type is parsed from config file with field definitions and used during ArclibXml
 * index record creation to handle specific values, like DATE.
 * </p>
 */
public enum AipXmlNodeValueType {
    DATE, TIME, DATETIME, OTHER
}
