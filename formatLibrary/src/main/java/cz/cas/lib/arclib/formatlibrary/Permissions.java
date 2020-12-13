package cz.cas.lib.arclib.formatlibrary;

/**
 * @see cz.cas.lib.arclib.security.authorization.permission.Permissions
 */
public interface Permissions {
    String RISK_RECORDS_READ    = "RISK_RECORDS_READ";
    String RISK_RECORDS_WRITE   = "RISK_RECORDS_WRITE";

    /**
     * @see cz.cas.lib.arclib.security.authorization.permission.Permissions#FORMAT_RECORDS_READ
     * @see cz.cas.lib.arclib.security.authorization.permission.Permissions#FORMAT_RECORDS_WRITE
     */
    String FORMAT_RECORDS_READ  = "FORMAT_RECORDS_READ";
    String FORMAT_RECORDS_WRITE = "FORMAT_RECORDS_WRITE";
}
