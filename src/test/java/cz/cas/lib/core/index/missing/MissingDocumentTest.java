package cz.cas.lib.core.index.missing;

import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.cas.lib.core.exception.GeneralException;
import cz.cas.lib.core.index.TestEntity;
import cz.cas.lib.core.index.dto.Params;
import helper.SrDbTest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static helper.ThrowableAssertion.assertThrown;

@Ignore
public class MissingDocumentTest extends SrDbTest {
    private MissingStoreImpl store;

    @Before
    public void setUp() throws Exception {
        super.testSetUp();

        store = new MissingStoreImpl();
        store.setEntityManager(getEm());
        store.setQueryFactory(new JPAQueryFactory(getEm()));
        store.setTemplate(getTemplate());
    }


    @Test
    public void reindexTest() {
        assertThrown(() -> store.reindex())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void saveSingleTest() {
        assertThrown(() -> store.save(new TestEntity()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void findTest() {
        assertThrown(() -> store.findAll(new Params()))
                .isInstanceOf(GeneralException.class);
    }
}
