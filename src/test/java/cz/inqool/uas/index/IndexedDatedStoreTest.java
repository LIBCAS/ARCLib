package cz.inqool.uas.index;

import com.querydsl.jpa.impl.JPAQueryFactory;
import helper.DbTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class IndexedDatedStoreTest extends DbTest {
    protected IndexedDatedStore indexedDatedStore;

    @Mock
    protected ElasticsearchTemplate template;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        indexedDatedStore = new IndexedStoreImpl();
        indexedDatedStore.setTemplate(template);
        indexedDatedStore.setEntityManager(getEm());
        indexedDatedStore.setQueryFactory(new JPAQueryFactory(getEm()));
    }

    @Test
    public void toIndexObjectTest() {
        TestEntity a = new TestEntity();

        IndexedDatedObject indexedDatedObject = indexedDatedStore.toIndexObject(a);
        assertThat(indexedDatedObject.getCreated(), is(a.getCreated()));
        assertThat(indexedDatedObject.getUpdated(), is(a.getUpdated()));
        assertThat(indexedDatedObject.getId(), is(a.getId()));
    }
}
