package cz.inqool.uas.index.noconstructor;

import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.inqool.uas.index.TestEntity;
import helper.EsDbTest;
import org.junit.Before;
import org.junit.Test;

import static helper.ThrowableAssertion.assertThrown;

public class NoEmptyConstructorTest extends EsDbTest {
    private TestStoreImpl store;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        store = new TestStoreImpl();
        store.setEntityManager(getEm());
        store.setQueryFactory(new JPAQueryFactory(getEm()));
        store.setTemplate(getTemplate());
    }

    @Test
    public void saveSingleTest() {
        assertThrown(() -> store.save(new TestEntity()))
                .isInstanceOf(InstantiationException.class);
    }
}
