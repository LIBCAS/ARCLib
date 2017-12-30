package cz.inqool.uas.svg;

import cz.inqool.uas.exception.GeneralException;

/**
 * Special exception for creating new graphics context
 */
public class GraphicsContextException extends GeneralException {

    public GraphicsContextException(String message, Throwable cause) {
        super(message, cause);
    }
}
