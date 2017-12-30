package cz.cas.lib.arclib.index;

import cz.cas.lib.arclib.api.IndexApi;
import cz.inqool.uas.exception.BadArgument;
import helper.ApiTest;
import org.apache.commons.lang3.SystemUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import static helper.ThrowableAssertion.assertThrown;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SolrIntegrationTest implements ApiTest {

    private static final String CMD = SystemUtils.IS_OS_WINDOWS ? "solr.cmd" : "solr";
    private static final String SIP_ID = "2fe5c72e-824d-4a78-9507-0842c559726b";
    private static final int XML_VERSION = 11;
    private static final String XML_ID = SIP_ID + "_" + XML_VERSION;
    private static final String ENDPOINT = "http://localhost:8983/solr/arclib_xml";
    private static String arclibXml;

    @Inject
    private IndexApi api;

    @Inject
    private IndexStore indexStore;

    @Inject
    private static SolrClient solrClient;

    /**
     * Stops all Solr instances and starts a new one. Path environment variable must contain path to <i>solr</i> binary.
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws SolrServerException
     */
    @BeforeClass
    public static void beforeClass() throws IOException, InterruptedException, SolrServerException {
        new ProcessBuilder(CMD, "stop", "-all").start().waitFor();
        Process startSolrProcess = new ProcessBuilder(CMD, "start").start();
        if (startSolrProcess.waitFor() != 0) {
            String err;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(startSolrProcess.getErrorStream()))) {
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();
                while (line != null) {
                    sb.append(line);
                    line = br.readLine();
                }
                err = sb.toString();
            }
            throw new IllegalStateException(String.format("Unable to start solr server: %s", err));
        }
        solrClient = new HttpSolrClient(ENDPOINT);
        arclibXml = new String(Files.readAllBytes(Paths.get("src/test/resources/index/arclibXml.xml")), StandardCharsets.UTF_8);
    }

    @Before
    public void beforeTest() throws IOException, SolrServerException, InterruptedException {
        solrClient.deleteByQuery("id:" + XML_ID);
        solrClient.commit();
        indexStore.createIndex(SIP_ID, XML_VERSION, arclibXml);
    }

    /**
     * Tests that 400 (BAD_REQUEST) is returned when filter contains field which is not defined in Solr schema.
     */
    @Test
    public void testQueryUndefinedField() throws Exception {
        mvc(api).perform(get("/api/list")
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
        mvc(api).perform(get("/api/list")
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
        String arclibXml = new String(Files.readAllBytes(Paths.get("src/test/resources/index/invalidValue.xml")), StandardCharsets.UTF_8);
        assertThrown(() -> indexStore.createIndex(UUID.randomUUID().toString(), 50, arclibXml)).isInstanceOf(BadArgument.class);
    }

    /**
     * Tests that documents are correctly filtered and therefore no document is retrieved with this query.
     */
    @Test
    public void testQueryNoResult() throws Exception {
        mvc(api).perform(get("/api/list")
                .param("filter[0].field", "id")
                .param("filter[0].operation", "EQ")
                .param("filter[0].value", "blah")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    /**
     * Tests that field can be queried case-insensitively.
     */
    @Test
    public void testQueryIdCaseInsensitive() throws Exception {
        mvc(api).perform(get("/api/list")
                .param("filter[0].field", "id")
                .param("filter[0].operation", "EQ")
                .param("filter[0].value", XML_ID.substring(0, 10).toLowerCase() + XML_ID.substring(10).toUpperCase())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(XML_ID))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    /**
     * Tests that field with multiple values can be queried.
     */
    @Test
    public void testQueryMultiValues() throws Exception {
        mvc(api).perform(get("/api/list")
                .param("filter[0].field", "event_type")
                .param("filter[0].operation", "EQ")
                .param("filter[0].value", "validation")

                .param("filter[1].field", "event_type")
                .param("filter[1].operation", "EQ")
                .param("filter[1].value", "ingestion")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0]").value(XML_ID));
    }

    /**
     * Tests that DATETIME field can be queried with ranges.
     */
    @Test
    public void testQueryDateRanges() throws Exception {
        mvc(api).perform(get("/api/list")
                .param("filter[0].field", "arc_event_date")
                .param("filter[0].operation", "GT")
                .param("filter[0].value", "2000-01-20")

                .param("filter[1].field", "arc_event_date")
                .param("filter[1].operation", "LT")
                .param("filter[1].value", "2020-01-20")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0]").value(XML_ID));
    }

    /**
     * Tests that AND/OR queries does work for sub-filters.
     */
    @Test
    public void testQueryInternal() throws Exception {
        mvc(api).perform(get("/api/list")
                .param("filter[0].operation", "AND")

                .param("filter[0].filter[0].field", "id")
                .param("filter[0].filter[0].operation", "ENDWITH")
                .param("filter[0].filter[0].value", "_" + XML_VERSION)

                .param("filter[0].filter[1].field", "id")
                .param("filter[0].filter[1].operation", "STARTWITH")
                .param("filter[0].filter[1].value", XML_ID.substring(0, 10))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0]").value(XML_ID));

        mvc(api).perform(get("/api/list")
                .param("filter[0].operation", "OR")

                .param("filter[0].filter[0].field", "id")
                .param("filter[0].filter[0].operation", "EQ")
                .param("filter[0].filter[0].value", "blah")

                .param("filter[0].filter[1].field", "id")
                .param("filter[0].filter[1].operation", "EQ")
                .param("filter[0].filter[1].value", "blahblah")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
