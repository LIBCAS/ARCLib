package cz.inqool.uas.report.exception;

import cz.inqool.uas.exception.GeneralException;

/**
 * Exception, when there is a problem during report generating
 */
public class GeneratingException extends GeneralException {
    public GeneratingException(String message) {
        super(message);
    }

    public GeneratingException(String message, Throwable cause) {
        super(message, cause);
    }
}
