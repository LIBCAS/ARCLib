package helper;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.inqool.uas.config.ElasticsearchTemplateProducer;
import cz.inqool.uas.config.ObjectMapperProducer;
import cz.inqool.uas.file.IndexedFileRef;
import org.elasticsearch.test.ESIntegTestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;

import javax.persistence.EntityManager;

@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class EsDbTest extends ESIntegTestCase {
    private DbTest dbTest = new DbTest() {};

    private ObjectMapperProducer objectMapperProducer = new ObjectMapperProducer();

    private ElasticsearchTemplateProducer templateProducer = new ElasticsearchTemplateProducer();

    private ElasticsearchTemplate template;

    @BeforeClass
    public static void classSetUp() {
        DbTest.classSetUp();
    }

    @AfterClass
    public static void classTearDown() {
        DbTest.classTearDown();
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();

        dbTest.testSetUp();

        ObjectMapper objectMapper = objectMapperProducer.objectMapper(false, false);
        this.template = templateProducer.elasticsearchTemplate(internalCluster().masterClient(), objectMapper);

        getTemplate().createIndex(IndexedFileRef.class);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();

        dbTest.testTearDown();

        template.getClient().close();
    }

    protected EntityManager getEm() {
        return dbTest.getEm();
    }

    protected ElasticsearchTemplate getTemplate() {
        return template;
    }

    @Override
    protected int numberOfReplicas() {
        return 1;
    }

    @Override
    protected int numberOfShards() {
        return 1;
    }

    protected String[] indices() {
        return new String[]{"uas"};
    }

    protected void flushCache() {
        dbTest.flushCache();

        flushAndRefresh(indices());
    }
}
