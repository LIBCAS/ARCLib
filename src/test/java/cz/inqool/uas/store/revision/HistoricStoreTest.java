package cz.inqool.uas.store.revision;

import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.inqool.uas.index.dto.Order;
import cz.inqool.uas.index.dto.Params;
import helper.DbTest;
import org.hamcrest.Matchers;
import org.hibernate.envers.RevisionType;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.List;

import static cz.inqool.uas.util.Utils.asSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * todo: tests for requests with Params
 */
public class HistoricStoreTest extends DbTest {

    private RevisionStoreImpl store;
    private RevisionHistoricStoreImpl historicStore;


    @Before
    public void setUp() {
        store = new RevisionStoreImpl();
        store.setEntityManager(getEm());
        store.setQueryFactory(new JPAQueryFactory(getEm()));

        historicStore = new RevisionHistoricStoreImpl();
        historicStore.setEntityManager(getEm());
    }

    @Test
    public void saveSingleTest() {
        VersionedEntity entity = new VersionedEntity();
        entity.setTest("test1");
        store.save(entity);

        flushCache();

        Collection<VersionedEntity> result = store.findAll();
        assertThat(result, containsInAnyOrder(entity));
    }

    @Test
    public void retrieveSingleRevisionTest() {
        VersionedEntity entity = new VersionedEntity();
        entity.setTest("test1");
        store.save(entity);

        flushCache();

        List<Revision> revisions = historicStore.getRevisions(entity.getId(), new Params());
        assertThat(revisions, hasSize(1));

        Revision revision = revisions.get(0);
        assertThat(revision.getId(), is(notNullValue()));
        assertThat(revision.getCreated(), is(notNullValue()));
        assertThat(revision.getItems(), hasSize(1));

        RevisionItem item = revision.getItems().iterator().next();
        assertThat(item.getEntityId(), is(entity.getId()));
        assertThat(item.getOperation(), is(RevisionType.ADD));

        VersionedEntity result = historicStore.findAtRevision(entity.getId(), revision.getId());
        assertThat(result.getId(), is(entity.getId()));
        assertThat(result.getTest(), is(entity.getTest()));
    }

    @Test
    public void retrieveMultipleRevisionTest() {
        VersionedEntity entity = new VersionedEntity();
        entity.setTest("test1");
        store.save(entity);

        flushCache();

        entity.setTest("test2");
        store.save(entity);

        flushCache();

        Params params =  new Params();
        params.setOrder(Order.ASC);

        List<Revision> revisions = historicStore.getRevisions(entity.getId(), params);
        assertThat(revisions, hasSize(2));

        Revision revision = revisions.get(0);
        assertThat(revision.getId(), is(notNullValue()));
        assertThat(revision.getCreated(), is(notNullValue()));

        RevisionItem item = revision.getItems().iterator().next();
        assertThat(item.getEntityId(), is(entity.getId()));
        assertThat(item.getOperation(), is(RevisionType.ADD));

        VersionedEntity result = historicStore.findAtRevision(entity.getId(), revision.getId());
        assertThat(result.getId(), is(entity.getId()));
        assertThat(result.getTest(), is("test1"));

        // second revision
        revision = revisions.get(1);
        assertThat(revision.getId(), is(notNullValue()));
        assertThat(revision.getCreated(), is(notNullValue()));

        item = revision.getItems().iterator().next();
        assertThat(item.getEntityId(), is(entity.getId()));
        assertThat(item.getOperation(), is(RevisionType.MOD));

        result = historicStore.findAtRevision(entity.getId(), revision.getId());
        assertThat(result.getId(), is(entity.getId()));
        assertThat(result.getTest(), is("test2"));
    }

