package cz.cas.lib.core.index;

import cz.cas.lib.core.index.solr.SolrDatedObject;
import cz.cas.lib.core.index.solr.SolrDictionaryObject;
import cz.cas.lib.core.index.solr.SolrDomainObject;
import org.apache.solr.client.solrj.beans.Field;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

public class FieldAnnotationTest {
    private SolrStoreImpl store;

    @Before
    public void setUp() {
        store = new SolrStoreImpl();
    }

    @Test
    public void simpleTest() {
        Field field = store.getFieldAnnotation(SolrDomainObject.class, "id");
        assertThat(field, is(notNullValue()));
    }

    @Test
    public void simpleNotExistTest() {
        Field field = store.getFieldAnnotation(SolrDomainObject.class, "id2");
        assertThat(field, is(nullValue()));
    }

    @Test
    public void inheritedTest() {
        Field field = store.getFieldAnnotation(SolrDatedObject.class, "id");
        assertThat(field, is(notNullValue()));
    }

    @Test
    public void doubleInheritedTest() {
        Field field = store.getFieldAnnotation(SolrDictionaryObject.class, "id");
        assertThat(field, is(notNullValue()));
    }

    @Test
    public void simpleGenericTest() {
        Field field = store.getFieldAnnotation(SolrTestEntity.class, "dependent.id");
        assertThat(field, is(notNullValue()));
    }

    @Test
    @Ignore
    public void collectionGenericTest() {
        Field field = store.getFieldAnnotation(SolrTestEntity.class, "dependents.id");
        assertThat(field, is(notNullValue()));
    }
}
