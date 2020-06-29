package cz.cas.lib.arclib.exception;

public class AipStateChangeException extends Exception {
    public AipStateChangeException(String message) {
        super(message);
    }

    public AipStateChangeException(String message, Throwable cause) {
        super(message, cause);
    }
}
