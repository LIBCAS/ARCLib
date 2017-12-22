package cz.inqool.uas.report;

import cz.inqool.uas.exception.MissingObject;
import cz.inqool.uas.index.IndexedDictionaryStore;
import cz.inqool.uas.index.LabeledReference;
import cz.inqool.uas.report.form.ReportForm;
import cz.inqool.uas.report.form.ReportForms;
import cz.inqool.uas.report.location.ReportLocation;
import cz.inqool.uas.report.location.ReportLocations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.util.List;

import static cz.inqool.uas.util.Utils.eq;

/**
 * Implementation of {@link IndexedDictionaryStore} for storing {@link Report} and indexing {@link IndexedReport}.
 */
@Slf4j
@Repository
public class ReportStore extends IndexedDictionaryStore<Report, QReport, IndexedReport> {
    private ReportLocations locations;

    private ReportForms forms;

    public ReportStore() {
        super(Report.class, QReport.class, IndexedReport.class);
    }

    /**
     * Converts a JPA instance to an Elasticsearch instance.
     *
     * <p>
     *     Location is retrieved from {@link ReportLocations} and
     *     form is retrieved from {@link ReportForms}.
     * </p>
     * @param o JPA instance
     * @return Elasticsearch instance
     */
    @Override
    public IndexedReport toIndexObject(Report o) {
        IndexedReport indexedReport = super.toIndexObject(o);

        indexedReport.setLabel(o.getLabel());
        indexedReport.setProvider(o.getProvider());
        indexedReport.setFileName(o.getFileName());
        indexedReport.setParams(o.getParams());

        String locationId = o.getLocation();
        if (locationId != null) {
            ReportLocation location = locations.getLocation(locationId);

            if (location != null) {
                indexedReport.setLocation(new LabeledReference(location.getId(), location.getName()));
            } else {
                log.warn("Unknown report location {} is used. Skipping.", locationId);
            }
        }

        String formId = o.getForm();
        if (formId != null) {
            ReportForm form = forms.getForm(formId);

            if (form != null) {
                indexedReport.setForm(new LabeledReference(form.getId(), form.getName()));
            } else {
                log.warn("Unknown report form {} is used. Skipping.", formId);
            }
        }

        return indexedReport;
    }

    /**
     * Gets the reports with provided location.
     *
     * @param location Provided location
     * @return {@link List} of {@link Report}
     * @throws MissingObject If {@link ReportLocation} does not exist
     */
    public List<Report> findByLocation(String location) {
        eq(locations.isValid(location), true, () -> new MissingObject(ReportLocation.class, location));

        QReport report = qObject();

        List<String> ids = query()
                .select(report.id)
                .where(report.location.eq(location))
                .where(report.active.eq(true))
                .fetch();

        return findAllInList(ids);
    }

    @Inject
    public void setLocations(ReportLocations locations) {
        this.locations = locations;
    }

    @Inject
    public void setForms(ReportForms forms) {
        this.forms = forms;
    }
}
