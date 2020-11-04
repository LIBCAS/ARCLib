package cz.cas.lib.arclib.index;

import com.querydsl.core.types.Order;
import cz.cas.lib.arclib.api.AipApi;
import cz.cas.lib.arclib.domain.AipQuery;
import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.index.solr.IndexQueryUtils;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedAipState;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlDocument;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlStore;
import cz.cas.lib.arclib.init.SolrTestRecordsInitializer;
import cz.cas.lib.arclib.security.authorization.data.Permissions;
import cz.cas.lib.arclib.security.authorization.data.UserRole;
import cz.cas.lib.arclib.security.user.UserDetailsImpl;
import cz.cas.lib.arclib.store.AipQueryStore;
import cz.cas.lib.arclib.store.ProducerStore;
import cz.cas.lib.arclib.store.UserStore;
import cz.cas.lib.core.index.dto.Filter;
import cz.cas.lib.core.index.dto.FilterOperation;
import helper.ApiTest;
import helper.DbTest;
import helper.TransformerFactoryWorkaroundTest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlDocument.*;
import static cz.cas.lib.arclib.init.SolrTestRecordsInitializer.PRODUCER_ID;
import static cz.cas.lib.arclib.init.SolrTestRecordsInitializer.USER_ID;
import static helper.ThrowableAssertion.assertThrown;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
public class IndexIntegrationTest extends TransformerFactoryWorkaroundTest implements ApiTest {

