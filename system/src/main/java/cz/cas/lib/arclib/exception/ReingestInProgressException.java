package cz.cas.lib.arclib.exception;

/**
 * thrown when operation is prohibited because of running reingest
 */
public class ReingestInProgressException extends Exception {
    public ReingestInProgressException(String message) {
        super(message);
    }
}
