package cz.cas.lib.arclib.exception;

import cz.cas.lib.arclib.domainbase.exception.GeneralException;

public class InvalidChecksumException extends GeneralException {
    public InvalidChecksumException(String message) {
        super(message);
    }
}