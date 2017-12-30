package cz.inqool.uas.index;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.elasticsearch.annotations.Field;

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

    @Test
    public void doubleInheritedTest() {
        Field field = store.getFieldAnnotation(IndexedDictionaryObject.class, "id");
        assertThat(field, is(notNullValue()));
    }

    @Test
    public void simpleGenericTest() {
        Field field = store.getFieldAnnotation(IndexedTestEntity.class, "dependent.id");
        assertThat(field, is(notNullValue()));
    }

    @Test
    public void collectionGenericTest() {
        Field field = store.getFieldAnnotation(IndexedTestEntity.class, "dependents.id");
        assertThat(field, is(notNullValue()));
    }
}
