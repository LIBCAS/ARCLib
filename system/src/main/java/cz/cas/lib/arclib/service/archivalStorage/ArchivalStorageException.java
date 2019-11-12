package cz.cas.lib.arclib.service.archivalStorage;

public class ArchivalStorageException extends Exception {

    public ArchivalStorageException(String message) {
        super(message);
    }

    public ArchivalStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public ArchivalStorageException(Throwable cause) {
        super(cause);
    }

    public ArchivalStorageException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
