package cz.cas.lib.arclib.index;

import com.querydsl.core.types.Order;
import cz.cas.lib.arclib.api.AipApi;
import cz.cas.lib.arclib.domain.AipQuery;
import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlDocumentState;
import cz.cas.lib.arclib.index.solr.arclibxml.SolrArclibXmlDocument;
import cz.cas.lib.arclib.index.solr.arclibxml.SolrArclibXmlStore;
import cz.cas.lib.arclib.init.SolrTestRecordsInitializer;
import cz.cas.lib.arclib.security.authorization.Roles;
import cz.cas.lib.arclib.security.user.UserDelegate;
import cz.cas.lib.arclib.store.AipQueryStore;
import cz.cas.lib.arclib.store.ProducerStore;
import cz.cas.lib.arclib.store.UserStore;
import cz.cas.lib.core.exception.BadArgument;
import cz.cas.lib.core.index.dto.Filter;
import cz.cas.lib.core.index.dto.FilterOperation;
import helper.ApiTest;
import helper.DbTest;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static cz.cas.lib.arclib.init.SolrTestRecordsInitializer.*;
import static cz.cas.lib.core.util.Utils.asList;
import static helper.ThrowableAssertion.assertThrown;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SolrIntegrationTest implements ApiTest {

    private Producer producer = new Producer(PRODUCER_ID);
    private User user = new User(USER_ID, producer);
    private UserDelegate userDelegate = new UserDelegate(user,asList(new SimpleGrantedAuthority(Roles.ADMIN)));

    @Value("${solr.arclibxml.corename}")
    private String coreName;
    @Resource(name = "ArclibXmlSolrTemplate")
    private SolrTemplate solrTemplate;
    @Inject
    private AipApi api;
    @Inject
    private IndexArclibXmlStore indexArclibXmlStore;
    @Inject
    private UserStore userStore;
    @Inject
    private ProducerStore producerStore;
    @Inject
    private AipQueryStore aipQueryStore;
    @Inject
    private SolrTestRecordsInitializer solrTestRecordsInitializer;

    /**
     * Stops all Solr instances and starts a new one. Path environment variable must contain path to <i>solr</i> binary.
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws SolrServerException
     */
    @BeforeClass
    public static void beforeClass() throws Exception {
    }

    @AfterClass
    public static void classTearDown() throws Exception {
        DbTest.classTearDown();
    }

    @Before
    public void beforeTest() throws IOException, SolrServerException, InterruptedException {
        solrTestRecordsInitializer.init();
        producerStore.save(producer);
        userStore.save(user);
        api.setUserDetails(userDelegate);
        ((SolrArclibXmlStore) api.getIndexArclibXmlStore()).setUserDetails(userDelegate);
    }

    /**
     * Tests that 400 (BAD_REQUEST) is returned when filter contains field which is not defined in Solr schema.
     */
    @Test
    public void testQueryUndefinedField() throws Exception {
        mvc(api).perform(get("/api/aip/list")
                .param("filter[0].field", "blah")
                .param("filter[0].operation", "STARTWITH")
                .param("filter[0].value", "someid"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Tests that 400 (BAD_REQUEST) is returned when filter contains unsupported operation.
     */
    @Test
    public void testQueryUndefinedOperation() throws Exception {
        mvc(api).perform(get("/api/aip/list")
                .param("filter[0].field", "id")
                .param("filter[0].operation", "blah")
                .param("filter[0].value", "someid"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Tests that 400 (BAD_REQUEST) is returned when XML file contains value which should be DATE/DATETIME/TIME but
     * cannot be parsed.
     */
    @Test
    public void testCreateIndexInvalidFieldValue() throws Exception {
        String arclibXml = new String(Files.readAllBytes(Paths.get("src/test/resources/arclibXmls/invalidValue.xml")), StandardCharsets.UTF_8);
        assertThrown(() -> indexArclibXmlStore.createIndex(arclibXml, PRODUCER_ID, USER_ID, IndexedArclibXmlDocumentState.PROCESSED)).isInstanceOf(BadArgument.class);
    }

    @Test
    public void testCreateIndex() throws Exception {
        String arclibXml = new String(Files.readAllBytes(Paths.get("src/main/resources/sampleData/arclibXml4.xml")), StandardCharsets.UTF_8);
        indexArclibXmlStore.createIndex(arclibXml, "otherproducer", "otheruser", IndexedArclibXmlDocumentState.PROCESSED);
        SimpleQuery q = new SimpleQuery();
        q.addCriteria(Criteria.where(SolrArclibXmlDocument.PRODUCER_ID).in("otherproducer").and("premis_event_type").in("validation"));
        List<SolrArclibXmlDocument> content = solrTemplate.query(coreName, q, SolrArclibXmlDocument.class).getContent();
        assertThat(content, hasSize(1));
        assertThat(content.get(0).getCreated().toInstant(), equalTo(Instant.parse("2018-07-30T11:54:38Z")));
    }

    /**
     * Tests that documents are correctly filtered and therefore no document is retrieved with this query.
     */
    @Test
    public void testQueryNoResult() throws Exception {
        mvc(api).perform(get("/api/aip/list")
                .param("filter[0].field", "id")
                .param("filter[0].operation", "EQ")
                .param("filter[0].value", "blah")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    public void testQuerySaveResult() throws Exception {
        mvc(api).perform(get("/api/aip/list")
                .param("filter[0].field", "id")
                .param("filter[0].operation", "NEQ")
                .param("filter[0].value", "blah")
                .param("save", "true")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(5)));
        List<AipQuery> all = aipQueryStore.findQueriesOfUser(USER_ID);
        assertThat(all, hasSize(1));
        AipQuery savedQuery = all.get(0);
        assertThat(savedQuery.getQuery().getFilter(), hasSize(2));
        Filter filter = savedQuery.getQuery().getFilter().get(0).getFilter().get(0);
        assertThat(filter.getOperation(), is(FilterOperation.NEQ));
        assertThat(filter.getField(), is("id"));
        assertThat(savedQuery.getResult().getItems(), hasSize(5));
    }


    /**
     * Tests that field with multiple values can be queried.
     */
    @Test
    public void testQueryMultiValues() throws Exception {
        mvc(api).perform(get("/api/aip/list")
                .param("filter[0].field", "premis_event_type")
                .param("filter[0].operation", "EQ")
                .param("filter[0].value", "validation")

                .param("filter[1].field", "premis_event_type")
                .param("filter[1].operation", "EQ")
                .param("filter[1].value", "ingestion")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)));
    }

    /**
     * Tests that documents of other producer are not listed.
     */
    @Test
    public void testQueryMultiProducers() throws Exception {
        SolrArclibXmlDocument doc = new SolrArclibXmlDocument("uuid", new Date(), "producerId", "userId", "authorialId", "sipId", 1, 1, "initial version", "initial version", "doc", IndexedArclibXmlDocumentState.PROCESSED, new HashMap<>());
        solrTemplate.saveBean(coreName, doc);
        solrTemplate.commit(coreName);

        mvc(api).perform(get("/api/aip/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(5)));
    }


    /**
     * Tests that DATETIME field range filter and sorting.
     */
    @Test
    public void testQueryDateTimeField() throws Exception {
        mvc(api).perform(get("/api/aip/list")
                .param("sort", SolrArclibXmlDocument.CREATED)
                .param("order", Order.DESC.toString())

                .param("filter[0].field", SolrArclibXmlDocument.CREATED)
                .param("filter[0].operation", "GT")
                .param("filter[0].value", "2018-03-08T10:59:00Z")

                .param("filter[1].field", SolrArclibXmlDocument.CREATED)
                .param("filter[1].operation", "LT")
                .param("filter[1].value", "2018-03-08T12:59:00Z")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].id").value(XML3_ID))
                .andExpect(jsonPath("$.items[1].id").value(XML2_ID))
                .andDo(rs -> System.out.println(rs.getResponse().getContentAsString()));
    }

    /**
     * Tests that AND/OR queries does work for sub-filters.
     */
    @Test
    public void testQueryInternal() throws Exception {
        mvc(api).perform(get("/api/aip/list")
                .param("filter[0].operation", "AND")

                .param("filter[0].filter[0].field", "id")
                .param("filter[0].filter[0].operation", "ENDWITH")
                .param("filter[0].filter[0].value", XML1_ID.substring(8))

                .param("filter[0].filter[1].field", "id")
                .param("filter[0].filter[1].operation", "STARTWITH")
                .param("filter[0].filter[1].value", XML1_ID.substring(0, 5))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").value(XML1_ID));

        mvc(api).perform(get("/api/aip/list")
                .param("filter[0].operation", "OR")

                .param("filter[0].filter[0].field", "id")
                .param("filter[0].filter[0].operation", "EQ")
                .param("filter[0].filter[0].value", XML1_ID)

                .param("filter[0].filter[1].field", "id")
                .param("filter[0].filter[1].operation", "EQ")
                .param("filter[0].filter[1].value", XML2_ID)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)));
    }
}