    @Test
    public void deleteTest() {
        VersionedEntity entity = new VersionedEntity();
        entity.setTest("test1");
        store.save(entity);

        flushCache();

        store.delete(entity);

        flushCache();

        Params params =  new Params();
        params.setOrder(Order.ASC);

        List<Revision> revisions = historicStore.getRevisions(entity.getId(), params);
        assertThat(revisions, hasSize(2));

        Revision revision = revisions.get(0);
        assertThat(revision.getId(), is(notNullValue()));
        assertThat(revision.getCreated(), is(notNullValue()));

        RevisionItem item = revision.getItems().iterator().next();
        assertThat(item.getEntityId(), is(entity.getId()));
        assertThat(item.getOperation(), is(RevisionType.ADD));

        VersionedEntity result = historicStore.findAtRevision(entity.getId(), revision.getId());
        assertThat(result.getId(), is(entity.getId()));
        assertThat(result.getTest(), is("test1"));

        // second revision
        revision = revisions.get(1);
        assertThat(revision.getId(), is(notNullValue()));
        assertThat(revision.getCreated(), is(notNullValue()));

        item = revision.getItems().iterator().next();
        assertThat(item.getEntityId(), is(entity.getId()));
        assertThat(item.getOperation(), is(RevisionType.DEL));

        result = historicStore.findAtRevision(entity.getId(), revision.getId());
        assertThat(result, is(nullValue()));
    }

    @Test
    public void multipleInstancesTest() {
        VersionedEntity entity1 = new VersionedEntity();
        entity1.setTest("test1");

        VersionedEntity entity2 = new VersionedEntity();
        entity2.setTest("test2");
        store.save(asSet(entity1, entity2));

        flushCache();

        List<Revision> revisions = historicStore.getRevisions(entity1.getId(), new Params());
        assertThat(revisions, hasSize(1));

        Revision revision = revisions.get(0);
        assertThat(revision.getId(), is(notNullValue()));
        assertThat(revision.getCreated(), is(notNullValue()));
        assertThat(revision.getItems(), hasSize(2));

        assertThat(revision.getItems(), hasItem(allOf(
                hasProperty("entityId", is(entity1.getId())),
                hasProperty("operation", is(RevisionType.ADD))
        )));

        assertThat(revision.getItems(), hasItem(allOf(
                hasProperty("entityId", is(entity2.getId())),
                hasProperty("operation", is(RevisionType.ADD))
        )));

        VersionedEntity result = historicStore.findAtRevision(entity1.getId(), revision.getId());
        assertThat(result.getId(), is(entity1.getId()));
        assertThat(result.getTest(), is(entity1.getTest()));

        result = historicStore.findAtRevision(entity2.getId(), revision.getId());
        assertThat(result.getId(), is(entity2.getId()));
        assertThat(result.getTest(), is(entity2.getTest()));
    }

    @Test
    public void multipleInstancesSeparateTest() {
        VersionedEntity entity1 = new VersionedEntity();
        entity1.setTest("test1");
        store.save(entity1);
        flushCache();

        VersionedEntity entity2 = new VersionedEntity();
        entity2.setTest("test2");
        store.save(entity2);
        flushCache();

        List<Revision> revisions = historicStore.getRevisions(entity1.getId(), new Params());
        assertThat(revisions, hasSize(1));

        Revision revision = revisions.get(0);
        assertThat(revision.getId(), is(notNullValue()));
        assertThat(revision.getCreated(), is(notNullValue()));
        assertThat(revision.getItems(), hasSize(1));

        assertThat(revision.getItems(), hasItem(allOf(
                hasProperty("entityId", is(entity1.getId())),
                hasProperty("operation", is(RevisionType.ADD))
        )));

        VersionedEntity result = historicStore.findAtRevision(entity1.getId(), revision.getId());
        assertThat(result.getId(), is(entity1.getId()));
        assertThat(result.getTest(), is(entity1.getTest()));

        result = historicStore.findAtRevision(entity2.getId(), revision.getId());
        assertThat(result, is(nullValue()));


        revisions = historicStore.getRevisions(entity2.getId(), new Params());
        assertThat(revisions, hasSize(1));

        revision = revisions.get(0);
        assertThat(revision.getId(), is(notNullValue()));
        assertThat(revision.getCreated(), is(notNullValue()));
        assertThat(revision.getItems(), hasSize(1));

        assertThat(revision.getItems(), hasItem(allOf(
                hasProperty("entityId", is(entity2.getId())),
                hasProperty("operation", is(RevisionType.ADD))
        )));

        result = historicStore.findAtRevision(entity1.getId(), revision.getId());
        assertThat(result.getId(), is(entity1.getId()));
        assertThat(result.getTest(), is(entity1.getTest()));

        result = historicStore.findAtRevision(entity2.getId(), revision.getId());
        assertThat(result.getId(), is(entity2.getId()));
        assertThat(result.getTest(), is(entity2.getTest()));
    }

