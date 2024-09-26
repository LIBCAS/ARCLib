package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.index.autocomplete.AutoCompleteItem;
import cz.cas.lib.arclib.report.Report;
import cz.cas.lib.arclib.report.ReportStore;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import cz.cas.lib.core.store.Transactional;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import static cz.cas.lib.arclib.domainbase.util.DomainBaseUtils.notNull;

@Service
@Slf4j
public class ReportService {

    private ReportStore store;

    /**
     * Compile template and stores it text form as well as compiled form to the database.
     * Validate type of template custom parameters if there are any. If the parameter type or its default value is invalid throws Exception.
     * Supported types:
     * <ul>
     * <li>java.lang.String</li>
     * <li>java.lang.Short</li>
     * <li>java.lang.Long</li>
     * <li>java.lang.Integer</li>
     * <li>java.lang.Float</li>
     * <li>java.lang.Double</li>
     * <li>java.lang.Boolean</li>
     * </ul>
     *
     * @param report report
     * @return report
     */
    @Transactional
    public Report saveReport(Report report) throws IOException {
        log.info(String.format("Storing report template: %s", report.getName()));
        JasperReport compiledReport;
        InputStream template = new ByteArrayInputStream(report.getTemplate().getBytes());
        try {
            compiledReport = JasperCompileManager.compileReport(template);
        } catch (JRException ex) {
            String e = "Error occurred during report template compilation.";
            log.error(e);
            throw new GeneralException(e, ex);
        }
        for (JRParameter param : compiledReport.getParameters()) {
            validateParameter(param);
        }
        report.setCompiledObject(compiledReport);
        return store.save(report);
    }

    /**
     * updates report, null arguments are not updated
     */
    @Transactional
    public void delete(String id) {
        log.info(String.format("Deleting report template: %s", id));
        Report report = find(id);
        store.delete(report);
    }

    /**
     * @param id id of the report
     * @return report
     * @throws MissingObject if does not exist or deleted
     */
    public Report find(String id) {
        Report report = store.find(id);
        notNull(report, () -> new MissingObject(Report.class, id));
        return report;
    }

    public Collection<Report> findAll() {
        return store.findAll();
    }

    public List<Report> findAllInList(List<String> ids) {
        return store.findAllInList(ids);
    }

    public Result<AutoCompleteItem> listAutoComplete(Params params) {
        return store.listAutoComplete(params);
    }

    private void validateParameter(JRParameter param) {
        if (param.isSystemDefined())
            return;
        JRExpression val = param.getDefaultValueExpression();
        //set value which does not throw NumberFormatException if param value is null or empty
        String defaultValueString = "0";
        if (val != null)
            defaultValueString = (val.getText() == null || val.getText().trim().isEmpty()) ? "0" : val.getText();
        try {
            switch (param.getValueClassName()) {
                case "java.lang.Short":
                    Short.parseShort(defaultValueString);
                    break;
                case "java.lang.Long":
                    Long.parseLong(defaultValueString);
                    break;
                case "java.lang.Integer":
                    Integer.parseInt(defaultValueString);
                    break;
                case "java.lang.Float":
                    Float.parseFloat(defaultValueString);
                    break;
                case "java.lang.Double":
                    Double.parseDouble(defaultValueString);
                    break;
                case "java.lang.Boolean":
                case "java.lang.String":
                    break;
                default:
                    String e = String.format("Found parameter of unsupported type: %s", param.getValueClassName());
                    log.warn(e);
                    throw new BadArgument(e);
            }
        } catch (NumberFormatException ex) {
            String e = String.format("Can't parse default value: %s as: %s type", defaultValueString, param.getValueClassName());
            log.warn(e);
            throw new BadArgument(e);
        }
    }

    @Autowired
    public void setStore(ReportStore store) {
        this.store = store;
    }

}
