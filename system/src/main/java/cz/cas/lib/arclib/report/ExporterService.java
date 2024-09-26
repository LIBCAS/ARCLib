package cz.cas.lib.arclib.report;

import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.ForbiddenObject;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

import javax.sql.DataSource;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.file.Files.*;

@Service
@Slf4j
public class ExporterService {

    @Resource(name = "SolrArclibXmlDatasource")
    private DataSource solrArclibXmlDs;
    private DataSource arclibSystemDbDs;
    /**
     * Place on filesystem where exported reports are saved to.
     * (if they are exported to filesystem instead of directly to API response)
     */
    private Path reportsDirectory;

    private static final String ONE_PAGE_PER_SHEET_PROPERTY_NAME = "net.sf.jasperreports.export.xls.one.page.per.sheet";
    private static final String ONE_PAGE_PER_SHEET_PROPERTY_VALUE = "true";

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
        jasperReport.setProperty(ONE_PAGE_PER_SHEET_PROPERTY_NAME, ONE_PAGE_PER_SHEET_PROPERTY_VALUE);

        log.info(String.format("Exporting report: %s to format: %s", report.getName(), format.toString()));
        DataSource dataSourceToUse = report.isArclibXmlDs() ? solrArclibXmlDs : arclibSystemDbDs;
        try {
            jasperPrint = JasperFillManager.fillReport(jasperReport, parseParams(customParams, jasperReport), dataSourceToUse.getConnection());
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
        log.debug("Preparing exporter for format:" + format);
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
            log.debug("Exporting...");
            exporter.exportReport();
            log.debug("Exporting succeeded.");
        } catch (JRException e) {
            throw new GeneralException("Export to " + format + " failed.", e);
        }
    }


    /**
     * Exports report with {@link #export} and saves it to application's filesystem.
     *
     * @param report       {@link Report entity}
     * @param format       file format to which export
     * @param customParams parameters used to override default parameter values
     */
    public void exportToFileSystem(Report report, ExportFormat format, Map<String, String> customParams) {
        try {
            if (!isDirectory(reportsDirectory) && exists(reportsDirectory)) {
                throw new ForbiddenObject(Path.class, reportsDirectory.toString());
            } else if (!isDirectory(reportsDirectory)) {
                createDirectories(reportsDirectory);
            }

            Path reportPath = reportsDirectory.resolve(report.getId() + format.getExtension());
            this.export(report, format, customParams, new BufferedOutputStream(Files.newOutputStream(reportPath)));
        } catch (IOException e) {
            throw new GeneralException("Export has failed with IOException", e);
        }
    }

    /**
     * Obtains reference to exported report file
     *
     * @param report for which to obtain File reference
     * @param format of exported report
     * @return reference to specific exported report File
     * @throws GeneralException when reports directory does not exists.
     * @throws MissingObject    if expected file for report + format does not exists
     */
    public File getExportedReportFile(Report report, ExportFormat format) {
        if (!exists(reportsDirectory) || !isDirectory(reportsDirectory)) {
            throw new GeneralException("Directory with exported reports does not exists.");
        }

        Path reportPath = reportsDirectory.resolve(report.getId() + format.getExtension());
        if (!exists(reportPath) || !isRegularFile(reportPath)) {
            throw new MissingObject(Path.class, reportPath.toString());
        }

        return reportPath.toFile();
    }

    /**
     * Deletes exported report file from filesystem if exists
     *
     * @param report for which to remove file from filesystem
     * @param format for matching extension of exported file
     */
    public void deleteExportedReportFile(Report report, ExportFormat format) {
        Path reportPath = reportsDirectory.resolve(report.getId() + format.getExtension());
        try {
            log.debug("Deleting exported report file: " + reportPath.toString());
            Files.deleteIfExists(reportPath);
        } catch (IOException e) {
            throw new UncheckedIOException("Exported report file deletion has failed", e);
        }
    }

    private Exporter getPdfExporter() {
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

    private Exporter getXlsExporter() {
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

    @Autowired
    public ExporterService(DataSource arclibSystemDbDs) {
        this.arclibSystemDbDs = arclibSystemDbDs;
    }

    @Autowired
    public void setReportsDirectory(@Value("${arclib.path.reports}") Path reportsDirectory) {
        this.reportsDirectory = reportsDirectory;
    }
}
