package cz.inqool.uas.report;

import cz.inqool.uas.report.form.ReportForm;
import cz.inqool.uas.report.form.ReportForms;
import cz.inqool.uas.report.location.ReportLocation;
import cz.inqool.uas.report.location.ReportLocations;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;

import static cz.inqool.uas.util.Utils.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class ReportStoreIndexingTest {
    private ReportLocation location;

    private ReportLocations locations;

    private ReportForm form;

    private ReportForms forms;

    private ReportStoreImpl store;

    @Before
    public void setUp() {
        location = new ReportLocation() {
            @Override
            public String getId() {
                return "myLocation";
            }

            @Override
            public String getName() {
                return null;
            }
        };

        locations = new ReportLocations();
        locations.setLocations(asList(location));

        form = new ReportForm() {
            @Override
            public String getId() {
                return "myForm";
            }

            @Override
            public String getName() {
                return null;
            }
        };

        forms = new ReportForms();
        forms.setForms(asList(form));

        store = new ReportStoreImpl();
        store.setLocations(locations);
        store.setForms(forms);
    }

    @Test
    public void toIndexObjectTest() {
        Report report = new Report();
        report.setActive(true);
        report.setCreated(Instant.now());
        report.setUpdated(Instant.now().plusSeconds(5));
        report.setName("myName");
        report.setProvider("myProvider");
        report.setParams("myParams");
        report.setLocation(location.getId());
        report.setForm(form.getId());
        report.setFileName("myFileName");

        IndexedReport indexedReport = store.toIndexObject(report);

        assertThat(indexedReport.getActive(), is(report.getActive()));
        assertThat(indexedReport.getCreated(), is(report.getCreated()));
        assertThat(indexedReport.getUpdated(), is(report.getUpdated()));
        assertThat(indexedReport.getName(), is(report.getName()));
        assertThat(indexedReport.getProvider(), is(report.getProvider()));
        assertThat(indexedReport.getParams(), is(report.getParams()));
        assertThat(indexedReport.getLocation().getId(), is(report.getLocation()));
        assertThat(indexedReport.getForm().getId(), is(report.getForm()));
        assertThat(indexedReport.getFileName(), is(report.getFileName()));
    }

    @Test
    public void toIndexObjectMissingLocationTest() {
        Report report = new Report();
        report.setActive(true);
        report.setCreated(Instant.now());
        report.setUpdated(Instant.now().plusSeconds(5));
        report.setName("myName");
        report.setProvider("myProvider");
        report.setParams("myParams");
        report.setForm(form.getId());
        report.setFileName("myFileName");

        IndexedReport indexedReport = store.toIndexObject(report);

        assertThat(indexedReport.getActive(), is(report.getActive()));
        assertThat(indexedReport.getCreated(), is(report.getCreated()));
        assertThat(indexedReport.getUpdated(), is(report.getUpdated()));
        assertThat(indexedReport.getName(), is(report.getName()));
        assertThat(indexedReport.getProvider(), is(report.getProvider()));
        assertThat(indexedReport.getParams(), is(report.getParams()));
        assertThat(indexedReport.getLocation(), is(nullValue()));
        assertThat(indexedReport.getForm().getId(), is(report.getForm()));
        assertThat(indexedReport.getFileName(), is(report.getFileName()));
    }

    @Test
    public void toIndexObjectMissingFormTest() {
        Report report = new Report();
        report.setActive(true);
        report.setCreated(Instant.now());
        report.setUpdated(Instant.now().plusSeconds(5));
        report.setName("myName");
        report.setProvider("myProvider");
        report.setParams("myParams");
        report.setLocation(location.getId());
        report.setFileName("myFileName");

        IndexedReport indexedReport = store.toIndexObject(report);

        assertThat(indexedReport.getActive(), is(report.getActive()));
        assertThat(indexedReport.getCreated(), is(report.getCreated()));
        assertThat(indexedReport.getUpdated(), is(report.getUpdated()));
        assertThat(indexedReport.getName(), is(report.getName()));
        assertThat(indexedReport.getProvider(), is(report.getProvider()));
        assertThat(indexedReport.getParams(), is(report.getParams()));
        assertThat(indexedReport.getLocation().getId(), is(report.getLocation()));
        assertThat(indexedReport.getForm(), is(nullValue()));
        assertThat(indexedReport.getFileName(), is(report.getFileName()));
    }

    @Test
    public void toIndexObjectNonExistingLocationTest() {
        Report report = new Report();
        report.setActive(true);
        report.setCreated(Instant.now());
        report.setUpdated(Instant.now().plusSeconds(5));
        report.setName("myName");
        report.setProvider("myProvider");
        report.setParams("myParams");
        report.setLocation("fake");
        report.setForm(form.getId());
        report.setFileName("myFileName");

        IndexedReport indexedReport = store.toIndexObject(report);

        assertThat(indexedReport.getActive(), is(report.getActive()));
        assertThat(indexedReport.getCreated(), is(report.getCreated()));
        assertThat(indexedReport.getUpdated(), is(report.getUpdated()));
        assertThat(indexedReport.getName(), is(report.getName()));
        assertThat(indexedReport.getProvider(), is(report.getProvider()));
        assertThat(indexedReport.getParams(), is(report.getParams()));
        assertThat(indexedReport.getLocation(), is(nullValue()));
        assertThat(indexedReport.getForm().getId(), is(report.getForm()));
        assertThat(indexedReport.getFileName(), is(report.getFileName()));
    }

    @Test
    public void toIndexObjectNonExistingFormTest() {
        Report report = new Report();
        report.setActive(true);
        report.setCreated(Instant.now());
        report.setUpdated(Instant.now().plusSeconds(5));
        report.setName("myName");
        report.setProvider("myProvider");
        report.setParams("myParams");
        report.setLocation(location.getId());
        report.setForm("fake");
        report.setFileName("myFileName");

        IndexedReport indexedReport = store.toIndexObject(report);

        assertThat(indexedReport.getActive(), is(report.getActive()));
        assertThat(indexedReport.getCreated(), is(report.getCreated()));
        assertThat(indexedReport.getUpdated(), is(report.getUpdated()));
        assertThat(indexedReport.getName(), is(report.getName()));
        assertThat(indexedReport.getProvider(), is(report.getProvider()));
        assertThat(indexedReport.getParams(), is(report.getParams()));
        assertThat(indexedReport.getLocation().getId(), is(report.getLocation()));
        assertThat(indexedReport.getForm(), is(nullValue()));
        assertThat(indexedReport.getFileName(), is(report.getFileName()));
    }

    private class ReportStoreImpl extends ReportStore {
    }
}
