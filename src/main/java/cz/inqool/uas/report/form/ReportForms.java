package cz.inqool.uas.report.form;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

/**
 * Report gathering forms manager.
 *
 */
@Service
public class ReportForms {
    private List<ReportForm> forms;

    /**
     * Gets all {@link ReportForm}s.
     *
     * @return {@link List} of {@link ReportForm}
     */
    public List<ReportForm> getForms() {
        return unmodifiableList(forms);
    }

    /**
     * Gets {@link ReportForm} corresponding to specified id.
     *
     * @param formId Specified id
     * @return {@link ReportForm}
     */
    public ReportForm getForm(String formId) {
        return forms.stream()
                    .filter(location -> Objects.equals(location.getId(), formId))
                    .findFirst()
                    .orElse(null);
    }

    /**
     * Tests if there is {@link ReportForm} with specified id.
     *
     * @param formId Specified id to test
     * @return Existence of {@link ReportForm}
     */
    public boolean isValid(String formId) {
        return forms.stream()
                    .map(ReportForm::getId)
                    .anyMatch(id -> Objects.equals(id, formId));
    }

    @Autowired(required = false)
    public void setForms(List<ReportForm> forms) {
        this.forms = forms != null ? forms : emptyList();
    }
}