    @Test
    public void multipleInstancesSeparateDeleteTest() {
        VersionedEntity entity1 = new VersionedEntity();
        entity1.setTest("test1");
        store.save(entity1);
        flushCache();

        VersionedEntity entity2 = new VersionedEntity();
        entity2.setTest("test2");
        store.save(entity2);
        store.delete(entity1);
        flushCache();

        Params params =  new Params();
        params.setOrder(Order.ASC);

        // first object
        List<Revision> revisions = historicStore.getRevisions(entity1.getId(), params);
        assertThat(revisions, hasSize(2));

        // first revision ADD
        Revision revision = revisions.get(0);
        assertThat(revision.getId(), is(notNullValue()));
        assertThat(revision.getCreated(), is(notNullValue()));
        assertThat(revision.getItems(), hasSize(1));

        assertThat(revision.getItems(), hasItem(allOf(
                hasProperty("entityId", is(entity1.getId())),
                hasProperty("operation", is(RevisionType.ADD))
        )));

        VersionedEntity result = historicStore.findAtRevision(entity1.getId(), revision.getId());
        assertThat(result.getId(), is(entity1.getId()));
        assertThat(result.getTest(), is(entity1.getTest()));

        // second revision DEL
        revision = revisions.get(1);
        assertThat(revision.getId(), is(notNullValue()));
        assertThat(revision.getCreated(), is(notNullValue()));
        assertThat(revision.getItems(), hasSize(2));

        assertThat(revision.getItems(), hasItem(allOf(
                hasProperty("entityId", is(entity1.getId())),
                hasProperty("operation", is(RevisionType.DEL))
        )));

        result = historicStore.findAtRevision(entity1.getId(), revision.getId());
        assertThat(result, is(nullValue()));


        // second object
        revisions = historicStore.getRevisions(entity2.getId(), params);
        assertThat(revisions, hasSize(1));

        revision = revisions.get(0);
        assertThat(revision.getId(), is(notNullValue()));
        assertThat(revision.getCreated(), is(notNullValue()));
        assertThat(revision.getItems(), hasSize(2));

        assertThat(revision.getItems(), hasItem(allOf(
                hasProperty("entityId", is(entity1.getId())),
                hasProperty("operation", is(RevisionType.DEL))
        )));

        assertThat(revision.getItems(), hasItem(allOf(
                hasProperty("entityId", is(entity2.getId())),
                hasProperty("operation", is(RevisionType.ADD))
        )));

        result = historicStore.findAtRevision(entity1.getId(), revision.getId());
        assertThat(result, is(nullValue()));

        result = historicStore.findAtRevision(entity2.getId(), revision.getId());
        assertThat(result.getId(), is(entity2.getId()));
        assertThat(result.getTest(), is(entity2.getTest()));
    }


