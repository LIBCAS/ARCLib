package cz.inqool.uas.report.exception;

import cz.inqool.uas.exception.GeneralException;
import cz.inqool.uas.file.FileRef;
import cz.inqool.uas.report.Report;
import cz.inqool.uas.report.ReportGenerator;

/**
 * Exception, when the {@link FileRef} specified as {@link Report#template} is not supported by {@link ReportGenerator}.
 */
public class UnsupportedTemplateException extends GeneralException {
    public UnsupportedTemplateException(String message) {
        super(message);
    }
}
