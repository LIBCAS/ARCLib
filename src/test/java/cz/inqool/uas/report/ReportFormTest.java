package cz.inqool.uas.report;

import cz.inqool.uas.report.form.ReportForm;
import cz.inqool.uas.report.form.ReportForms;
import cz.inqool.uas.report.location.ReportLocation;
import org.junit.Test;

import static cz.inqool.uas.util.Utils.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ReportFormTest {


    @Test
    public void simpleTest() {
        ReportForm form = new ReportForm() {
            @Override
            public String getId() {
                return "existing";
            }

            @Override
            public String getName() {
                return "Existing form";
            }
        };

        ReportForms forms = new ReportForms();
        forms.setForms(asList(form));

        assertThat(forms.getForms(), hasSize(1));
        assertThat(forms.getForms(), containsInAnyOrder(form));
        assertThat(forms.isValid(form.getId()), is(true));
        assertThat(forms.getForm(form.getId()), is(form));
    }

    @Test
    public void nonExistingTest() {
        ReportLocation nonForm = new ReportLocation() {
            @Override
            public String getId() {
                return "nonExisting";
            }

            @Override
            public String getName() {
                return "NonExisting form";
            }
        };

        ReportForms forms = new ReportForms();
        forms.setForms(asList());

        assertThat(forms.getForms(), hasSize(0));
        assertThat(forms.isValid(nonForm.getId()), is(false));
        assertThat(forms.getForm(nonForm.getId()), is(nullValue()));
    }

    @Test
    public void combinedTest() {
        ReportForm form = new ReportForm() {
            @Override
            public String getId() {
                return "existing";
            }

            @Override
            public String getName() {
                return "Existing form";
            }
        };

        ReportForm nonForm = new ReportForm() {
            @Override
            public String getId() {
                return "nonExisting";
            }

            @Override
            public String getName() {
                return "NonExisting form";
            }
        };

        ReportForms forms = new ReportForms();
        forms.setForms(asList(form));

        assertThat(forms.getForms(), hasSize(1));
        assertThat(forms.getForms(), containsInAnyOrder(form));
        assertThat(forms.isValid(form.getId()), is(true));
        assertThat(forms.getForm(form.getId()), is(form));

        assertThat(forms.isValid(nonForm.getId()), is(false));
        assertThat(forms.getForm(nonForm.getId()), is(nullValue()));
    }
}
