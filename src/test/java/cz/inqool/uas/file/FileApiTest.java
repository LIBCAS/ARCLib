package cz.inqool.uas.file;

import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.inqool.uas.transformer.TikaTransformer;
import cz.inqool.uas.transformer.tika.TikaProvider;
import helper.ApiTest;
import helper.DbTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.file.Paths;

import static java.nio.file.Files.createDirectories;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FileApiTest extends DbTest implements ApiTest {

    private FileApi api;

    private FileRepository repository;

    private FileRefStore store;

    @Mock
    private ElasticsearchTemplate template;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        store = new FileRefStore();
        store.setTemplate(template);
        store.setQueryFactory(new JPAQueryFactory(getEm()));
        store.setEntityManager(getEm());

        createDirectories(Paths.get("fileTestFiles"));

        TikaProvider tikaProvider = new TikaProvider();
        TikaTransformer transformer = new TikaTransformer();
        transformer.setTika(tikaProvider.tika());

        repository = new FileRepository();
        repository.setStore(store);
        repository.setTransformer(transformer);
        repository.setBasePath("fileTestFiles");

        api = new FileApi();
        api.setRepository(repository);
    }

    @Test
    public void uploadTest() throws Exception {
        /*
        Short file path
         */
        MockMultipartFile shortFilePathMultiPartFile = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Spring Framework".getBytes());

        mvc(api).
                perform(MockMvcRequestBuilders.fileUpload("/api/files/").file(shortFilePathMultiPartFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test.txt"));

        /*
        Full file path
        */
        MockMultipartFile fullFilePathMultiPartFile = new MockMultipartFile(
                "file", "test/path/test.txt", "text/plain", "Spring Framework".getBytes());

        mvc(api).
                perform(MockMvcRequestBuilders.fileUpload("/api/files/").file(fullFilePathMultiPartFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test.txt"));


        MockMultipartFile fullFilePathMultiPartFile2 = new MockMultipartFile(
                "file", "test\\path\\test.txt", "text/plain", "Spring Framework".getBytes());

        mvc(api).
                perform(MockMvcRequestBuilders.fileUpload("/api/files/").file(fullFilePathMultiPartFile2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test.txt"));
    }
}
