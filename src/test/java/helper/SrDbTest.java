package helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.inqool.uas.config.ObjectMapperProducer;
import cz.inqool.uas.index.solr.util.MySolrJConverter;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.schema.SolrPersistentEntitySchemaCreator;

import java.util.Collections;

public class SrDbTest extends DbTest {

    private ObjectMapperProducer objectMapperProducer = new ObjectMapperProducer();

    private HttpSolrClient client;

    private SolrTemplate template;

    @BeforeClass
    public static void classSetUp() {
        DbTest.classSetUp();
    }

    @AfterClass
    public static void classTearDown() {
        DbTest.classTearDown();
    }

    @Before
    public void testSetUp() throws Exception {
        super.testSetUp();

        ObjectMapper objectMapper = objectMapperProducer.objectMapper(false, false);

        client = new HttpSolrClient("http://localhost:8983/solr");
        classSetUp();


        template = new SolrTemplate(client);
        template.setSchemaCreationFeatures(Collections.singletonList(SolrPersistentEntitySchemaCreator.Feature.CREATE_MISSING_FIELDS));

        MySolrJConverter converter = new MySolrJConverter();
        template.setSolrConverter(converter);

        template.afterPropertiesSet();
    }

    @After
    public void testTearDown() throws Exception {
        super.testTearDown();

        client.close();
    }

    public HttpSolrClient getClient() {
        return client;
    }

    public SolrTemplate getTemplate() {
        return template;
    }
}