    private Producer producer = new Producer(PRODUCER_ID);
    private User user = new User(USER_ID, producer, Set.of(new UserRole("ADMIN", "Admin role", Set.of(Permissions.ADMIN_PRIVILEGE))));
    private UserDetailsImpl userDetailsImpl = new UserDetailsImpl(user);
    public static final String XML1_ID = "ARCLIB_900000003";
    public static final String XML2_ID = "ARCLIB_900000004";
    public static final String XML3_ID = "ARCLIB_900000005";

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
        api.setUserDetails(userDetailsImpl);
        ((IndexedArclibXmlStore) api.getIndexArclibXmlStore()).setUserDetails(userDetailsImpl);
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
        assertThrown(() -> indexArclibXmlStore.createIndex(arclibXml.getBytes(), PRODUCER_ID, "", "", null, false, true)).isInstanceOf(BadArgument.class);
    }

    @Test
    public void testCreateIndex() throws Exception {
        String arclibXml = new String(Files.readAllBytes(Paths.get("src/main/resources/sampleData/8b/2e/fa/8b2efafd-b637-4b97-a8f7-1b97dd4ee622_xml_2")), StandardCharsets.UTF_8);
        indexArclibXmlStore.createIndex(arclibXml.getBytes(), "otherproducer", "", "", null, false, true);
        SimpleQuery q = new SimpleQuery();
        q.addCriteria(Criteria.where(IndexedArclibXmlDocument.PRODUCER_ID).in("otherproducer").and("type").in("Periodical"));
        List<IndexedArclibXmlDocument> content = solrTemplate.query(coreName, q, IndexedArclibXmlDocument.class).getContent();
        assertThat(content, hasSize(1));
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
                .param("queryName", "test query"))
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
     * ignored: there are currently no multivalue fields
     */
    @Ignore
    @Test
    public void testQueryMultiValues() throws Exception {
        String arclibXml = new String(Files.readAllBytes(Paths.get("system/src/main/resources/sampleData/8b/2e/fa/8b2efafd-b637-4b97-a8f7-1b97dd4ee622_xml_2")), StandardCharsets.UTF_8);
        indexArclibXmlStore.createIndex(arclibXml.getBytes(), PRODUCER_ID, "", "", null, false, false);
        mvc(api).perform(get("/api/aip/list")
                .param("filter[0].field", "dublin_core")
                .param("filter[0].operation", "CONTAINS")
                .param("filter[0].value", "model:periodicalvolume")
                .param("filter[1].field", "dublin_core")
                .param("filter[1].operation", "CONTAINS")
                .param("filter[1].value", "model:periodicalitem")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)));
    }

    /**
     * Tests that field with multiple values can be queried.
     */
    @Test
    public void nestedQuery() throws Exception {
        String arclibXml = new String(Files.readAllBytes(Paths.get("src/test/resources/arclibXmls/validSimple.xml")), StandardCharsets.UTF_8);
        indexArclibXmlStore.createIndex(arclibXml.getBytes(), PRODUCER_ID, "", "", null, false, false);
        mvc(api).perform(get("/api/aip/list")
                .param("filter[0].field", "premis_event")
                .param("filter[0].operation", "NESTED")
                .param("filter[0].filter[0].field", "premis_event_type")
                .param("filter[0].filter[0].operation", "EQ")
                .param("filter[0].filter[0].value", "transfer")
                .param("filter[0].filter[1].field", "premis_event_detail")
                .param("filter[0].filter[1].operation", "EQ")
                .param("filter[0].filter[1].value", "validation detail")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));

        mvc(api).perform(get("/api/aip/list")
                .param("filter[0].field", "premis_event")
                .param("filter[0].operation", "NESTED")
                .param("filter[0].filter[0].field", "premis_event_type")
                .param("filter[0].filter[0].operation", "EQ")
                .param("filter[0].filter[0].value", "transfer")
                .param("filter[0].filter[1].field", "premis_event_detail")
                .param("filter[0].filter[1].operation", "EQ")
                .param("filter[0].filter[1].value", "transfer detail")
                .param("filter[1].field", "premis_event")
                .param("filter[1].operation", "NESTED")
                .param("filter[1].filter[0].field", "premis_event_type")
                .param("filter[1].filter[0].operation", "EQ")
                .param("filter[1].filter[0].value", "validation")
                .param("filter[1].filter[1].field", "premis_event_detail")
                .param("filter[1].filter[1].operation", "EQ")
                .param("filter[1].filter[1].value", "validation detail")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)));
    }

    /**
     * Tests that documents of other producer are not listed.
     */
    @Test
    public void testQueryMultiProducers() throws Exception {
        SolrInputDocument doc = createDocument(
                "uuid",
                new Date(),
                "producerName",
                "producerId",
                "userName",
                "authorialId",
                "sipId",
                1,
                1,
                "initial version",
                "initial version",
                "doc",
                null,
                true,
                IndexedArclibXmlDocument.MAIN_INDEX_TYPE_VALUE,
                new HashMap<>());
        solrTemplate.saveDocument(coreName, doc);
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
                .param("sort", IndexedArclibXmlDocument.CREATED)
                .param("order", Order.DESC.toString())

                .param("filter[0].field", IndexedArclibXmlDocument.CREATED)
                .param("filter[0].operation", "GT")
                .param("filter[0].value", "2018-03-08T10:59:00Z")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(5)));

        mvc(api).perform(get("/api/aip/list")
                .param("sort", IndexedArclibXmlDocument.CREATED)
                .param("order", Order.DESC.toString())

                .param("filter[0].field", IndexedArclibXmlDocument.CREATED)
                .param("filter[0].operation", "LT")
                .param("filter[0].value", "2018-03-08T10:59:00Z")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", empty()));
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

    @Test
    public void testUuidSearch() throws Exception {
        mvc(api).perform(get("/api/aip/list")
                .param("filter[0].field", IndexedArclibXmlDocument.CONTENT)
                .param("filter[0].operation", "CONTAINS")
                .param("filter[0].value", "7033d800-0935-11e4-beed-5ef3fc9ae860")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", empty()));

        mvc(api).perform(get("/api/aip/list")
                .param("filter[0].field", IndexedArclibXmlDocument.CONTENT)
                .param("filter[0].operation", "CONTAINS")
                .param("filter[0].value", "7033d800-0935-11e4-beed-5ef3fc9ae867")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", not(empty())));
    }

    @Test
    public void testUrnSearch() throws Exception {
        mvc(api).perform(get("/api/aip/list")
                .param("filter[0].field", IndexedArclibXmlDocument.CONTENT)
                .param("filter[0].operation", "CONTAINS")
                .param("filter[0].value", "urn:nbn:cz:nk-nonono")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", empty()));

        mvc(api).perform(get("/api/aip/list")
                .param("filter[0].field", IndexedArclibXmlDocument.CONTENT)
                .param("filter[0].operation", "CONTAINS")
                .param("filter[0].value", "urn:nbn:cz:nk-0016ke")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", not(empty())));
    }

    @Test
    public void testTrailingCommaSearch() throws Exception {
        mvc(api).perform(get("/api/aip/list")
                .param("filter[0].field", IndexedArclibXmlDocument.CONTENT)
                .param("filter[0].operation", "CONTAINS")
                .param("filter[0].value", "richard novak,")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", not(empty())));

        mvc(api).perform(get("/api/aip/list")
                .param("filter[0].field", IndexedArclibXmlDocument.CONTENT)
                .param("filter[0].operation", "CONTAINS")
                .param("filter[0].value", "richard novak")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", not(empty())));
    }

    private SolrInputDocument createDocument(String id, Date created, String producerId, String producerName, String userName, String authorialId, String sipId, Integer sipVersionNumber, Integer xmlVersionNumber, String sipVersionOf, String xmlVersionOf, String document, IndexedAipState aipState, Boolean debugMode, String indexType, Map<String, Object> fields) {
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField(ID, id);
        doc.addField(IndexedArclibXmlDocument.CREATED, created);
        doc.addField(IndexedArclibXmlDocument.PRODUCER_ID, producerId);
        doc.addField(PRODUCER_NAME, producerName);
        doc.addField(USER_NAME, userName);
        doc.addField(AUTHORIAL_ID, authorialId);
        doc.addField(SIP_ID, sipId);
        doc.addField(SIP_VERSION_NUMBER, sipVersionNumber);
        doc.addField(XML_VERSION_NUMBER, xmlVersionNumber);
        doc.addField(SIP_VERSION_OF, sipVersionOf);
        doc.addField(XML_VERSION_OF, xmlVersionOf);
        doc.addField(CONTENT, document);
        if (aipState != null)
            doc.addField(AIP_STATE, aipState.toString());
        doc.addField(DEBUG_MODE, debugMode);
        doc.addField(IndexQueryUtils.TYPE_FIELD, indexType);
        return doc;
    }
}
