package cz.cas.lib.arclib.exception;

public class AuthorialPackageNotLockedException extends Exception {
    public AuthorialPackageNotLockedException(String authorialPackageId) {
        super("Can't continue with edit of AIP XML because the authorial package: " + authorialPackageId + " is not locked. " +
                "If the problem persists try increasing arclib.keepAliveNetworkDelay property");
    }
}
