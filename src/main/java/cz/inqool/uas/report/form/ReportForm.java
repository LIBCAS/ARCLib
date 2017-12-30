package cz.inqool.uas.report.form;

import org.springframework.context.annotation.Bean;

/**
 * A form used in report data gathering.
 *
 * <p>
 *     Implemented in frontend, but declared in backend. {@link ReportForm}s not declared will not be accessible in
 *     {@link cz.inqool.uas.report.Report} definition.
 * </p>
 * <p>
 *     Declaration is done by producing a {@link Bean} of implementation class.
 * </p>
 */
public interface ReportForm {
    String getId();
    String getName();
}
