package cz.inqool.uas.file;

import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.inqool.uas.exception.BadArgument;
import cz.inqool.uas.exception.ForbiddenObject;
import cz.inqool.uas.exception.MissingObject;
import cz.inqool.uas.transformer.TikaTransformer;
import cz.inqool.uas.transformer.tika.TikaProvider;
import helper.DbTest;
import org.apache.commons.io.IOUtils;
import org.apache.tika.exception.TikaException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.util.FileSystemUtils;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;

import static cz.inqool.uas.util.Utils.resource;
import static helper.ThrowableAssertion.assertThrown;
import static java.nio.file.Files.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class FileRepositoryTest extends DbTest {
    private FileRefStore store;

    private FileRepository repository;

    @Mock
    private ElasticsearchTemplate template;

    @Before
    public void setUp() throws IOException, TikaException, SAXException {
        MockitoAnnotations.initMocks(this);

        store = new FileRefStore();
        store.setEntityManager(getEm());
        store.setQueryFactory(new JPAQueryFactory(getEm()));
        store.setTemplate(template);

        createDirectories(Paths.get("fileTestFiles"));

        TikaProvider tikaProvider = new TikaProvider();
        TikaTransformer transformer = new TikaTransformer();
        transformer.setTika(tikaProvider.tika());

        repository = new FileRepository();
        repository.setStore(store);
        repository.setBasePath("fileTestFiles");
        repository.setTransformer(transformer);
    }

    @After
    public void tearDown() throws IOException {
        delete("fileTestFiles");
    }

    @Test
    public void saveTest() throws IOException {
        try (InputStream stream = resource("cz/inqool/uas/file/test.pdf")) {
            FileRef pdfFile = repository.create(stream, "test.pdf", "application/pdf", false);

            assertThat(pdfFile, is(notNullValue()));
            assertThat(pdfFile.getName(), is("test.pdf"));
            assertThat(pdfFile.getContentType(), is("application/pdf"));
        }
    }

    @Test
    public void saveMissingParameterTest() throws IOException {
        try (InputStream stream = resource("cz/inqool/uas/file/test.pdf")) {
            assertThrown(() -> repository.create(null, "test.pdf", "application/pdf", false))
                    .isInstanceOf(BadArgument.class);

            assertThrown(() -> repository.create(stream, null, "application/pdf", false))
                    .isInstanceOf(BadArgument.class);

            assertThrown(() -> repository.create(stream, "test.pdf", null, false))
                    .isInstanceOf(BadArgument.class);

        }
    }

    @Test
    public void saveNotExistingFolderTest() throws IOException {
        FileRepository repository = new FileRepository();
        repository.setStore(store);
        repository.setBasePath("fileTestFiles2");

        try (InputStream stream = resource("cz/inqool/uas/file/test.pdf")) {
            FileRef pdfFile = repository.create(stream, "test.pdf", "application/pdf", false);

            assertThat(pdfFile, is(notNullValue()));
            assertThat(pdfFile.getName(), is("test.pdf"));
            assertThat(pdfFile.getContentType(), is("application/pdf"));
        }

        delete("fileTestFiles2");
    }

    @Test
    public void saveToExistingFileTest() throws IOException {
        createFile(Paths.get("fileTestFiles3"));

        FileRepository repository = new FileRepository();
        repository.setStore(store);
        repository.setBasePath("fileTestFiles3");

        try (InputStream stream = resource("cz/inqool/uas/file/test.pdf")) {
            assertThrown(() -> repository.create(stream, "test.pdf", "application/pdf", false))
                    .isInstanceOf(ForbiddenObject.class);
        }

        delete("fileTestFiles3");
    }

    @Test
    public void getTest() throws IOException {
        String id;
        try (InputStream stream = resource("cz/inqool/uas/file/test.pdf")) {
            FileRef pdfFile = repository.create(stream, "test.pdf", "application/pdf", false);
            id = pdfFile.getId();
        }

        FileRef fileRef = repository.get(id);
        assertThat(fileRef, is(notNullValue()));
        assertThat(fileRef.getName(), is("test.pdf"));
        assertThat(fileRef.getContentType(), is("application/pdf"));
        assertThat(fileRef.getSize(), is(greaterThan(0L)));

        try (InputStream stream = resource("cz/inqool/uas/file/test.pdf"); InputStream stream2 = fileRef.getStream()) {
            IOUtils.contentEquals(stream, stream2);
        }
    }

    @Test
    public void getMissingIdTest() throws IOException {
        assertThrown(() -> repository.get(null))
                .isInstanceOf(BadArgument.class);

    }

    @Test
    public void getMissingFileTest() throws IOException {
        assertThrown(() -> repository.get(UUID.randomUUID().toString()))
                .isInstanceOf(MissingObject.class);
    }

    @Test
    public void getMissingDataTest() throws IOException {
        String id;
        try (InputStream stream = resource("cz/inqool/uas/file/test.pdf")) {
            FileRef pdfFile = repository.create(stream, "test.pdf", "application/pdf", false);
            id = pdfFile.getId();
        }

        delete("fileTestFiles/" + id);

        assertThrown(() -> repository.get(id))
                .isInstanceOf(MissingObject.class);

    }

    @Test
    public void getRefTest() throws IOException {
        String id;
        try (InputStream stream = resource("cz/inqool/uas/file/test.pdf")) {
            FileRef pdfFile = repository.create(stream, "test.pdf", "application/pdf", false);
            id = pdfFile.getId();
        }

        FileRef fileRef = repository.getRef(id);
        assertThat(fileRef, is(notNullValue()));
        assertThat(fileRef.getName(), is("test.pdf"));
        assertThat(fileRef.getContentType(), is("application/pdf"));
        assertThat(fileRef.getSize(), is(nullValue()));
        assertThat(fileRef.getStream(), is(nullValue()));
    }

    @Test
    public void getRefMissingFileTest() throws IOException {
        assertThrown(() -> repository.getRef(UUID.randomUUID().toString()))
                .isInstanceOf(MissingObject.class);
    }

    @Test
    public void resetTest() throws IOException {
        String id;
        try (InputStream stream = resource("cz/inqool/uas/file/test.pdf")) {
            FileRef pdfFile = repository.create(stream, "test.pdf", "application/pdf", false);
            id = pdfFile.getId();
        }

        FileRef fileRef = repository.get(id);
        try (InputStream stream = resource("cz/inqool/uas/file/test.pdf"); InputStream stream2 = fileRef.getStream()) {
            IOUtils.contentEquals(stream, stream2);
        }

        repository.reset(fileRef);
        try (InputStream stream = resource("cz/inqool/uas/file/test.pdf"); InputStream stream2 = fileRef.getStream()) {
            IOUtils.contentEquals(stream, stream2);
        }
    }

    @Test
    public void resetClosePreviousStreamTest() throws IOException {
        String id;
        try (InputStream stream = resource("cz/inqool/uas/file/test.pdf")) {
            FileRef pdfFile = repository.create(stream, "test.pdf", "application/pdf", false);
            id = pdfFile.getId();
        }

        FileRef fileRef = repository.get(id);
        InputStream stream = fileRef.getStream();
        repository.reset(fileRef);

        assertThrown(stream::read)
                .isInstanceOf(IOException.class);
    }

    @Test
    public void resetMissingDataTest() throws IOException {
        String id;
        try (InputStream stream = resource("cz/inqool/uas/file/test.pdf")) {
            FileRef pdfFile = repository.create(stream, "test.pdf", "application/pdf", false);
            id = pdfFile.getId();
        }

        FileRef fileRef = repository.get(id);
        repository.close(fileRef);

        delete("fileTestFiles/" + id);

        assertThrown(() -> repository.reset(fileRef))
                .isInstanceOf(MissingObject.class);
    }

    @Test
    public void closeTest() throws IOException {
        String id;
        try (InputStream stream = resource("cz/inqool/uas/file/test.pdf")) {
            FileRef pdfFile = repository.create(stream, "test.pdf", "application/pdf", false);
            id = pdfFile.getId();
        }

        FileRef fileRef = repository.get(id);
        repository.close(fileRef);

        assertThat(fileRef.getStream(), is(nullValue()));
        assertThat(fileRef.getSize(), is(nullValue()));
    }

    @Test
    public void closeNotOpenedTest() throws IOException {
        String id;
        try (InputStream stream = resource("cz/inqool/uas/file/test.pdf")) {
            FileRef pdfFile = repository.create(stream, "test.pdf", "application/pdf", false);
            id = pdfFile.getId();
        }

        FileRef fileRef = repository.getRef(id);
        repository.close(fileRef);

        assertThat(fileRef.getStream(), is(nullValue()));
        assertThat(fileRef.getSize(), is(nullValue()));
    }

    @Test
    public void closeClosedTest() throws IOException {
        String id;
        try (InputStream stream = resource("cz/inqool/uas/file/test.pdf")) {
            FileRef pdfFile = repository.create(stream, "test.pdf", "application/pdf", false);
            id = pdfFile.getId();
        }

        FileRef fileRef = repository.get(id);
        fileRef.getStream().close();

        repository.close(fileRef);

        assertThat(fileRef.getStream(), is(nullValue()));
        assertThat(fileRef.getSize(), is(nullValue()));
    }

    @Test
    public void deleteTest() throws IOException {
        String id;
        try (InputStream stream = resource("cz/inqool/uas/file/test.pdf")) {
            FileRef pdfFile = repository.create(stream, "test.pdf", "application/pdf", false);
            id = pdfFile.getId();
        }

        FileRef fileRef = repository.get(id);
        try (InputStream stream = resource("cz/inqool/uas/file/test.pdf"); InputStream stream2 = fileRef.getStream()) {
            IOUtils.contentEquals(stream, stream2);
        }

        repository.del(fileRef);

        assertThrown(() -> repository.get(id))
                .isInstanceOf(MissingObject.class);
    }

    @Test
    public void deleteMissingFileTest() throws IOException {
        String id;
        try (InputStream stream = resource("cz/inqool/uas/file/test.pdf")) {
            FileRef pdfFile = repository.create(stream, "test.pdf", "application/pdf", false);
            id = pdfFile.getId();
        }

        FileRef fileRef = repository.get(id);
        try (InputStream stream = resource("cz/inqool/uas/file/test.pdf"); InputStream stream2 = fileRef.getStream()) {
            IOUtils.contentEquals(stream, stream2);
        }

        repository.del(fileRef);
        repository.del(fileRef);

        assertThrown(() -> repository.get(id))
                .isInstanceOf(MissingObject.class);
    }

    @Test
    public void deleteMissingFileParameterTest() throws IOException {
        assertThrown(() -> repository.del(null))
                .isInstanceOf(BadArgument.class);
    }

    @Test
    public void indexContentTest() throws IOException {
        try (InputStream stream = resource("cz/inqool/uas/file/test.txt")) {
            FileRef pdfFile = repository.create(stream, "test.txt", "text/plain", true);

            assertThat(pdfFile, is(notNullValue()));
            assertThat(pdfFile.getContentType(), is("text/plain"));
            assertThat(pdfFile.getIndexedContent(), is(true));
        }
    }

    @Test
    public void indexContentUnsupportedTest() throws IOException {
        try (InputStream stream = resource("cz/inqool/uas/file/test.pdf")) {
            FileRef pdfFile = repository.create(stream, "test.pdf", "fake/fake", true);

            assertThat(pdfFile, is(notNullValue()));
            assertThat(pdfFile.getContentType(), is("fake/fake"));
            assertThat(pdfFile.getIndexedContent(), is(false));
        }
    }
}
