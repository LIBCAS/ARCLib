package cz.cas.lib.core.store;

import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.cas.lib.core.domain.DomainObject;
import cz.cas.lib.core.store.dated.DatedChildTestEntity;
import cz.cas.lib.core.store.dated.DatedStoreDomainImpl;
import cz.cas.lib.core.store.dated.DatedStoreImpl;
import cz.cas.lib.core.store.dated.DatedTestEntity;
import helper.DbTest;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static cz.cas.lib.core.util.Utils.*;
import static helper.TestUtils.closeTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class DatedStoreTest extends DbTest {

    private DatedStoreImpl store;

    private DatedStoreDomainImpl storeGeneral;

    private List<DatedTestEntity> entities;

    private List<DatedTestEntity> entitiesAll;

    private DatedTestEntity deleted;

    @Before
    public void setUp() {
        store = new DatedStoreImpl();
        store.setEntityManager(getEm());
        store.setQueryFactory(new JPAQueryFactory(getEm()));

        storeGeneral = new DatedStoreDomainImpl();
        storeGeneral.setEntityManager(getEm());
        storeGeneral.setQueryFactory(new JPAQueryFactory(getEm()));

        deleted = new DatedTestEntity();
        deleted.setDeleted(Instant.now());

        entities = asList(new DatedTestEntity(), new DatedTestEntity(), new DatedTestEntity());
        entitiesAll = asList(entities, deleted);
    }

    @Test
    public void findAllEmptyTest() {
        store.save(deleted);
        flushCache();

        Collection<DatedTestEntity> all = store.findAll();
        assertThat(all, is(empty()));
    }

    @Test
    public void findAllTest() {
        store.save(entitiesAll);
        flushCache();

        Collection<DatedTestEntity> all = store.findAll();
        assertThat(all, hasSize(3));
        assertThat(all, containsInAnyOrder(entities.toArray()));
    }

    @Test
    public void findAnyEmptyTest() {
        store.save(deleted);
        flushCache();

        DatedTestEntity any = store.findAny();
        assertThat(any, is(nullValue()));
    }


    @Test
    public void findAllInListEmptyTest() {
        store.save(deleted);
        flushCache();

        List<DatedTestEntity> list = store.findAllInList(asList(deleted.getId()));
        assertThat(list, is(empty()));
    }

    @Test
    public void findAllInListTest() {
        store.save(entitiesAll);
        flushCache();

        List<DatedTestEntity> list = store.findAllInList(Collections.emptyList());
        assertThat(list, is(empty()));

        list = store.findAllInList(asList(entities.get(0).getId(), deleted.getId()));
        assertThat(list, hasSize(1));
        assertThat(list, hasItem(entities.get(0)));
    }

    @Test
    public void findTest() {
        store.save(entitiesAll);
        flushCache();

        DatedTestEntity e = store.find(deleted.getId());
        assertThat(e, is(nullValue()));
    }

    @Test
    public void createTest() {
        DatedTestEntity e = new DatedTestEntity();
        e.setName("name");
        e.setOrder(2L);
        e.setActive(true);

        store.save(e);
        flushCache();

        DatedTestEntity entity = store.find(e.getId());
        assertThat(entity, is(notNullValue()));
        assertThat(entity.getCreated(), is(notNullValue()));
        assertThat(entity.getUpdated(), is(notNullValue()));
        assertThat(entity.getDeleted(), is(nullValue()));

        assertThat(entity.getName(), is("name"));
        assertThat(entity.getOrder(), is(2L));
        assertThat(entity.getActive(), is(true));
    }

    @Test
    public void updateTest() throws InterruptedException {
        DatedTestEntity e = new DatedTestEntity();
        e = store.save(e);
        flushCache();

        Thread.sleep(100);

        e.setName("changed name");
        e = store.save(e); //update
        flushCache();

        DatedTestEntity entity = store.find(e.getId());
        assertThat(entity, is(notNullValue()));
        assertThat(entity.getCreated(), is(notNullValue()));
        assertThat(entity.getUpdated(), is(notNullValue()));
        assertThat(entity.getCreated(), is(not(entity.getUpdated())));
    }

    @Test
    public void updateWithoutChangeTest() throws InterruptedException {
        DatedTestEntity e = new DatedTestEntity();
        e = store.save(e);
        flushCache();

        Thread.sleep(100);

        e = store.save(e); //update
        flushCache();

        DatedTestEntity entity = store.find(e.getId());
        assertThat(entity, is(notNullValue()));
        assertThat(entity.getCreated(), is(notNullValue()));
        assertThat(entity.getUpdated(), is(notNullValue()));
        assertThat(entity.getCreated(), is(closeTo(entity.getUpdated(), 50)));
    }

    @Test
    public void createMultipleTest() {
        DatedTestEntity e1 = new DatedTestEntity();
        DatedTestEntity e2 = new DatedTestEntity();

        store.save(asSet(e1, e2)); //create
        flushCache();

        Collection<DatedTestEntity> all = store.findAll();
        assertThat(all, hasSize(2));
        assertThat(all, hasItem(e1));
        assertThat(all, hasItem(e2));
        assertThat(all, hasItem(allOf(
                hasProperty("id", is(e1.getId())),
                hasProperty("created", is(notNullValue())),
                hasProperty("updated", is(notNullValue())),
                hasProperty("deleted", is(nullValue()))
        )));
        assertThat(all, hasItem(allOf(
                hasProperty("id", is(e2.getId())),
                hasProperty("created", is(notNullValue())),
                hasProperty("updated", is(notNullValue())),
                hasProperty("deleted", is(nullValue()))
        )));
    }

    @Test
    public void updateMultipleTest() throws InterruptedException {
        DatedTestEntity e1 = new DatedTestEntity();
        DatedTestEntity e2 = new DatedTestEntity();

        Collection<? extends DatedTestEntity> result = store.save(asSet(e1, e2));//create
        e1 = get(result, DomainObject::getId, e1.getId());
        e2 = get(result, DomainObject::getId, e2.getId());

        flushCache();

        e1.setName("changed name");
        e2.setName("changed name2");
        Thread.sleep(100);
        result = store.save(asSet(e1, e2));//update
        e1 = get(result, DomainObject::getId, e1.getId());
        e2 = get(result, DomainObject::getId, e2.getId());

        flushCache();

        Collection<DatedTestEntity> all = store.findAll();
        assertThat(all, hasSize(2));
        assertThat(all, hasItem(e1));
        assertThat(all, hasItem(e2));

        all.forEach(entity -> {
            assertThat(entity, is(notNullValue()));
            assertThat(entity.getCreated(), is(notNullValue()));
            assertThat(entity.getUpdated(), is(notNullValue()));
            assertThat(entity.getCreated(), is(not(entity.getUpdated())));
        });
    }

    @Test
    public void createUpdateTest() throws InterruptedException {
        DatedTestEntity e1 = new DatedTestEntity();
        DatedTestEntity e2 = new DatedTestEntity();

        String e1Id = e1.getId();

        e2 = store.save(e2); //create
        flushCache();

        Thread.sleep(200);

        e2.setName("changed name");
        Thread.sleep(10);
        Collection<? extends DatedTestEntity> result = store.save(asSet(e1, e2));//create and update
        e1 = get(result, DomainObject::getId, e1.getId());
        e2 = get(result, DomainObject::getId, e2.getId());

        flushCache();

        Collection<DatedTestEntity> all = store.findAll();
        assertThat(all, hasSize(2));
        assertThat(all, hasItem(e1));
        assertThat(all, hasItem(e2));

        all.forEach(entity -> {
            assertThat(entity, is(notNullValue()));
            assertThat(entity.getCreated(), is(notNullValue()));
            assertThat(entity.getUpdated(), is(notNullValue()));

            if (Objects.equals(entity.getId(), e1Id)) {
                assertThat(entity.getCreated(), is(closeTo(entity.getUpdated(), 50)));
            } else {
                assertThat(entity.getCreated(), is(not(entity.getUpdated())));
            }
        });
    }

    @Test
    public void createUpdateWithChildTest() throws InterruptedException {
        DatedChildTestEntity child1 = new DatedChildTestEntity();
        DatedChildTestEntity child2 = new DatedChildTestEntity();

        DatedTestEntity e1 = new DatedTestEntity();
        DatedTestEntity e2 = new DatedTestEntity();

        String e1Id = e1.getId();

        e1.setChild(child1);
        e2.setChild(child2);

        e2 = store.save(e2); //create
        flushCache();

        Thread.sleep(100);

        e2.setName("changed name");
        e2.getChild().setName("changed child name");
        Collection<? extends DatedTestEntity> result = store.save(asSet(e1, e2));//create and update
        e1 = get(result, DomainObject::getId, e1.getId());
        e2 = get(result, DomainObject::getId, e2.getId());
        flushCache();

        Collection<DatedTestEntity> all = store.findAll();
        assertThat(all, hasSize(2));
        assertThat(all, hasItem(e1));
        assertThat(all, hasItem(e2));

        all.forEach(entity -> {
            DatedChildTestEntity child = entity.getChild();

            assertThat(child, is(notNullValue()));
            assertThat(child.getCreated(), is(notNullValue()));
            assertThat(child.getUpdated(), is(notNullValue()));

            if (Objects.equals(entity.getId(), e1Id)) {
                assertThat(child.getCreated(), is(closeTo(child.getUpdated(), 50)));
                assertThat(entity.getCreated(), is(closeTo(entity.getUpdated(), 50)));
            } else {
                assertThat(child.getCreated(), is(not(child.getUpdated())));
                assertThat(entity.getCreated(), is(not(entity.getUpdated())));
            }
        });
    }

    @Test
    public void updateWithChildWithoutChange() throws InterruptedException {
        DatedTestEntity e = new DatedTestEntity();
        e.setChild(new DatedChildTestEntity());

        e = store.save(e); //create
        flushCache();

        Thread.sleep(100);

        e = store.save(e); //update
        flushCache();

        DatedTestEntity entity = store.find(e.getId());
        DatedChildTestEntity child = entity.getChild();

        assertThat(child, is(notNullValue()));
        assertThat(child.getCreated(), is(notNullValue()));
        assertThat(child.getUpdated(), is(notNullValue()));
        assertThat(child.getCreated(), is(closeTo(child.getUpdated(), 50)));

        assertThat(entity.getCreated(), is(closeTo(entity.getUpdated(), 50)));
    }

    @Test
    public void updateWithChildChangeParent() throws InterruptedException {
        DatedTestEntity e = new DatedTestEntity();
        e.setChild(new DatedChildTestEntity());

        e = store.save(e); //create
        flushCache();

        Thread.sleep(100);

        e.setName("changed name");
        e = store.save(e); //update
        flushCache();

        DatedTestEntity entity = store.find(e.getId());
        DatedChildTestEntity child = entity.getChild();

        assertThat(child, is(notNullValue()));
        assertThat(child.getCreated(), is(notNullValue()));
        assertThat(child.getUpdated(), is(notNullValue()));
        assertThat(child.getCreated(), is(closeTo(child.getUpdated(), 50)));

        assertThat(entity.getCreated(), is(not(entity.getUpdated())));
    }

    @Test
    public void updateWithChildChangeChild() throws InterruptedException {
        DatedTestEntity e = new DatedTestEntity();
        e.setChild(new DatedChildTestEntity());

        e = store.save(e); //create
        flushCache();

        Thread.sleep(100);

        e.getChild().setName("changed child name");
        e = store.save(e); //update
        flushCache();

        DatedTestEntity entity = store.find(e.getId());
        DatedChildTestEntity child = entity.getChild();

        assertThat(child, is(notNullValue()));
        assertThat(child.getCreated(), is(notNullValue()));
        assertThat(child.getUpdated(), is(notNullValue()));
        assertThat(child.getCreated(), is(not(child.getUpdated())));

        assertThat(entity.getCreated(), is(closeTo(entity.getUpdated(), 50)));
    }

    @Test
    public void deleteTest() {
        DatedTestEntity e = new DatedTestEntity();

        store.save(e);
        flushCache();

        store.delete(e);

        Collection<DatedTestEntity> all = store.findAll();
        assertThat(all, is(empty()));

        all = storeGeneral.findAll();
        assertThat(all, hasSize(1));
        assertThat(all, hasItem(e));

        assertThat(all, hasItem(allOf(
                hasProperty("id", is(e.getId())),
                hasProperty("created", is(notNullValue())),
                hasProperty("updated", is(notNullValue())),
                hasProperty("deleted", is(notNullValue()))
        )));
    }
}
