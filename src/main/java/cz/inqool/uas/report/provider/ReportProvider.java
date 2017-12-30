package cz.inqool.uas.report.provider;

import cz.inqool.uas.report.form.ReportForm;
import cz.inqool.uas.report.location.ReportLocation;

import java.util.List;
import java.util.Map;

/**
 * Report data provider.
 *
 * Every provider should be provided by own class because they are distingueshed by class name.
 *
 * <p>
 *     Unlike {@link ReportLocation} and {@link ReportForm} the identification of {@link ReportProvider} is
 *     done through class name.
 * </p>
 */
public interface ReportProvider {
    /**
     * Gathers backend data in the form of a {@link Map}.
     *
     * <p>
     *     Backend data could be represented for example by a {@link List} of database rows. In most cases the
     *     provider will need some params as input (e.g. sql query, date time range provided by user, ...).
     * </p>
     * <p>
     *     Developer should extend {@link BaseProvider} so he can work with actual parameters instead of a {@link Map}.
     * </p>
     * <p>
     *     Return data should contain the input params as they are in many cases used in the report template.
     *     This is done automatically if extending {@link BaseProvider}.
     * </p>
     *
     * @param input Input parameters
     * @return gathered data in the form of a {@link Map}
     */
    Map<String, Object> provide(Map<String, Object> input);

    /**
     * Gets user friendly name of the provider.
     * @return Name of the provider
     */
    String getName();

    /**
     * Gets Id of the report provider (class name of the provider).
     *
     * @return Id of the report provider
     */
    default String getId() {
        return this.getClass().getName();
    }
}
