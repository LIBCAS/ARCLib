package cz.inqool.uas.report;

import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.inqool.uas.exception.MissingObject;
import cz.inqool.uas.file.FileRef;
import cz.inqool.uas.file.FileRefStore;
import cz.inqool.uas.file.FileRepository;
import cz.inqool.uas.report.form.ReportForm;
import cz.inqool.uas.report.form.ReportForms;
import cz.inqool.uas.report.location.ReportLocation;
import cz.inqool.uas.report.location.ReportLocations;
import cz.inqool.uas.transformer.TikaTransformer;
import cz.inqool.uas.transformer.tika.TikaProvider;
import helper.DbTest;
import org.apache.tika.exception.TikaException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;

import static cz.inqool.uas.util.Utils.*;
import static helper.ThrowableAssertion.assertThrown;
import static java.nio.file.Files.createDirectories;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

public class ReportStoreTest extends DbTest {
    private ReportStore store;

    private FileRepository repository;

    private FileRefStore fileStore;

    @Mock
    private ElasticsearchTemplate template;

    @Before
    public void setUp() throws IOException, TikaException, SAXException {
        ReportLocation location1 = new ReportLocation() {
            @Override
            public String getId() {
                return "myLocation";
            }

            @Override
            public String getName() {
                return null;
            }
        };

        ReportLocation location2 = new ReportLocation() {
            @Override
            public String getId() {
                return "otherLocation";
            }

            @Override
            public String getName() {
                return null;
            }
        };

        ReportLocations locations = new ReportLocations();
        locations.setLocations(asList(location1, location2));

        ReportForm form = new ReportForm() {
            @Override
            public String getId() {
                return "myForm";
            }

            @Override
            public String getName() {
                return null;
            }
        };

        ReportForms forms = new ReportForms();
        forms.setForms(asList(form));

        MockitoAnnotations.initMocks(this);

        fileStore = new FileRefStore();
        fileStore.setEntityManager(getEm());
        fileStore.setQueryFactory(new JPAQueryFactory(getEm()));
        fileStore.setTemplate(template);

        createDirectories(Paths.get("fileTestFiles"));

        TikaProvider tikaProvider = new TikaProvider();
        TikaTransformer transformer = new TikaTransformer();
        transformer.setTika(tikaProvider.tika());

        repository = new FileRepository();
        repository.setStore(fileStore);
        repository.setBasePath("fileTestFiles");
        repository.setTransformer(transformer);

        store = new ReportStore();
        store.setEntityManager(getEm());
        store.setQueryFactory(new JPAQueryFactory(getEm()));
        store.setTemplate(template);
        store.setLocations(locations);
        store.setForms(forms);
    }

    @After
    public void tearDown() throws IOException {
        delete("fileTestFiles");
    }

    @Test
    public void findByLocationTest() {

        Report report1 = new Report();
        report1.setCreated(Instant.now());
        report1.setUpdated(Instant.now().plusSeconds(5));
        report1.setActive(true);
        report1.setName("myName");
        report1.setProvider("myProvider");
        report1.setParams("myParams");
        report1.setLocation("myLocation");
        report1.setForm("myForm");
        report1.setFileName("myFileName");

        Report report2 = new Report();
        report2.setCreated(Instant.now());
        report2.setUpdated(Instant.now().plusSeconds(5));
        report2.setActive(true);
        report2.setName("myName");
        report2.setProvider("myProvider");
        report2.setParams("myParams");
        report2.setLocation("myLocation");
        report2.setForm("myForm");
        report2.setFileName("myFileName");

        Report report3 = new Report();
        report3.setCreated(Instant.now());
        report3.setUpdated(Instant.now().plusSeconds(5));
        report3.setActive(true);
        report3.setName("myName");
        report3.setProvider("myProvider");
        report3.setParams("myParams");
        report3.setLocation("fake");
        report3.setForm("myForm");
        report3.setFileName("myFileName");

        Report report4 = new Report();
        report4.setCreated(Instant.now());
        report4.setUpdated(Instant.now().plusSeconds(5));
        report4.setActive(true);
        report4.setName("myName");
        report4.setProvider("myProvider");
        report4.setParams("myParams");
        report4.setLocation("otherLocation");
        report4.setForm("myForm");
        report4.setFileName("myFileName");

        Report report5 = new Report();
        report5.setCreated(Instant.now());
        report5.setUpdated(Instant.now().plusSeconds(5));
        report5.setActive(false);
        report5.setName("myName");
        report5.setProvider("myProvider");
        report5.setParams("myParams");
        report5.setLocation("otherLocation");
        report5.setForm("myForm");
        report5.setFileName("myFileName");

        store.save(asSet(report1, report2, report3, report4, report5));
        flushCache();

        List<Report> reports = store.findByLocation("myLocation");
        assertThat(reports, is(notNullValue()));
        assertThat(reports, hasSize(2));
        assertThat(reports, containsInAnyOrder(report1, report2));

        reports = store.findByLocation("otherLocation");
        assertThat(reports, is(notNullValue()));
        assertThat(reports, hasSize(1));
        assertThat(reports, containsInAnyOrder(report4));

        assertThrown(() -> store.findByLocation("fake"))
                .isInstanceOf(MissingObject.class);
    }

    @Test
    public void saveTest() throws IOException {
        InputStream stream = resource("cz/inqool/uas/report/template.docx");

        FileRef fileRef = repository.create(stream, "template.docx", SupportedType.DOCX.getContentType(), false);
        stream.close();

        Report report = new Report();
        report.setTemplate(fileRef);
        report.setCreated(Instant.now());
        report.setUpdated(Instant.now().plusSeconds(5));
        report.setActive(true);
        report.setName("myName");
        report.setProvider("myProvider");
        report.setParams("myParams");
        report.setLocation("myLocation");
        report.setForm("myForm");
        report.setFileName("myFileName");

        store.save(report);

        Report found = store.find(report.getId());

        assertThat(found, is(report));
        assertThat(found.getTemplate(), is(fileRef));
    }
}
