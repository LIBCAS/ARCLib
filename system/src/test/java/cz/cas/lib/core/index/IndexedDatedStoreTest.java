package cz.cas.lib.core.index;

import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.cas.lib.core.index.solr.IndexedDatedObject;
import cz.cas.lib.core.index.solr.IndexedDatedStore;
import helper.DbTest;
import org.apache.solr.client.solrj.SolrClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class IndexedDatedStoreTest extends DbTest {
    protected IndexedDatedStore indexedDatedStore;

    @Mock
    protected SolrClient solrClient;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        indexedDatedStore = new IndexedStoreImpl();
        indexedDatedStore.setSolrClient(solrClient);
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
