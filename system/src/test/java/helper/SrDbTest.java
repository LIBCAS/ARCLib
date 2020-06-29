package helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.cas.lib.arclib.domainbase.store.DomainStore;
import cz.cas.lib.core.config.ObjectMapperProducer;
import cz.cas.lib.core.index.solr.IndexedDatedStore;
import cz.cas.lib.core.index.solr.IndexedDomainStore;
import cz.cas.lib.core.index.solr.IndexedNamedStore;
import lombok.Getter;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.schema.SolrPersistentEntitySchemaCreator;

import java.util.Collections;
import java.util.Properties;

public class SrDbTest extends DbTest {


    private ObjectMapperProducer objectMapperProducer = new ObjectMapperProducer();

    @Getter
    private HttpSolrClient client;
    @Getter
    private SolrTemplate template;
    @Getter
    private SolrTemplate arclibXmlSolrTemplate;
    protected String arclibXmlCoreName;
    protected String testCoreName;


    @BeforeClass
    public static void classSetUp() throws Exception {
        DbTest.classSetUp();
    }

    @AfterClass
    public static void classTearDown() throws Exception {
        DbTest.classTearDown();
    }

    /**
     * should have different name than its subclass or the method of subclass is not called
     *
     * @throws Exception
     */
    @Before
    public void testSetUpp() throws Exception {
        Properties props = new Properties();
        props.load(ClassLoader.getSystemResourceAsStream("application.properties"));
        arclibXmlCoreName = props.getProperty("solr.arclibxml.corename");
        testCoreName = props.getProperty("solr.test.corename");
        ObjectMapper objectMapper = objectMapperProducer.objectMapper(false, false);

        client = new HttpSolrClient.Builder().withBaseSolrUrl("http://localhost:8983/solr").build();

        template = new SolrTemplate(client);
        template.setSchemaCreationFeatures(Collections.singletonList(SolrPersistentEntitySchemaCreator.Feature.CREATE_MISSING_FIELDS));
        template.afterPropertiesSet();

        arclibXmlSolrTemplate = new SolrTemplate(client);
        arclibXmlSolrTemplate.afterPropertiesSet();
    }

    /**
     * should have different name than its subclass or the method of subclass is not called
     *
     * @throws Exception
     */
    @After
    public void testTearDownn() throws Exception {
        client.deleteByQuery(testCoreName, "*:*");
        client.commit(testCoreName);
        client.deleteByQuery(arclibXmlCoreName, "*:*");
        client.commit(arclibXmlCoreName);
        client.close();
    }

    @Override
    public void initializeStores(DomainStore... stores) {
        for (DomainStore store : stores) {
            store.setEntityManager(super.getEm());
            store.setQueryFactory(new JPAQueryFactory(super.getEm()));
            if (store instanceof IndexedDomainStore) {
                ((IndexedDomainStore) store).setTemplate(getTemplate());
                ((IndexedDomainStore) store).init();
                continue;
            }
            if (store instanceof IndexedDatedStore) {
                ((IndexedDatedStore) store).setTemplate(getTemplate());
                ((IndexedDatedStore) store).init();
                continue;
            }
            if (store instanceof IndexedNamedStore) {
                ((IndexedNamedStore) store).setTemplate(getTemplate());
                ((IndexedNamedStore) store).init();
            }

        }
    }
}
