package cz.cas.lib.arclib.report;

import cz.cas.lib.core.exception.BadArgument;
import cz.cas.lib.core.exception.ConflictException;
import cz.cas.lib.core.exception.GeneralException;
import cz.cas.lib.core.store.DomainStore;
import cz.cas.lib.core.store.Transactional;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import org.apache.commons.io.IOUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Repository;

import javax.persistence.PersistenceException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Repository
public class ReportStore extends DomainStore<Report, QReport> {
    public ReportStore() {
        super(Report.class, QReport.class);
    }

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
     * @param name     template name
     * @param template template file stream
     * @return
     */
    @Transactional
    public String saveReport(String name, InputStream template) throws IOException {
        log.info(String.format("Storing report template: %s", name));
        byte[] templateBytes = IOUtils.toByteArray(template);
        IOUtils.closeQuietly(template);
        JasperReport compiledReport;
        template = new ByteArrayInputStream(templateBytes);
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
        template = new ByteArrayInputStream(templateBytes);
        String id;
        try {
            id = save(new Report(name, IOUtils.toString(template, StandardCharsets.UTF_8), compiledReport)).getId();
        } catch (PersistenceException e) {
            if (e.getCause() instanceof ConstraintViolationException)
                throw new ConflictException("Duplicate name of template:" + name);
            else
                throw e;
        }
        return id;
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
}
