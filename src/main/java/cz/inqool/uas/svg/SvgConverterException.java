package cz.inqool.uas.svg;

import cz.inqool.uas.exception.GeneralException;

/**
 * Special exception for service transcoding SVG to PNG
 */
public class SvgConverterException extends GeneralException {

    public SvgConverterException (String message, Throwable cause) {
        super(message, cause);
    }
}
