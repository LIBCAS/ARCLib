package cz.inqool.uas.store;

import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.inqool.uas.store.general.DomainStoreImpl;
import cz.inqool.uas.store.general.GeneralTestEntity;
import helper.DbTest;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static cz.inqool.uas.util.Utils.asSet;
import static helper.ThrowableAssertion.assertThrown;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;

public class DomainStoreTest extends DbTest {

    private DomainStoreImpl store;

    private List<GeneralTestEntity> entities;

    @Before
    public void setUp() {
        store = new DomainStoreImpl();
        store.setEntityManager(getEm());
        store.setQueryFactory(new JPAQueryFactory(getEm()));
        entities = asList(new GeneralTestEntity(), new GeneralTestEntity(), new GeneralTestEntity());
    }

    @Test
    public void saveSingleTest() {
        GeneralTestEntity entity = new GeneralTestEntity();
        store.save(entity);

        flushCache();

        Collection<GeneralTestEntity> result = store.findAll();
        assertThat(result, containsInAnyOrder(entity));
    }

    @Test
    public void saveNullTest() {
        assertThrown(() -> store.save((GeneralTestEntity) null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThrown(() -> store.save((Collection<? extends GeneralTestEntity>) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void findAllEmptyTest() {
        Collection<GeneralTestEntity> all = store.findAll();
        assertThat(all, is(empty()));
    }

    @Test
    public void findAllTest() {
        store.save(entities);
        flushCache();

        Collection<GeneralTestEntity> all = store.findAll();
        assertThat(all, hasSize(3));
        assertThat(all, containsInAnyOrder(entities.toArray()));
    }

    @Test
    public void findAnyEmptyTest() {
        GeneralTestEntity any = store.findAny();
        assertThat(any, is(nullValue()));
    }


    @Test
    public void findAnyTest() {
        store.save(entities);
        flushCache();

        GeneralTestEntity any = store.findAny();
        assertThat(any, is(notNullValue()));
        assertThat(any, isIn(entities));
    }

    @Test
    public void findAllInListEmptyTest() {
        List<GeneralTestEntity> list = store.findAllInList(Collections.emptyList());
        assertThat(list, is(empty()));

        list = store.findAllInList(asList(entities.get(0).getId(), entities.get(1).getId()));
        assertThat(list, is(empty()));

        list = store.findAllInList(asList(entities.get(0).getId(), "fake"));
        assertThat(list, is(empty()));

        list = store.findAllInList(asList("fake1", "fake2"));
        assertThat(list, is(empty()));
    }

    @Test
    public void findAllInListTest() {
        store.save(entities);
        flushCache();

        List<GeneralTestEntity> list = store.findAllInList(Collections.emptyList());
        assertThat(list, is(empty()));

        list = store.findAllInList(asList(entities.get(0).getId(), entities.get(1).getId()));
        assertThat(list, hasSize(2));
        assertThat(list, containsInAnyOrder(entities.get(0), entities.get(1)));

        list = store.findAllInList(asList(entities.get(0).getId(), "fake"));
        assertThat(list, hasSize(1));
        assertThat(list, containsInAnyOrder(entities.get(0)));

        list = store.findAllInList(asList("fake1", "fake2"));
        assertThat(list, is(empty()));
    }

    @Test
    public void findEmptyTest() {
        GeneralTestEntity e = store.find(entities.get(0).getId());
        assertThat(e, is(nullValue()));

        e = store.find("");
        assertThat(e, is(nullValue()));
    }

    @Test
    public void findTest() {
        store.save(entities);
        flushCache();

        GeneralTestEntity e = store.find(entities.get(0).getId());
        assertThat(e, is(entities.get(0)));

        e = store.find("");
        assertThat(e, is(nullValue()));
    }

    @Test
    public void createTest() {
        GeneralTestEntity e = new GeneralTestEntity();
        e.setTest("test");
        store.save(e);
        flushCache();

        Collection<GeneralTestEntity> all = store.findAll();
        assertThat(all, hasSize(1));
        assertThat(all, contains(e));
        assertThat(all.iterator().next().getTest(), is("test"));
    }

    @Test
    public void updateTest() {
        GeneralTestEntity e = new GeneralTestEntity();
        e.setTest("test");
        store.save(e);
        flushCache();

        e.setTest("new test");
        store.save(e); //update
        flushCache();

        Collection<GeneralTestEntity> all = store.findAll();
        assertThat(all, hasSize(1));
        assertThat(all, contains(e));
        assertThat(all.iterator().next().getTest(), is("new test"));
    }

    @Test
    public void createMultipleTest() {
        GeneralTestEntity e1 = new GeneralTestEntity();
        e1.setTest("test1");
        GeneralTestEntity e2 = new GeneralTestEntity();
        e2.setTest("test2");

        store.save(asSet(e1, e2)); //create
        flushCache();

        Collection<GeneralTestEntity> all = store.findAll();
        assertThat(all, hasSize(2));
        assertThat(all, hasItem(e1));
        assertThat(all, hasItem(e2));
        assertThat(all, hasItem(hasProperty("test", equalTo("test1"))));
        assertThat(all, hasItem(hasProperty("test", equalTo("test2"))));
    }

    @Test
    public void updateMultipleTest() {
        GeneralTestEntity e1 = new GeneralTestEntity();
        e1.setTest("test1");
        GeneralTestEntity e2 = new GeneralTestEntity();
        e2.setTest("test2");

        store.save(asSet(e1, e2)); //create
        flushCache();

        e1.setTest("test3");
        e2.setTest("test4");

        store.save(asSet(e1, e2)); //create
        flushCache();

        Collection<GeneralTestEntity> all = store.findAll();
        assertThat(all, hasSize(2));
        assertThat(all, hasItem(e1));
        assertThat(all, hasItem(e2));
        assertThat(all, hasItem(hasProperty("test", is("test3"))));
        assertThat(all, hasItem(hasProperty("test", is("test4"))));
    }

    @Test
    public void createUpdateTest() {
        GeneralTestEntity e1 = new GeneralTestEntity();
        e1.setTest("test1");
        GeneralTestEntity e2 = new GeneralTestEntity();
        e2.setTest("test2");

        store.save(e2); //create
        flushCache();

        e2.setTest("test4");

        store.save(asSet(e1, e2)); //create and update
        flushCache();

        Collection<GeneralTestEntity> all = store.findAll();
        assertThat(all, hasSize(2));
        assertThat(all, hasItem(e1));
        assertThat(all, hasItem(e2));
        assertThat(all, hasItem(hasProperty("test", is("test1"))));
        assertThat(all, hasItem(hasProperty("test", is("test4"))));
    }

    @Test
    public void deleteEmptyTest() {
        GeneralTestEntity notInDb = new GeneralTestEntity();

        store.delete(null);
        store.delete(notInDb);
        store.delete(entities.get(0));

        Collection<GeneralTestEntity> all = store.findAll();
        assertThat(all, is(empty()));
    }


    @Test
    public void deleteTest() {
        GeneralTestEntity notInDb = new GeneralTestEntity();

        store.save(entities);
        flushCache();

        store.delete(null);
        flushCache();
        Collection<GeneralTestEntity> all = store.findAll();
        assertThat(all, hasSize(3));

        store.delete(notInDb);
        flushCache();
        all = store.findAll();
        assertThat(all, hasSize(3));

        store.delete(entities.get(0));
        flushCache();
        all = store.findAll();
        assertThat(all, hasSize(2));
        assertThat(all, containsInAnyOrder(entities.get(1), entities.get(2)));
    }

    @Test
    public void getTypeTest() {
        assertThat(store.getType(), Matchers.<Class<GeneralTestEntity>>is(GeneralTestEntity.class));
    }
}