    @Test
    public void singleDepTest() {
        VersionedDepEntity depEntity = new VersionedDepEntity();
        depEntity.setValue("value1");

        VersionedEntity entity = new VersionedEntity();
        entity.setTest("test1");
        entity.setDeps(asSet(depEntity));

        store.save(entity);

        flushCache();

        List<Revision> revisions = historicStore.getRevisions(entity.getId(), new Params());
        assertThat(revisions, hasSize(1));

        Revision revision = revisions.get(0);
        assertThat(revision.getId(), is(notNullValue()));
        assertThat(revision.getCreated(), is(notNullValue()));
        assertThat(revision.getItems(), hasSize(3));

        assertThat(revision.getItems(), hasItem(allOf(
                hasProperty("entityId", is(entity.getId())),
                hasProperty("operation", is(RevisionType.ADD))
        )));

        assertThat(revision.getItems(), hasItem(allOf(
                hasProperty("entityId", is(depEntity.getId())),
                hasProperty("operation", is(RevisionType.ADD))
        )));

        // @JoinColumn creates own item for the parent entity
        assertThat(revision.getItems(), hasItem(allOf(
                hasProperty("entityId", is(entity.getId())),
                hasProperty("operation", is(RevisionType.MOD))
        )));


        VersionedEntity result = historicStore.findAtRevision(entity.getId(), revision.getId());
        assertThat(result.getId(), is(entity.getId()));
        assertThat(result.getTest(), is(entity.getTest()));
        assertThat(result.getDeps(), hasItem(allOf(
                hasProperty("id", is(depEntity.getId())),
                hasProperty("value", is(depEntity.getValue()))
        )));
    }

    @Test
    public void multipleDepTest() {
        // first save
        VersionedDepEntity depEntity1 = new VersionedDepEntity();
        depEntity1.setValue("value1");

        VersionedEntity entity = new VersionedEntity();
        entity.setTest("test1");
        entity.setDeps(asSet(depEntity1));

        store.save(entity);
        flushCache();

        // second save
        VersionedDepEntity depEntity2 = new VersionedDepEntity();
        depEntity2.setValue("value2");

        entity.setDeps(asSet(depEntity1, depEntity2));

        store.save(entity);
        flushCache();

        Params params =  new Params();
        params.setOrder(Order.ASC);

        List<Revision> revisions = historicStore.getRevisions(entity.getId(), params);
        assertThat(revisions, hasSize(2));

        // first revision
        Revision revision = revisions.get(0);
        assertThat(revision.getId(), is(notNullValue()));
        assertThat(revision.getCreated(), is(notNullValue()));
        assertThat(revision.getItems(), hasSize(3));

        assertThat(revision.getItems(), hasItem(allOf(
                hasProperty("entityId", is(entity.getId())),
                hasProperty("operation", is(RevisionType.ADD))
        )));

        assertThat(revision.getItems(), hasItem(allOf(
                hasProperty("entityId", is(depEntity1.getId())),
                hasProperty("operation", is(RevisionType.ADD))
        )));

        // @JoinColumn creates own item for the parent entity
        assertThat(revision.getItems(), hasItem(allOf(
                hasProperty("entityId", is(entity.getId())),
                hasProperty("operation", is(RevisionType.MOD))
        )));

        VersionedEntity result = historicStore.findAtRevision(entity.getId(), revision.getId());

        assertThat(result.getId(), is(entity.getId()));
        assertThat(result.getTest(), is(entity.getTest()));
        assertThat(result.getDeps(), hasSize(1));
        assertThat(result.getDeps(), hasItem(allOf(
                hasProperty("id", is(depEntity1.getId())),
                hasProperty("value", is(depEntity1.getValue()))
        )));

        // second revision
        revision = revisions.get(1);
        assertThat(revision.getId(), is(notNullValue()));
        assertThat(revision.getCreated(), is(notNullValue()));
        assertThat(revision.getItems(), hasSize(2));

        assertThat(revision.getItems(), hasItem(allOf(
                hasProperty("entityId", is(depEntity2.getId())),
                hasProperty("operation", is(RevisionType.ADD))
        )));

        // @JoinColumn creates own item for the parent entity
        assertThat(revision.getItems(), hasItem(allOf(
                hasProperty("entityId", is(entity.getId())),
                hasProperty("operation", is(RevisionType.MOD))
        )));

        result = historicStore.findAtRevision(entity.getId(), revision.getId());

        assertThat(result.getId(), is(entity.getId()));
        assertThat(result.getTest(), is(entity.getTest()));
        assertThat(result.getDeps(), hasSize(2));
        assertThat(result.getDeps(), hasItem(allOf(
                hasProperty("id", is(depEntity1.getId())),
                hasProperty("value", is(depEntity1.getValue()))
        )));
        assertThat(result.getDeps(), hasItem(allOf(
                hasProperty("id", is(depEntity2.getId())),
                hasProperty("value", is(depEntity2.getValue()))
        )));
    }

