package cz.cas.lib.core.index.noconstructor;

import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.cas.lib.core.index.TestEntity;
import helper.SrDbTest;
import org.junit.Before;
import org.junit.Test;

import static helper.ThrowableAssertion.assertThrown;

public class NoEmptyConstructorTest extends SrDbTest {
    private TestStoreImpl store;

    @Before
    public void setUp() throws Exception {
        super.testSetUp();

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
