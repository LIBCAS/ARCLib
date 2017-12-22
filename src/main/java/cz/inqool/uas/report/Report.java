package cz.inqool.uas.report;

import cz.inqool.uas.domain.DictionaryObject;
import cz.inqool.uas.file.FileRef;
import cz.inqool.uas.report.form.ReportForm;
import cz.inqool.uas.report.location.ReportLocation;
import cz.inqool.uas.report.provider.ReportProvider;
import cz.inqool.uas.report.provider.SqlProvider;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


/**
 * Single named report definition.
 *
 * <p>
 *     Every report definition has:
 * </p>
 *     <ul>
 *         <li>A file defining the produced content (also the used templater).</li>
 *         <li>A {@link ReportProvider} responsible for gathering the data.</li>
 *         <li>Location to show the report definition available.</li>
 *         <li>Optionally fixed parameter list in JSON format.</li>
 *         <li>Optionally the form used for gathering user input.</li>
 *     </ul>
 */
@Getter
@Setter
@Entity
@Table(name = "uas_report")
public class Report extends DictionaryObject {
    /**
     * Label shown in menu
     */
    protected String label;

    /**
     * File template.
     */
    //@NotNull
    @Fetch(FetchMode.SELECT)
    @ManyToOne
    protected FileRef template;

    /**
     * Used {@link ReportProvider} class name
     */
    //@NotNull
    protected String provider;

    /**
     * Fixed params
     *
     * <p>
     *     E.g. {@link SqlProvider} requires SQL query, which should be fixed in those params. Only SQL params should
     *     be supplied in this case.
     * </p>
     */
    @Lob
    protected String params;

    /**
     * {@link ReportLocation} specifying the place to offer this report
     */
    //@NotNull
    protected String location;

    /**
     * {@link ReportForm} specifying the form to use
     *
     * <p>
     *     Can be null, if no form is required (fully fixed params or no params needed)
     * </p>
     */
    protected String form;

    /**
     * File name for generated report
     *
     * <p>
     *     todo: extend to support templating with current date and time
     * </p>
     */
    protected String fileName;
}
