package cz.inqool.uas.report;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.inqool.uas.exception.BadArgument;
import cz.inqool.uas.exception.MissingAttribute;
import cz.inqool.uas.exception.MissingObject;
import cz.inqool.uas.export.DocxExporter;
import cz.inqool.uas.export.PdfExporter;
import cz.inqool.uas.export.PptxExporter;
import cz.inqool.uas.export.XlsxExporter;
import cz.inqool.uas.file.FileRef;
import cz.inqool.uas.file.FileRepository;
import cz.inqool.uas.report.exception.UnsupportedTemplateException;
import cz.inqool.uas.report.provider.ReportProvider;
import cz.inqool.uas.report.provider.ReportProviders;
import cz.inqool.uas.service.Templater;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.Map;

import static cz.inqool.uas.util.Utils.notNull;

/**
 * Report generator
 */
@Service
public class ReportGenerator {
    private ReportStore store;

    private FileRepository repository;

    private PdfExporter pdfExporter;

    private XlsxExporter xlsxExporter;

    private DocxExporter docxExporter;

    private PptxExporter pptxExporter;

    private ObjectMapper mapper;

    private ReportProviders providers;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
                                                               .withZone(ZoneId.systemDefault());
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                                                               .withZone(ZoneId.systemDefault());
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                                                                   .withZone(ZoneId.systemDefault());

    private Templater templater;

    /**
     * Generates {@link Report} specified by id and {@link Map} of params and returns it to the caller.
     *
     * <p>
     *     Report generation contains three stages of params supplying:
     * </p>
     * <ol>
     *     <li>Optional user supplied through ui</li>
     *     <li>Optional report globally supplied</li>
     *     <li>Optional provided by selected {@link ReportProvider}</li>
     * </ol>
     *
     * @param reportId Id of the {@link Report} to generate
     * @param params User supplied parameters
     * @return {@link GeneratedReport} containing generated content
     * @throws UnsupportedTemplateException If template file has unsupported content type
     */
    public GeneratedReport generate(String reportId, Map<String, Object> params) {
        notNull(reportId, () -> new IllegalArgumentException("reportId"));

        Report report = store.find(reportId);
        notNull(report, () -> new MissingObject(Report.class, reportId));

        Map<String, Object> allParams = gatherParameters(report, params);

        return generateInternal(report, allParams);
    }

    /**
     * Generates {@link Report} specified by id and {@link Map} of params and stores it as a file in
     * {@link FileRepository}.
     *
     * <p>
     *     Report generation contains three stages of params supplying:
     * </p>
     * <ol>
     *     <li>Optional user supplied through ui</li>
     *     <li>Optional report globally supplied</li>
     *     <li>Optional provided by selected {@link ReportProvider}</li>
     * </ol>
     *
     * <p>
     *     User can also specify if the content indexing should happen.
     * </p>
     *
     * @param reportId Id of the {@link Report} to generate
     * @param index Should the content be indexed
     * @param params User supplied parameters
     * @return {@link FileRef} containing reference to generated content
     * @throws UnsupportedTemplateException If template file has unsupported content type
     */
    public FileRef generateToFile(String reportId, Map<String, Object> params, boolean index) {

        GeneratedReport generatedReport = generate(reportId, params);

        ByteArrayInputStream contentStream = new ByteArrayInputStream(generatedReport.getContent());

        return repository.create(contentStream, generatedReport.getName(),
                generatedReport.getContentType(), index);
    }

    private Map<String, Object> gatherParameters(Report report, Map<String, Object> params) {
        Map<String, Object> allParams = new HashMap<>();

        if (params != null) {
            allParams.putAll(params);
        }

        // overwrite user supplied parameters with global set
        if (report.getParams() != null) {
            try {
                HashMap<String, Object> globalParams = mapper.readValue(
                        report.getParams(),
                        new TypeReference<Map<String, Object>>() {}
                );

                allParams.putAll(globalParams);
            } catch (IOException ex) {
                throw new BadArgument(ex);
            }
        }

        // add helpers
        allParams.put("dateFormatter", dateFormatter);
        allParams.put("timeFormatter", timeFormatter);
        allParams.put("dateTimeFormatter", dateTimeFormatter);

        // call report provider
        if (report.getProvider() != null) {
            ReportProvider provider = providers.getProvider(report.getProvider());
            notNull(provider, () -> new MissingAttribute(report, "provider"));
            return provider.provide(allParams);
        } else {
            return allParams;
        }
    }

    private GeneratedReport generateInternal(Report report, Map<String, Object> params) {
        FileRef template = report.getTemplate();
        notNull(template, () -> new MissingAttribute(report, "template"));

        SupportedType type = SupportedType.getSupported(template.getContentType());
        notNull(type, () -> new UnsupportedTemplateException(template.getContentType()));

        byte[] content;
        try {
            repository.reset(template);

            switch (type) {
                case DOCX:
                    content = generateDocx(template.getStream(), params);
                    break;
                case PPTX:
                    content = generatePptx(template.getStream(), params);
                    break;
                case XSLX:
                    content = generateXlsx(template.getStream(), params);
                    break;
                default:
                case HTML:
                    content = generatePdf(template.getStream(), params);
                    break;
            }
        } finally {
            repository.close(template);
        }

        String fileName = templater.transform(report.getFileName(), params);

        return new GeneratedReport(content, type.getResultType(), fileName);
    }

    private byte[] generatePdf(InputStream template, Map<String, Object> params) {
        return pdfExporter.export(template, params);
    }

    private byte[] generateXlsx(InputStream template, Map<String, Object> params) {
        return xlsxExporter.export(template, params);
    }

    private byte[] generateDocx(InputStream template, Map<String, Object> params) {
        return docxExporter.export(template, params);
    }

    private byte[] generatePptx(InputStream template, Map<String, Object> params) {
        return pptxExporter.export(template, params);
    }

    @Inject
    public void setStore(ReportStore store) {
        this.store = store;
    }

    @Inject
    public void setRepository(FileRepository repository) {
        this.repository = repository;
    }

    @Inject
    public void setPdfExporter(PdfExporter pdfExporter) {
        this.pdfExporter = pdfExporter;
    }

    @Inject
    public void setXlsxExporter(XlsxExporter xlsxExporter) {
        this.xlsxExporter = xlsxExporter;
    }

    @Inject
    public void setDocxExporter(DocxExporter docxExporter) {
        this.docxExporter = docxExporter;
    }

    @Inject
    public void setMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Inject
    public void setProviders(ReportProviders providers) {
        this.providers = providers;
    }

    @Inject
    public void setPptxExporter(PptxExporter pptxExporter) {
        this.pptxExporter = pptxExporter;
    }

    @Inject
    public void setTemplater(Templater templater) {
        this.templater = templater;
    }
}
