package cz.inqool.uas.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.inqool.uas.config.ObjectMapperProducer;
import cz.inqool.uas.report.exception.GeneratingException;
import cz.inqool.uas.report.provider.SqlProvider;
import cz.inqool.uas.report.sql.SqlStoreImpl;
import cz.inqool.uas.report.sql.SqlTestEntity;
import helper.DbTest;
import org.apache.tika.exception.TikaException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static cz.inqool.uas.util.Utils.asList;
import static cz.inqool.uas.util.Utils.asSet;
import static helper.ThrowableAssertion.assertThrown;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;

public class SqlProviderTest extends DbTest {
    private SqlProvider provider = new SqlProvider();

    private SqlStoreImpl store;

    @Before
    public void setUp() throws IOException, TikaException, SAXException {
        MockitoAnnotations.initMocks(this);

        ObjectMapperProducer producer = new ObjectMapperProducer();
        ObjectMapper mapper = producer.objectMapper(false, false);

        provider.setEm(getEm());
        provider.setObjectMapper(mapper);

        store = new SqlStoreImpl();
        store.setEntityManager(getEm());
        store.setQueryFactory(new JPAQueryFactory(getEm()));
    }

    @Test
    public void nosqlTest() {
        SqlProvider.Input input = new SqlProvider.Input();

        assertThrown(() -> provider.provide(input))
                .isInstanceOf(GeneratingException.class);
    }

    @Test
    public void simpleTest() {
        SqlProvider.Input input = new SqlProvider.Input();
        input.setSql("select 1");

        Map<String, Object> params = provider.provide(input);
        Object result = params.get("result");

        assertThat(result, is(notNullValue()));
        assertThat(result, instanceOf(List.class));
        assertThat(((List)result).size(), is(1));
        assertThat(((List)result).get(0), is(1));
    }

    @Test
    public void withoutParams() {
        SqlTestEntity entity = new SqlTestEntity();
        entity.setTest("test1");
        store.save(entity);
        flushCache();

        SqlProvider.Input input = new SqlProvider.Input();
        input.setSql("select id, test from test_sql");

        Map<String, Object> params = provider.provide(input);
        Object result = params.get("result");

        assertThat(result, is(notNullValue()));
        assertThat(result, instanceOf(List.class));

        List resultList = (List) result;
        assertThat(resultList.size(), is(1));

        Object object = resultList.get(0);
        assertThat(object, instanceOf(Object[].class));

        Object[] objectArray = (Object[]) object;

        assertThat(objectArray.length, is(2));
        assertThat(objectArray[0], is(entity.getId()));
        assertThat(objectArray[1], is(entity.getTest()));
    }

    @Test
    public void withoutMultipleParams() {
        SqlTestEntity entity1 = new SqlTestEntity();
        entity1.setTest("test1");

        SqlTestEntity entity2 = new SqlTestEntity();
        entity2.setTest("test2");

        store.save(asSet(entity1, entity2));
        flushCache();

        SqlProvider.Input input = new SqlProvider.Input();
        input.setSql("select id, test from test_sql order by test");

        Map<String, Object> params = provider.provide(input);
        Object result = params.get("result");

        assertThat(result, is(notNullValue()));
        assertThat(result, instanceOf(List.class));

        List resultList = (List) result;
        assertThat(resultList.size(), is(2));

        // entity 1
        Object object = resultList.get(0);
        assertThat(object, instanceOf(Object[].class));

        Object[] objectArray = (Object[]) object;

        assertThat(objectArray.length, is(2));
        assertThat(objectArray[0], is(entity1.getId()));
        assertThat(objectArray[1], is(entity1.getTest()));

        // entity 2
        object = resultList.get(1);
        assertThat(object, instanceOf(Object[].class));

        objectArray = (Object[]) object;

        assertThat(objectArray.length, is(2));
        assertThat(objectArray[0], is(entity2.getId()));
        assertThat(objectArray[1], is(entity2.getTest()));
    }

    @Test
    public void filtered() {
        SqlTestEntity entity1 = new SqlTestEntity();
        entity1.setTest("test1");

        SqlTestEntity entity2 = new SqlTestEntity();
        entity2.setTest("test2");

        store.save(asSet(entity1, entity2));
        flushCache();

        SqlProvider.Input input = new SqlProvider.Input();
        input.setSql("select id, test from test_sql where id = ?");
        input.setParams(asList(entity1.getId()));

        Map<String, Object> params = provider.provide(input);
        Object result = params.get("result");

        assertThat(result, is(notNullValue()));
        assertThat(result, instanceOf(List.class));

        List resultList = (List) result;
        assertThat(resultList.size(), is(1));

        Object object = resultList.get(0);
        assertThat(object, instanceOf(Object[].class));

        Object[] objectArray = (Object[]) object;

        assertThat(objectArray.length, is(2));
        assertThat(objectArray[0], is(entity1.getId()));
        assertThat(objectArray[1], is(entity1.getTest()));
    }

    @Test
    public void filteredAll() {
        SqlTestEntity entity = new SqlTestEntity();
        entity.setTest("test1");

        store.save(entity);
        flushCache();

        SqlProvider.Input input = new SqlProvider.Input();
        input.setSql("select id, test from test_sql where id = ?");
        input.setParams(asList("fake"));

        Map<String, Object> params = provider.provide(input);
        Object result = params.get("result");

        assertThat(result, is(notNullValue()));
        assertThat(result, instanceOf(List.class));

        List resultList = (List) result;
        assertThat(resultList.size(), is(0));
    }
}
