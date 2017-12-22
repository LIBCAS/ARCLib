package cz.inqool.uas.error;

import com.querydsl.jpa.impl.JPAQueryFactory;
import helper.DbTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ErrorStoreIndexingTest extends DbTest {

    protected ErrorStore store;

    @Mock
    protected ElasticsearchTemplate elasticsearchTemplate;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        store =  new ErrorStore();
        store.setEntityManager(getEm());
        store.setTemplate(elasticsearchTemplate);
        store.setQueryFactory(new JPAQueryFactory(getEm()));
    }

    @Test
    public void toIndexObjectTest() {
        Error error = new Error();
        error.setClientSide(true);
        error.setUserId("123");
        error.setIp("192.0.0.0");
        error.setUrl("test url");

        IndexedError indexedError = store.toIndexObject(error);

        assertThat(indexedError.getCreated(), is(error.getCreated()));
        assertThat(indexedError.getUpdated(), is(error.getUpdated()));
        assertThat(indexedError.getClientSide(), is(error.getClientSide()));
        assertThat(indexedError.getUserId(), is(error.getUserId()));
        assertThat(indexedError.getIp(), is(error.getIp()));
        assertThat(indexedError.getUrl(), is(error.getUrl()));
    }
}
