package cz.cas.lib.arclib.exception;

/**
 * thrown when reingest is in other state than it is expected when performing reingest operation
 */
public class ReingestStateException extends Exception {
    public ReingestStateException(String message) {
        super(message);
    }
}