    @Test
    public void multipleDepDeleteTest() {
        // first save
        VersionedDepEntity depEntity1 = new VersionedDepEntity();
        depEntity1.setValue("value1");

        VersionedEntity entity = new VersionedEntity();
        entity.setTest("test1");
        entity.setDeps(asSet(depEntity1));

        store.save(entity);
        flushCache();

        // second save
        VersionedDepEntity depEntity2 = new VersionedDepEntity();
        depEntity2.setValue("value2");

        entity.setDeps(asSet(depEntity2));

        store.save(entity);
        flushCache();

        Params params =  new Params();
        params.setOrder(Order.ASC);

        List<Revision> revisions = historicStore.getRevisions(entity.getId(), params);
        assertThat(revisions, hasSize(2));

        // first revision
        Revision revision = revisions.get(0);
        assertThat(revision.getId(), is(notNullValue()));
        assertThat(revision.getCreated(), is(notNullValue()));
        assertThat(revision.getItems(), hasSize(3));

        assertThat(revision.getItems(), hasItem(allOf(
                hasProperty("entityId", is(entity.getId())),
                hasProperty("operation", is(RevisionType.ADD))
        )));

        assertThat(revision.getItems(), hasItem(allOf(
                hasProperty("entityId", is(depEntity1.getId())),
                hasProperty("operation", is(RevisionType.ADD))
        )));

        // @JoinColumn creates own item for the parent entity
        assertThat(revision.getItems(), hasItem(allOf(
                hasProperty("entityId", is(entity.getId())),
                hasProperty("operation", is(RevisionType.MOD))
        )));

        VersionedEntity result = historicStore.findAtRevision(entity.getId(), revision.getId());

        assertThat(result.getId(), is(entity.getId()));
        assertThat(result.getTest(), is(entity.getTest()));
        assertThat(result.getDeps(), hasSize(1));
        assertThat(result.getDeps(), hasItem(allOf(
                hasProperty("id", is(depEntity1.getId())),
                hasProperty("value", is(depEntity1.getValue()))
        )));

        // second revision
        revision = revisions.get(1);
        assertThat(revision.getId(), is(notNullValue()));
        assertThat(revision.getCreated(), is(notNullValue()));
        assertThat(revision.getItems(), hasSize(3));

        assertThat(revision.getItems(), hasItem(allOf(
                hasProperty("entityId", is(depEntity1.getId())),
                hasProperty("operation", is(RevisionType.DEL))
        )));

        assertThat(revision.getItems(), hasItem(allOf(
                hasProperty("entityId", is(depEntity2.getId())),
                hasProperty("operation", is(RevisionType.ADD))
        )));

        // @JoinColumn creates own item for the parent entity
        assertThat(revision.getItems(), hasItem(allOf(
                hasProperty("entityId", is(entity.getId())),
                hasProperty("operation", is(RevisionType.MOD))
        )));

        result = historicStore.findAtRevision(entity.getId(), revision.getId());

        assertThat(result.getId(), is(entity.getId()));
        assertThat(result.getTest(), is(entity.getTest()));
        assertThat(result.getDeps(), hasSize(1));
        assertThat(result.getDeps(), hasItem(allOf(
                hasProperty("id", is(depEntity2.getId())),
                hasProperty("value", is(depEntity2.getValue()))
        )));
    }

    @Test
    public void getTypeTest() {
        assertThat(historicStore.getType(), Matchers.<Class<VersionedEntity>>is(VersionedEntity.class));
    }
}
