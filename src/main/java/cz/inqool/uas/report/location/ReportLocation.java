package cz.inqool.uas.report.location;

import org.springframework.context.annotation.Bean;

/**
 * A single report location throughout the application.
 *
 * <p>
 *     Declaration is done by producing a {@link Bean} of implementation class.
 * </p>
 * <p>
 *     One can image this somewhat like this:
 * </p>
   <ol>
       <li>Somewhere in the application user clicks on a button to show available reports (e.g.).</li>
       <li>Frontend calls {@link ReportLocationApi#list()} to receive available reports
       for this button/location.</li>
       <li>Only context specific reports are made visible to the user for choosing.</li>
   </ol>
 */
public interface ReportLocation {
    String getId();
    String getName();
}
