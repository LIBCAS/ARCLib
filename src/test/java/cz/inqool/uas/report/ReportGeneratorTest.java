package cz.inqool.uas.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.inqool.uas.config.ObjectMapperProducer;
import cz.inqool.uas.exception.MissingAttribute;
import cz.inqool.uas.exception.MissingObject;
import cz.inqool.uas.export.DocxExporter;
import cz.inqool.uas.export.PdfExporter;
import cz.inqool.uas.export.XlsxExporter;
import cz.inqool.uas.file.FileRef;
import cz.inqool.uas.file.FileRepository;
import cz.inqool.uas.report.exception.UnsupportedTemplateException;
import cz.inqool.uas.service.Templater;
import cz.inqool.uas.transformer.TikaTransformer;
import cz.inqool.uas.transformer.tika.TikaProvider;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.util.FileSystemUtils;
import org.xml.sax.SAXException;

import java.io.*;
import java.nio.file.*;
import java.util.Map;

import static cz.inqool.uas.util.Utils.asMap;
import static cz.inqool.uas.util.Utils.resource;
import static helper.ThrowableAssertion.assertThrown;
import static java.nio.file.Files.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReportGeneratorTest {

    @Mock
    private FileRepository repository;

    private DocxExporter docxExporter;

    private XlsxExporter xlsxExporter;

    private PdfExporter pdfExporter;

    private ObjectMapper mapper;

    private TikaTransformer transformer;

    private Templater templater;

    @Before
    public void setUp() throws IOException, TikaException, SAXException {
        MockitoAnnotations.initMocks(this);

        createDirectories(Paths.get("fileTestFiles"));

        docxExporter = new DocxExporter();

        xlsxExporter = new XlsxExporter();

        VelocityEngine engine = new VelocityEngine();
        engine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        engine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        engine.init();

        templater = new Templater();
        templater.setEngine(engine);

        pdfExporter = new PdfExporter();
        pdfExporter.setTemplater(templater);

        ObjectMapperProducer objectMapperProducer = new ObjectMapperProducer();
        mapper = objectMapperProducer.objectMapper(false, false);

        TikaProvider provider = new TikaProvider();
        Tika tika = provider.tika();

        transformer = new TikaTransformer();
        transformer.setTika(tika);
    }

    @After
    public void tearDown() throws IOException {
        delete("fileTestFiles");
    }

    @Test
    public void docxTestWithParams() throws IOException {
        docxTest(asMap("param", "value"));
    }

    @Test
    public void docxTestWithoutParams() throws IOException {
        docxTest(null);
    }

    private void docxTest(Map<String, Object> params) throws IOException {
        FileRef fileRef = docxReport();

        Report report = new Report();
        report.setTemplate(fileRef);
        report.setFileName("report.docx");

        ReportStore store = mock(ReportStore.class);
        when(store.find(report.getId())).thenReturn(report);

        ReportGenerator generator = new ReportGenerator();
        generator.setStore(store);
        generator.setRepository(repository);
        generator.setDocxExporter(docxExporter);
        generator.setTemplater(templater);

        GeneratedReport generated = generator.generate(report.getId(), params);

        assertThat(generated, is(notNullValue()));
        assertThat(generated.name, is(report.getFileName()));
        assertThat(generated.contentType, is(SupportedType.DOCX.getResultType()));
        assertThat(generated.content, is(notNullValue()));

        fileRef.getStream().close();
    }

    @Test
    public void xlsxTestWithParams() throws IOException {
        xlsxTest(asMap("param", "value"));
    }

    @Test
    public void xlsxTestWithoutParams() throws IOException {
        xlsxTest(null);
    }

    private void xlsxTest(Map<String, Object> params) throws IOException {
        FileRef fileRef = xlsxReport();

        Report report = new Report();
        report.setTemplate(fileRef);
        report.setFileName("report.xlsx");

        ReportStore store = mock(ReportStore.class);
        when(store.find(report.getId())).thenReturn(report);

        ReportGenerator generator = new ReportGenerator();
        generator.setStore(store);
        generator.setRepository(repository);
        generator.setXlsxExporter(xlsxExporter);
        generator.setTemplater(templater);

        GeneratedReport generated = generator.generate(report.getId(), params);

        assertThat(generated, is(notNullValue()));
        assertThat(generated.name, is(report.getFileName()));
        assertThat(generated.contentType, is(SupportedType.XSLX.getResultType()));
        assertThat(generated.content, is(notNullValue()));

        fileRef.getStream().close();
    }

    @Test
    public void pdfTestWithParams() throws IOException {
        pdfTest(asMap("param", "value"));
    }

    @Test
    public void pdfTestWithoutParams() throws IOException {
        pdfTest(null);
    }

    public void pdfTest(Map<String, Object> params) throws IOException {
        FileRef fileRef = htmlReport();

        Report report = new Report();
        report.setTemplate(fileRef);
        report.setFileName("report.pdf");

        ReportStore store = mock(ReportStore.class);
        when(store.find(report.getId())).thenReturn(report);

        ReportGenerator generator = new ReportGenerator();
        generator.setStore(store);
        generator.setRepository(repository);
        generator.setPdfExporter(pdfExporter);
        generator.setTemplater(templater);

        GeneratedReport generated = generator.generate(report.getId(), params);

        assertThat(generated, is(notNullValue()));
        assertThat(generated.name, is(report.getFileName()));
        assertThat(generated.contentType, is(SupportedType.HTML.getResultType()));
        assertThat(generated.content, is(notNullValue()));

        fileRef.getStream().close();
    }

    @Test
    public void missingIdTest() throws IOException {
        ReportStore store = mock(ReportStore.class);

        ReportGenerator generator = new ReportGenerator();
        generator.setStore(store);
        generator.setRepository(repository);
        generator.setDocxExporter(docxExporter);

        assertThrown(() -> generator.generate(null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void missingReportTest() throws IOException {
        ReportStore store = mock(ReportStore.class);
        when(store.find("id")).thenReturn(null);

        ReportGenerator generator = new ReportGenerator();
        generator.setStore(store);
        generator.setRepository(repository);
        generator.setDocxExporter(docxExporter);

        assertThrown(() -> generator.generate("id", null))
                .isInstanceOf(MissingObject.class);
    }

    @Test
    public void missingTemplate() throws IOException {

        Report report = new Report();
        report.setFileName("report.docx");

        ReportStore store = mock(ReportStore.class);
        when(store.find(report.getId())).thenReturn(report);

        ReportGenerator generator = new ReportGenerator();
        generator.setStore(store);
        generator.setRepository(repository);

        assertThrown(() -> generator.generate(report.getId(), null))
                .isInstanceOf(MissingAttribute.class);
    }

    @Test
    public void unsupportedTest() throws IOException {
        FileRef fileRef = unsupportedReport();

        Report report = new Report();
        report.setTemplate(fileRef);
        report.setFileName("report.txt");

        ReportStore store = mock(ReportStore.class);
        when(store.find(report.getId())).thenReturn(report);

        ReportGenerator generator = new ReportGenerator();
        generator.setStore(store);
        generator.setRepository(repository);

        assertThrown(() -> generator.generate(report.getId(), null))
                .isInstanceOf(UnsupportedTemplateException.class);

        fileRef.getStream().close();
    }

    @Test
    public void reportParamsTest() throws IOException {
        FileRef fileRef = docxReport();

        Report report = new Report();
        report.setTemplate(fileRef);
        report.setParams("{\"param\": \"global\"}");
        report.setFileName("report.docx");

        ReportStore store = mock(ReportStore.class);
        when(store.find(report.getId())).thenReturn(report);

        ReportGenerator generator = new ReportGenerator();
        generator.setStore(store);
        generator.setRepository(repository);
        generator.setDocxExporter(docxExporter);
        generator.setMapper(mapper);
        generator.setTemplater(templater);

        GeneratedReport generated = generator.generate(report.getId(), null);

        assertThat(generated, is(notNullValue()));
        assertThat(generated.name, is(report.getFileName()));
        assertThat(generated.contentType, is(SupportedType.DOCX.getResultType()));
        assertThat(generated.content, is(notNullValue()));

        fileRef.getStream().close();

        checkDocxContent(generated.content, "global");
    }

    @Test
    public void paramTest() throws IOException {
        FileRef fileRef = docxReport();

        Report report = new Report();
        report.setTemplate(fileRef);
        report.setFileName("report.docx");

        ReportStore store = mock(ReportStore.class);
        when(store.find(report.getId())).thenReturn(report);

        ReportGenerator generator = new ReportGenerator();
        generator.setStore(store);
        generator.setRepository(repository);
        generator.setDocxExporter(docxExporter);
        generator.setTemplater(templater);

        GeneratedReport generated = generator.generate(report.getId(), asMap("param", "local"));

        assertThat(generated, is(notNullValue()));
        assertThat(generated.name, is(report.getFileName()));
        assertThat(generated.contentType, is(SupportedType.DOCX.getResultType()));
        assertThat(generated.content, is(notNullValue()));

        fileRef.getStream().close();

        checkDocxContent(generated.getContent(), "local");
    }

    @Test
    public void overrideTest() throws IOException {
        FileRef fileRef = docxReport();

        Report report = new Report();
        report.setTemplate(fileRef);
        report.setParams("{\"param\": \"global\"}");
        report.setFileName("report.docx");

        ReportStore store = mock(ReportStore.class);
        when(store.find(report.getId())).thenReturn(report);

        ReportGenerator generator = new ReportGenerator();
        generator.setStore(store);
        generator.setRepository(repository);
        generator.setDocxExporter(docxExporter);
        generator.setMapper(mapper);
        generator.setTemplater(templater);

        GeneratedReport generated = generator.generate(report.getId(), asMap("param", "local"));

        assertThat(generated, is(notNullValue()));
        assertThat(generated.name, is(report.getFileName()));
        assertThat(generated.contentType, is(SupportedType.DOCX.getResultType()));
        assertThat(generated.content, is(notNullValue()));

        fileRef.getStream().close();

        checkDocxContent(generated.getContent(), "global");
    }

    private void checkDocxContent(byte[] document, String assertedValue) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        transformer.transform(SupportedType.DOCX.getContentType(), "text/plain", new ByteArrayInputStream(document), out);

        String result = new String(out.toByteArray(), "UTF-8");
        assertThat(result, is(assertedValue));
    }

    private static void delete(String path) throws IOException {
        File file = new File(path);
        FileSystemUtils.deleteRecursively(file);
    }


    private FileRef docxReport() throws IOException {
        InputStream stream = resource("cz/inqool/uas/report/template.docx");
        FileRef fileRef = new FileRef();
        fileRef.setStream(stream);
        fileRef.setContentType(SupportedType.DOCX.getContentType());
        return fileRef;
    }

    private FileRef xlsxReport() throws IOException {
        InputStream stream = resource("cz/inqool/uas/report/template.xlsx");
        FileRef fileRef = new FileRef();
        fileRef.setStream(stream);
        fileRef.setContentType(SupportedType.XSLX.getContentType());
        return fileRef;
    }

    private FileRef htmlReport() throws IOException {
        InputStream stream = resource("cz/inqool/uas/report/template.html");
        FileRef fileRef = new FileRef();
        fileRef.setStream(stream);
        fileRef.setContentType(SupportedType.HTML.getContentType());
        return fileRef;
    }

    private FileRef unsupportedReport() throws IOException {
        InputStream stream = resource("cz/inqool/uas/report/template.txt");
        FileRef fileRef = new FileRef();
        fileRef.setStream(stream);
        fileRef.setContentType("text/plain");
        return fileRef;
    }
}
