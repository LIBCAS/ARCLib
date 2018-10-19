package helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.cas.lib.core.config.ObjectMapperProducer;
import cz.cas.lib.core.index.solr.SolrDatedStore;
import cz.cas.lib.core.index.solr.SolrDomainStore;
import cz.cas.lib.core.index.solr.SolrNamedStore;
import cz.cas.lib.core.index.solr.util.MySolrJConverter;
import cz.cas.lib.core.store.DomainStore;
import lombok.Getter;
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

    @Getter
    private HttpSolrClient client;
    @Getter
    private SolrTemplate template;
    @Getter
    private SolrTemplate arclibXmlSolrTemplate;

    @BeforeClass
    public static void classSetUp() throws Exception {
        DbTest.classSetUp();
    }

    @AfterClass
    public static void classTearDown() throws Exception {
        DbTest.classTearDown();
    }

    @Before
    public void testSetUp() throws Exception {
        super.testSetUp();

        ObjectMapper objectMapper = objectMapperProducer.objectMapper(false, false);

        client = new HttpSolrClient("http://localhost:8983/solr");

        template = new SolrTemplate(client);
        template.setSchemaCreationFeatures(Collections.singletonList(SolrPersistentEntitySchemaCreator.Feature.CREATE_MISSING_FIELDS));

        MySolrJConverter mySolrJConverter = new MySolrJConverter();
        template.setSolrConverter(mySolrJConverter);

        template.afterPropertiesSet();

        arclibXmlSolrTemplate = new SolrTemplate(client);
        arclibXmlSolrTemplate.afterPropertiesSet();

        client.deleteByQuery("arclib_xml_test", "*:*");
        client.commit("arclib_xml_test");
        client.deleteByQuery("test", "*:*");
        client.commit("test");
    }

    @After
    public void testTearDown() throws Exception {
        super.testTearDown();

        client.close();
    }

    @Override
    public void initializeStores(DomainStore... stores) {
        for (DomainStore store : stores) {
            store.setEntityManager(super.getEm());
            store.setQueryFactory(new JPAQueryFactory(super.getEm()));
            if (store instanceof SolrDomainStore) {
                ((SolrDomainStore) store).setTemplate(getTemplate());
                continue;
            }
            if (store instanceof SolrDatedStore) {
                ((SolrDatedStore) store).setTemplate(getTemplate());
                continue;
            }
            if (store instanceof SolrNamedStore) {
                ((SolrNamedStore) store).setTemplate(getTemplate());
            }

        }
    }
}
