package cz.inqool.uas.detach.test;


import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.inqool.uas.detach.objects.Object1;
import cz.inqool.uas.detach.objects.Object2;
import cz.inqool.uas.detach.store.Object1Store;
import cz.inqool.uas.detach.store.Object2Store;
import helper.DbTest;
import org.apache.tika.exception.TikaException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.xml.sax.SAXException;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class DetachedTest extends DbTest {

    private Object1Store object1Store;

    private Object2Store object2Store;

    @Before
    public void setUp() throws IOException, TikaException, SAXException {
        MockitoAnnotations.initMocks(this);

        object1Store = new Object1Store();
        object1Store.setEntityManager(getEm());
        object1Store.setQueryFactory(new JPAQueryFactory(getEm()));

        object2Store = new Object2Store();
        object2Store.setEntityManager(getEm());
        object2Store.setQueryFactory(new JPAQueryFactory(getEm()));
    }

    @Test
    public void detach() {
        Object1 object1 = new Object1();
        object1.setId("id1");
        object1 = object1Store.save(object1);
        flushCache();

        Object2 object2 = new Object2();
        object2.setId("id2");
        object2.setObject1(object1);
        object2 = object2Store.save(object2);
        flushCache();

        object1 = object1Store.find(object1.getId());
        assertThat(object1.getObject2Set(), hasSize(1));

    }
}
