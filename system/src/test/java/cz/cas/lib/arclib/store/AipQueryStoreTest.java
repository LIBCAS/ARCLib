package cz.cas.lib.arclib.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.arclib.domain.AipQuery;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedAipState;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlDocument;
import cz.cas.lib.core.index.dto.*;
import cz.cas.lib.core.util.ApplicationContextUtils;
import helper.DbTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Collections;

import static cz.cas.lib.core.util.Utils.asList;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;

public class AipQueryStoreTest extends DbTest {

    private AipQueryStore store;
    @Mock
    private ApplicationContextUtils utils;

    @Before
    public void setUp() {
        store = new AipQueryStore();
        initializeStores(store);
        org.springframework.context.ApplicationContext applicationContextMock = Mockito.mock(org.springframework.context.ApplicationContext.class);
        Mockito.when(applicationContextMock.getBean(eq(ObjectMapper.class))).thenReturn(new ObjectMapper());
        new ApplicationContextUtils().setApplicationContext(applicationContextMock);
    }

    @Test
    public void storeTest() {
        AipQuery query = new AipQuery();
        query.setName("q");
        Params params = new Params();
        params.addFilter(new Filter("field", FilterOperation.EQ, "value", asList()));
        params.setPageSize(10);
        params.addSorting(new SortSpecification("blah", Order.ASC));
        query.setQuery(params);
        Result<IndexedArclibXmlDocument> result = new Result<>();
        IndexedArclibXmlDocument indexedArclibXmlDocument = new IndexedArclibXmlDocument();
        indexedArclibXmlDocument.setAipState(IndexedAipState.ARCHIVED);
        indexedArclibXmlDocument.setFields(Collections.singletonMap("key", "value"));
        result.setItems(asList(indexedArclibXmlDocument));
        result.setCount(5L);
        query.setResult(result);
        store.save(query);
        AipQuery fromDb = store.find(query.getId());
        assertThat(fromDb, is(query));
        assertThat(fromDb.getResult().getItems().get(0), instanceOf(IndexedArclibXmlDocument.class));
        assertThat(fromDb.getResult().getItems().get(0).getFields().get("key"), is("value"));
    }
}
