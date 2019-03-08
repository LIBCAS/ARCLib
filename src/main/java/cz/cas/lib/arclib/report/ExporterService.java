package cz.cas.lib.arclib.report;

import cz.cas.lib.core.exception.BadArgument;
import cz.cas.lib.core.exception.GeneralException;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.*;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ExporterService {

    private DataSource ds;

    /**
     * Exports report to specified format and fill given {@link OutputStream} with it
     *
     * @param report       {@link Report entity}
     * @param format       file format to which export
     * @param customParams parameters used to override default parameter values
     * @param os           {@link OutputStream} to be filled with exported file
     */
    public void export(Report report, ExportFormat format, Map<String, String> customParams, OutputStream os) throws IOException {
        JasperPrint jasperPrint;
        JasperReport jasperReport = (JasperReport) report.getCompiledObject();
        log.info(String.format("Exporting: %s to: %s", report.getName(), format.toString()));
        try {
            jasperPrint = JasperFillManager.fillReport(jasperReport, parseParams(customParams, jasperReport), ds.getConnection());
        } catch (SQLException ex) {
            String e = "Error occurred during database access.";
            log.error(e);
            throw new GeneralException(e, ex);
        } catch (JRException ex) {
            String e = "Error occurred during report template filling.";
            log.error(e);
            throw new GeneralException(e, ex);
        }
        Exporter exporter;
        log.debug("Preparing exporter");
        switch (format) {
            case PDF:
                exporter = getPdfExporter();
                exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(os));
                break;
            case XLSX:
                exporter = getXlsExporter();
                exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(os));
                break;
            case CSV:
                exporter = getCsvExporter();
                exporter.setExporterOutput(new SimpleWriterExporterOutput(os));
                break;
            case HTML:
                exporter = getHtmlExporter();
                exporter.setExporterOutput(new SimpleHtmlExporterOutput(os));
                break;
            default:
                throw new IllegalArgumentException("Unsupported export format");
        }
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        try {
            log.debug("Exporting");
            exporter.exportReport();
        } catch (JRException e) {
            throw new GeneralException("Export to " + format + " failed.", e);
        }
    }

    private Exporter getPdfExporter() throws IOException {
        JRPdfExporter exporter = new JRPdfExporter();
        SimplePdfReportConfiguration reportConfig
                = new SimplePdfReportConfiguration();
        reportConfig.setSizePageToContent(true);
        reportConfig.setForceLineBreakPolicy(false);
        SimplePdfExporterConfiguration exportConfig
                = new SimplePdfExporterConfiguration();
        exportConfig.setMetadataAuthor("ARCLib Reporting System");
        exportConfig.setEncrypted(true);
        exporter.setConfiguration(reportConfig);
        exporter.setConfiguration(exportConfig);
        return exporter;
    }

    private Exporter getCsvExporter() {
        return new JRCsvExporter();
    }

    private Exporter getXlsExporter() throws IOException {
        JRXlsxExporter exporter = new JRXlsxExporter();
        SimpleXlsxReportConfiguration reportConfig
                = new SimpleXlsxReportConfiguration();
        exporter.setConfiguration(reportConfig);
        return exporter;
    }

    private Exporter getHtmlExporter() {
        return new HtmlExporter();
    }

    private Map<String, Object> parseParams(Map<String, String> customParams, JasperReport report) {
        log.debug("Parsing parameters");
        Map<String, Object> parsedParams = new HashMap<>();
        List<JRParameter> reportParams = new ArrayList<>();
        for (JRParameter reportParam : report.getParameters()) {
            if (!reportParam.isSystemDefined())
                reportParams.add(reportParam);
        }
        for (String paramName : customParams.keySet()) {
            boolean found = false;
            for (int i = 0; i < reportParams.size(); i++) {
                if (reportParams.get(i).getName().equals(paramName)) {
                    String paramClassName = reportParams.get(i).getValueClassName();
                    parsedParams.put(paramName, parseValue(paramClassName, customParams.get(paramName)));
                    found = true;
                    log.debug(String.format("Param: %s:%s", paramName, paramClassName));
                    break;
                }
            }
            if (!found) {
                String e = String.format("Parameter: %s not defined in report template.", paramName);
                log.warn(e);
                throw new BadArgument(e);
            }
        }
        return parsedParams;
    }

    private Object parseValue(String className, String value) {
        try {
            switch (className) {
                case "java.lang.String":
                    return value;
                case "java.lang.Short":
                    return Short.parseShort(value);
                case "java.lang.Long":
                    return Long.parseLong(value);
                case "java.lang.Integer":
                    return Integer.parseInt(value);
                case "java.lang.Float":
                    return Float.parseFloat(value);
                case "java.lang.Double":
                    return Double.parseDouble(value);
                case "java.lang.Boolean":
                    return Boolean.parseBoolean(value);
                default:
                    String e = String.format("Unsupported parameter type: %s", className);
                    log.error(e);
                    throw new IllegalArgumentException(e);
            }
        } catch (NumberFormatException ex) {
            String e = String.format("Can't parse: %s as: %s type", value, className);
            log.warn(e);
            throw new BadArgument(e);
        }
    }

    @Inject
    public void setDS(DataSource ds) {
        this.ds = ds;
    }
}
