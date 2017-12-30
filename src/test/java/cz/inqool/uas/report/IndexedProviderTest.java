package cz.inqool.uas.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.inqool.uas.config.ObjectMapperProducer;
import cz.inqool.uas.index.dto.Params;
import cz.inqool.uas.index.dto.Result;
import cz.inqool.uas.report.exception.GeneratingException;
import cz.inqool.uas.report.index.IndexedStoreImpl;
import cz.inqool.uas.report.index.ReportTestEntity;
import cz.inqool.uas.report.provider.DataAdapterProvider;
import cz.inqool.uas.store.DomainStore;
import helper.EsDbTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;

import java.util.Map;

import static helper.ThrowableAssertion.assertThrown;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Mockito.when;

public class IndexedProviderTest extends EsDbTest {
    private DataAdapterProvider provider = new DataAdapterProvider();

    private IndexedStoreImpl store;

    @Mock
    private ApplicationContext context;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        MockitoAnnotations.initMocks(this);

        ObjectMapperProducer producer = new ObjectMapperProducer();
        ObjectMapper mapper = producer.objectMapper(false, false);

        provider.setApplicationContext(context);
        provider.setObjectMapper(mapper);

        store = new IndexedStoreImpl();
        store.setEntityManager(getEm());
        store.setQueryFactory(new JPAQueryFactory(getEm()));
        store.setTemplate(getTemplate());

        when(context.getBean(IndexedStoreImpl.class)).thenReturn(store);
    }

    @Test
    public void emptyTest() {
        store.reindex();

        DataAdapterProvider.Input input = new DataAdapterProvider.Input();
        input.setParams(new Params());
        input.setAdapter(IndexedStoreImpl.class.getName());

        Map<String, Object> params = provider.provide(input);
        Object result = params.get("result");

        assertThat(result, is(notNullValue()));
        assertThat(result, instanceOf(Result.class));
        assertThat(((Result)result).getCount(), is(0L));
        assertThat(((Result)result).getItems().size(), is(0));
    }

    @Test
    public void singleTest() {
        store.reindex();

        ReportTestEntity entity = new ReportTestEntity();
        entity.setStringAttribute("test1");
        store.save(entity);
        flushCache();

        DataAdapterProvider.Input input = new DataAdapterProvider.Input();
        input.setParams(new Params());
        input.setAdapter(IndexedStoreImpl.class.getName());

        Map<String, Object> params = provider.provide(input);
        Object resultObject = params.get("result");

        assertThat(resultObject, is(notNullValue()));
        assertThat(resultObject, instanceOf(Result.class));

        Result<cz.inqool.uas.index.TestEntity> result = (Result) resultObject;
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems().size(), is(1));
        assertThat(result.getItems(), containsInAnyOrder(entity));
    }

    @Test
    public void multipleTest() {
        store.reindex();

        ReportTestEntity entity1 = new ReportTestEntity();
        entity1.setStringAttribute("test1");

        ReportTestEntity entity2 = new ReportTestEntity();
        entity2.setStringAttribute("test2");

        store.save(asSet(entity1, entity2));
        flushCache();

        DataAdapterProvider.Input input = new DataAdapterProvider.Input();
        input.setParams(new Params());
        input.getParams().setSort("stringAttribute");
        input.setAdapter(IndexedStoreImpl.class.getName());

        Map<String, Object> params = provider.provide(input);
        Object resultObject = params.get("result");

        assertThat(resultObject, is(notNullValue()));
        assertThat(resultObject, instanceOf(Result.class));

        Result<cz.inqool.uas.index.TestEntity> result = (Result) resultObject;
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems().size(), is(2));
        assertThat(result.getItems(), containsInAnyOrder(entity1, entity2));
    }

    @Test
    public void notIndexedStoreTest() {
        store.reindex();

        DataAdapterProvider.Input input = new DataAdapterProvider.Input();
        input.setParams(new Params());
        input.setAdapter(DomainStore.class.getName());

        assertThrown(() -> provider.provide(input))
                .isInstanceOf(GeneratingException.class);
    }

    @Test
    public void noStoreTest() {
        store.reindex();

        DataAdapterProvider.Input input = new DataAdapterProvider.Input();
        input.setParams(new Params());

        assertThrown(() -> provider.provide(input))
                .isInstanceOf(GeneratingException.class);
    }

    @Test
    public void storeNotFoundTest() {
        store.reindex();

        DataAdapterProvider.Input input = new DataAdapterProvider.Input();
        input.setParams(new Params());
        input.setAdapter(IndexedStoreImpl.class.getName()+"fake");

        assertThrown(() -> provider.provide(input))
                .isInstanceOf(GeneratingException.class);
    }

    @Test
    public void notParamsTest() {
        store.reindex();

        DataAdapterProvider.Input input = new DataAdapterProvider.Input();
        input.setAdapter(IndexedStoreImpl.class.getName());

        assertThrown(() -> provider.provide(input))
                .isInstanceOf(GeneratingException.class);
    }
}
