package cz.inqool.uas.index.missing;

import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.inqool.uas.exception.GeneralException;
import cz.inqool.uas.index.TestEntity;
import cz.inqool.uas.index.dto.Params;
import helper.EsDbTest;
import org.junit.Before;
import org.junit.Test;

import static helper.ThrowableAssertion.assertThrown;

public class MissingDocumentTest extends EsDbTest {
    private MissingStoreImpl store;

    @Before
    public void setUp() throws Exception {
        super.setUp();

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
