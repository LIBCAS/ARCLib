package cz.inqool.uas.security;

/**
 * List of all permissions in UAS core.
 *
 * Important! Every permission needs to have ROLE_ prefix for spring security to work.
 * fixme: add security restriction on APIs
 */
public class Permissions {
    public static final String ACTION = "ROLE_ACTION";
    public static final String SEQUENCE = "ROLE_SEQUENCE";
    public static final String REPORT = "ROLE_REPORT";
    public static final String BPM = "ROLE_BPM";
    public static final String ERROR = "ROLE_ERROR";
    public static final String JOB = "ROLE_JOB";
    public static final String DICTIONARY = "ROLE_DICTIONARY";
}
