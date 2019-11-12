package cz.cas.lib.core.index;

import cz.cas.lib.core.index.solr.IndexedDatedObject;
import cz.cas.lib.core.index.solr.IndexedDomainObject;
import org.apache.solr.client.solrj.beans.Field;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

public class FieldAnnotationTest {
    private IndexedStoreImpl store;

    @Before
    public void setUp() {
        store = new IndexedStoreImpl();
    }

    @Test
    public void simpleTest() {
        Field field = store.getFieldAnnotation(IndexedDomainObject.class, "id");
        assertThat(field, is(notNullValue()));
    }

    @Test
    public void simpleNotExistTest() {
        Field field = store.getFieldAnnotation(IndexedDomainObject.class, "id2");
        assertThat(field, is(nullValue()));
    }

    @Test
    public void inheritedTest() {
        Field field = store.getFieldAnnotation(IndexedDatedObject.class, "id");
        assertThat(field, is(notNullValue()));
    }
}
