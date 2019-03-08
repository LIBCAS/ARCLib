package cz.cas.lib.core.index;

import cz.cas.lib.arclib.domain.Batch;
import cz.cas.lib.arclib.index.solr.SolrQueryUtils;
import cz.cas.lib.arclib.index.solr.arclibxml.SolrArclibXmlDocument;
import cz.cas.lib.arclib.store.BatchStore;
import cz.cas.lib.core.exception.BadArgument;
import cz.cas.lib.core.index.dto.*;
import helper.SrDbTest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleFilterQuery;
import org.springframework.data.solr.core.query.SimpleQuery;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static cz.cas.lib.core.util.Utils.*;
import static helper.ThrowableAssertion.assertThrown;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Requires <i>test</i> and <i>solrref</i> cores to be created and <i>folding</i> field type to be present in the <i>test</i> core.
 * Folding type can look like this:
 * <p>
 * {@code
 * <fieldType name="folding" class="solr.TextField" positionIncrementGap="100">
 * <analyzer>
 * <tokenizer class="solr.KeywordTokenizerFactory"/>
 * <filter class="solr.LowerCaseFilterFactory"/>
 * <filter class="solr.ASCIIFoldingFilterFactory" preserveOriginal="true"/>
 * </analyzer>
 * </fieldType>
 * }
 * </p>
 */
public class SolrStoreTest extends SrDbTest {
    private SolrStoreImpl store = new SolrStoreImpl();
    private BatchStore batchStore = new BatchStore();

    private TestEntity[] entities;

    @Before
    public void setUpSolrStoreTest() throws Exception {
        initializeStores(batchStore, store);
        entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity()};
    }

    @Test
    public void saveSingleTest() {
        store.save(entities[0]);


        Result<TestEntity> result = store.findAll(new Params());
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0]));
    }

    @Test
    public void saveMultipleTest() {
        store.save(asList(entities));


        Result<TestEntity> result = store.findAll(new Params());
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), containsInAnyOrder(entities));
    }

    @Test
    public void saveEmptyTest() {
        store.save(emptyList());


        Result<TestEntity> result = store.findAll(new Params());
        assertThat(result.getCount(), is(0L));
        assertThat(result.getItems(), is(empty()));
    }

    @Test
    public void deleteTest() {
        store.save(asList(entities));
        store.delete(entities[0]);


        Result<TestEntity> result = store.findAll(new Params());
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[1], entities[2]));
    }

    @Test
    public void orderingTest() {
        store.save(entities[0]);
        store.save(entities[1]);
        store.save(entities[2]);


        // ASC
        Params params = new Params();
        params.setOrder(Order.ASC);

        Result<TestEntity> result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), contains(entities));

        // DESC
        params = new Params();
        params.setOrder(Order.DESC);

        result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), contains(reverse(entities)));
    }

    @Test
    public void multisortTest() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setStringAttribute("holmes");
        entities[0].setIntAttribute(3);
        entities[1].setStringAttribute("string");
        entities[1].setIntAttribute(2);
        entities[2].setStringAttribute("string");
        entities[2].setIntAttribute(1);

        store.save(asList(entities));


        // ASC ASC
        Params params = new Params();
        List<SortSpecification> sortingSpec = new ArrayList<>();
        sortingSpec.add(new SortSpecification("stringAttribute", Order.ASC));
        sortingSpec.add(new SortSpecification("intAttribute", Order.ASC));
        params.setSorting(sortingSpec);

        Result<TestEntity> result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), contains(entities[0], entities[2], entities[1]));

        // DESC DESC
        params = new Params();
        sortingSpec = new ArrayList<>();
        sortingSpec.add(new SortSpecification("stringAttribute", Order.DESC));
        sortingSpec.add(new SortSpecification("intAttribute", Order.DESC));
        params.setSorting(sortingSpec);

        result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), contains(entities[1], entities[2], entities[0]));

        // ASC DESC
        params = new Params();
        sortingSpec = new ArrayList<>();
        sortingSpec.add(new SortSpecification("stringAttribute", Order.ASC));
        sortingSpec.add(new SortSpecification("intAttribute", Order.DESC));
        params.setSorting(sortingSpec);

        result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), contains(entities[0], entities[1], entities[2]));

        // DESC ASC
        params = new Params();
        sortingSpec = new ArrayList<>();
        sortingSpec.add(new SortSpecification("stringAttribute", Order.DESC));
        sortingSpec.add(new SortSpecification("intAttribute", Order.ASC));
        params.setSorting(sortingSpec);

        result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), contains(entities[2], entities[1], entities[0]));
    }

    @Test
    public void sortingStringTestWithMultisortEmpty() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setStringAttribute("holmes");
        entities[1].setStringAttribute("string");
        entities[2].setStringAttribute("abraka-dabra");

        store.save(asList(entities));


        // ASC
        Params params = new Params();
        params.setSort("stringAttribute");
        params.setOrder(Order.ASC);

        List<SortSpecification> sortingSpec = new ArrayList<>();
        params.setSorting(sortingSpec);

        Result<TestEntity> result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), contains(entities[2], entities[0], entities[1]));

        // DESC
        params = new Params();
        params.setSort("stringAttribute");
        params.setOrder(Order.DESC);

        sortingSpec = new ArrayList<>();
        params.setSorting(sortingSpec);

        result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), contains(entities[1], entities[0], entities[2]));
    }

    @Test
    public void sortingStringTest() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setStringAttribute("holmes");
        entities[1].setStringAttribute("string");
        entities[2].setStringAttribute("abraka-dabra");

        store.save(asList(entities));


        // ASC
        Params params = new Params();
        params.setSort("stringAttribute");
        params.setOrder(Order.ASC);

        Result<TestEntity> result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), contains(entities[2], entities[0], entities[1]));

        // DESC
        params = new Params();
        params.setSort("stringAttribute");
        params.setOrder(Order.DESC);

        result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), contains(entities[1], entities[0], entities[2]));
    }

    @Test
    public void sortingIntTest() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setIntAttribute(2);
        entities[1].setIntAttribute(0);
        entities[2].setIntAttribute(10);

        store.save(asList(entities));


        // ASC
        Params params = new Params();
        params.setSort("intAttribute");
        params.setOrder(Order.ASC);

        Result<TestEntity> result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), contains(entities[1], entities[0], entities[2]));

        // DESC
        params = new Params();
        params.setSort("intAttribute");
        params.setOrder(Order.DESC);

        result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), contains(entities[2], entities[0], entities[1]));
    }

    @Test
    public void sortingDoubleTest() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setDoubleAttribute(5.0);
        entities[1].setDoubleAttribute(4.0);
        entities[2].setDoubleAttribute(200.0);

        store.save(asList(entities));


        // ASC
        Params params = new Params();
        params.setSort("doubleAttribute");
        params.setOrder(Order.ASC);

        Result<TestEntity> result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), contains(entities[1], entities[0], entities[2]));

        // DESC
        params = new Params();
        params.setSort("doubleAttribute");
        params.setOrder(Order.DESC);

        result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), contains(entities[2], entities[0], entities[1]));
    }

    @Test
    public void sortingLocalDateTest() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity()};
        LocalDate now = LocalDate.now();

        entities[0].setLocalDateAttribute(now.plusDays(3));
        entities[1].setLocalDateAttribute(now.plusDays(200));
        entities[2].setLocalDateAttribute(now.minusDays(5));

        //storeAip.save(asList(entities));
        store.save(entities[0]);
        store.save(entities[1]);
        store.save(entities[2]);


        // ASC
        Params params = new Params();
        params.setSort("localDateAttribute");
        params.setOrder(Order.ASC);

        Result<TestEntity> result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), contains(entities[2], entities[0], entities[1]));

        // DESC
        params = new Params();
        params.setSort("localDateAttribute");
        params.setOrder(Order.DESC);

        result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), contains(entities[1], entities[0], entities[2]));
    }

    @Test
    public void sortingInstantTest() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity()};
        Instant now = Instant.now();
        entities[0].setInstantAttribute(now.plusMillis(2000));
        entities[1].setInstantAttribute(now);
        entities[2].setInstantAttribute(now.plusMillis(1000));

        store.save(asList(entities));


        // ASC
        Params params = new Params();
        params.setSort("instantAttribute");
        params.setOrder(Order.ASC);

        Result<TestEntity> result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), contains(entities[1], entities[2], entities[0]));

        // DESC
        params = new Params();
        params.setSort("instantAttribute");
        params.setOrder(Order.DESC);

        result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), contains(entities[0], entities[2], entities[1]));
    }

    @Test
    @Ignore
    public void sortingDependentTest() {
        DependentEntity[] dependents = new DependentEntity[]{new DependentEntity(), new DependentEntity(), new DependentEntity()};
        dependents[0].setName("holmes");
        dependents[1].setName("string");
        dependents[2].setName("abraka-dabra");

        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity()};
//        entities[0].setDependent(dependents[0]);
//        entities[1].setDependent(dependents[1]);
//        entities[2].setDependent(dependents[2]);

        store.save(asList(entities));


        // ASC
        Params params = new Params();
        params.setSort("dependent.name");
        params.setOrder(Order.ASC);

        Result<TestEntity> result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), contains(entities[2], entities[0], entities[1]));

        // DESC
        params = new Params();
        params.setSort("dependent.name");
        params.setOrder(Order.DESC);

        result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), contains(entities[1], entities[0], entities[2]));
    }

    @Test
    public void filteringStringTest() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setStringAttribute("abrasive");
        entities[1].setStringAttribute("kadabra");
        entities[2].setStringAttribute("abraka-dabra");

        store.save(asList(entities));


        // EQ
        Params params = new Params();
        params.setFilter(asList(new Filter("stringAttribute", FilterOperation.EQ, "kadabra", null)));

        Result<TestEntity> result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[1]));

        // NEQ
        params = new Params();
        params.setFilter(asList(new Filter("stringAttribute", FilterOperation.NEQ, "kadabra", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[2], entities[3]));

        // STARTSWITH
        params = new Params();
        params.setFilter(asList(new Filter("stringAttribute", FilterOperation.STARTWITH, "abra", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[2]));

        // STARTSWITH
        params = new Params();
        params.setFilter(asList(new Filter("foldingAttribute", FilterOperation.ENDWITH, "abra", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[1], entities[2]));

        // CONTAINS
        params = new Params();
        params.setFilter(asList(new Filter("stringAttribute", FilterOperation.CONTAINS, "dab", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[1], entities[2]));

        // IS_NULL
        params = new Params();
        params.setFilter(asList(new Filter("stringAttribute", FilterOperation.IS_NULL, null, null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[3]));

        // NOT_NULL
        params = new Params();
        params.setFilter(asList(new Filter("stringAttribute", FilterOperation.NOT_NULL, null, null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[1], entities[2]));
    }

    @Test
    public void filteringIntTest() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setIntAttribute(5);
        entities[1].setIntAttribute(10);
        entities[2].setIntAttribute(100);

        store.save(asList(entities));


        // EQ
        Params params = new Params();
        params.setFilter(asList(new Filter("intAttribute", FilterOperation.EQ, "5", null)));

        Result<TestEntity> result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0]));

        // NEQ
        params = new Params();
        params.setFilter(asList(new Filter("intAttribute", FilterOperation.NEQ, "5", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), containsInAnyOrder(entities[1], entities[2], entities[3]));

        // LT
        params = new Params();
        params.setFilter(asList(new Filter("intAttribute", FilterOperation.LT, "100", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[1]));

        // GT
        params = new Params();
        params.setFilter(asList(new Filter("intAttribute", FilterOperation.GT, "5", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[1], entities[2]));

        // LTE
        params = new Params();
        params.setFilter(asList(new Filter("intAttribute", FilterOperation.LTE, "10", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[1]));

        // GTE
        params = new Params();
        params.setFilter(asList(new Filter("intAttribute", FilterOperation.GTE, "10", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[1], entities[2]));

        // IS_NULL
        params = new Params();
        params.setFilter(asList(new Filter("intAttribute", FilterOperation.IS_NULL, null, null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[3]));

        // NOT_NULL
        params = new Params();
        params.setFilter(asList(new Filter("intAttribute", FilterOperation.NOT_NULL, null, null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[1], entities[2]));
    }

    @Test
    public void filteringDoubleTest() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setDoubleAttribute(5.0);
        entities[1].setDoubleAttribute(10.0);
        entities[2].setDoubleAttribute(100.0);

        store.save(asList(entities));


        // EQ
        Params params = new Params();
        params.setFilter(asList(new Filter("doubleAttribute", FilterOperation.EQ, "5.0", null)));

        Result<TestEntity> result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0]));

        // NEQ
        params = new Params();
        params.setFilter(asList(new Filter("doubleAttribute", FilterOperation.NEQ, "5.0", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), containsInAnyOrder(entities[1], entities[2], entities[3]));

        // LT
        params = new Params();
        params.setFilter(asList(new Filter("doubleAttribute", FilterOperation.LT, "100.0", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[1]));

        // GT
        params = new Params();
        params.setFilter(asList(new Filter("doubleAttribute", FilterOperation.GT, "5.0", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[1], entities[2]));

        // LTE
        params = new Params();
        params.setFilter(asList(new Filter("doubleAttribute", FilterOperation.LTE, "10.0", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[1]));

        // GTE
        params = new Params();
        params.setFilter(asList(new Filter("doubleAttribute", FilterOperation.GTE, "10.0", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[1], entities[2]));

        // IS_NULL
        params = new Params();
        params.setFilter(asList(new Filter("doubleAttribute", FilterOperation.IS_NULL, null, null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[3]));

        // NOT_NULL
        params = new Params();
        params.setFilter(asList(new Filter("doubleAttribute", FilterOperation.NOT_NULL, null, null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[1], entities[2]));
    }

    @Test
    public void filteringLocalDateTest() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity(), new TestEntity()};
        LocalDate now = LocalDate.parse("2016-11-21");
        entities[0].setLocalDateAttribute(now.plusDays(5));
        entities[1].setLocalDateAttribute(now.plusDays(10));
        entities[2].setLocalDateAttribute(now.plusDays(100));

        store.save(asList(entities));


        // EQ
        Params params = new Params();
        params.setFilter(asList(new Filter("localDateAttribute", FilterOperation.EQ, "2016-11-26T00:00:00Z", null)));

        Result<TestEntity> result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0]));

        // NEQ
        params = new Params();
        params.setFilter(asList(new Filter("localDateAttribute", FilterOperation.NEQ, "2016-11-26T00:00:00Z", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), containsInAnyOrder(entities[1], entities[2], entities[3]));

        // LT
        params = new Params();
        params.setFilter(asList(new Filter("localDateAttribute", FilterOperation.LT, "2017-03-01T00:00:00Z", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[1]));

        // GT
        params = new Params();
        params.setFilter(asList(new Filter("localDateAttribute", FilterOperation.GT, "2016-11-26T00:00:00Z", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[1], entities[2]));

        // LTE
        params = new Params();
        params.setFilter(asList(new Filter("localDateAttribute", FilterOperation.LTE, "2016-12-01T00:00:00Z", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[1]));

        // GTE
        params = new Params();
        params.setFilter(asList(new Filter("localDateAttribute", FilterOperation.GTE, "2016-12-01T00:00:00Z", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[1], entities[2]));

        // IS_NULL
        params = new Params();
        params.setFilter(asList(new Filter("localDateAttribute", FilterOperation.IS_NULL, null, null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[3]));

        // NOT_NULL
        params = new Params();
        params.setFilter(asList(new Filter("localDateAttribute", FilterOperation.NOT_NULL, null, null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[1], entities[2]));
    }

    @Test
    public void filteringInstantTest() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity(), new TestEntity()};
        Instant now = Instant.parse("2016-11-21T00:00:00Z");
        entities[0].setInstantAttribute(now.plusSeconds(5));
        entities[1].setInstantAttribute(now.plusSeconds(10));
        entities[2].setInstantAttribute(now.plusSeconds(100));

        store.save(asList(entities));


        // EQ
        Params params = new Params();
        params.setFilter(asList(new Filter("instantAttribute", FilterOperation.EQ, "2016-11-21T00:00:05Z", null)));

        Result<TestEntity> result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0]));

        // NEQ
        params = new Params();
        params.setFilter(asList(new Filter("instantAttribute", FilterOperation.NEQ, "2016-11-21T00:00:05Z", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), containsInAnyOrder(entities[1], entities[2], entities[3]));

        // LT
        params = new Params();
        params.setFilter(asList(new Filter("instantAttribute", FilterOperation.LT, "2016-11-21T00:01:40Z", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[1]));

        // GT
        params = new Params();
        params.setFilter(asList(new Filter("instantAttribute", FilterOperation.GT, "2016-11-21T00:00:05Z", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[1], entities[2]));

        // LTE
        params = new Params();
        params.setFilter(asList(new Filter("instantAttribute", FilterOperation.LTE, "2016-11-21T00:00:10Z", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[1]));

        // GTE
        params = new Params();
        params.setFilter(asList(new Filter("instantAttribute", FilterOperation.GTE, "2016-11-21T00:00:10Z", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[1], entities[2]));

        // IS_NULL
        params = new Params();
        params.setFilter(asList(new Filter("instantAttribute", FilterOperation.IS_NULL, null, null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[3]));

        // NOT_NULL
        params = new Params();
        params.setFilter(asList(new Filter("instantAttribute", FilterOperation.NOT_NULL, null, null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[1], entities[2]));
    }

    @Test
    @Ignore
    public void filteringDependentTest() {
        DependentEntity[] dependents = new DependentEntity[]{new DependentEntity(), new DependentEntity(), new DependentEntity(), new DependentEntity()};
        dependents[0].setName("abrasive");
        dependents[1].setName("kadabra");
        dependents[2].setName("abraka-dabra");

        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setDependent(dependents[0]);
        entities[1].setDependent(dependents[1]);
        entities[2].setDependent(dependents[2]);
        entities[3].setDependent(dependents[3]);

        store.save(asList(entities));


        // EQ
        Params params = new Params();
        params.setFilter(asList(new Filter("dependent.name", FilterOperation.EQ, "kadabra", null)));

        Result<TestEntity> result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[1]));

        // NEQ
        params = new Params();
        params.setFilter(asList(new Filter("dependent.name", FilterOperation.NEQ, "kadabra", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[2], entities[3]));

        // STARTSWITH
        params = new Params();
        params.setFilter(asList(new Filter("dependent.name", FilterOperation.STARTWITH, "abra", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[2]));

        // STARTSWITH
        params = new Params();
        params.setFilter(asList(new Filter("dependent.name", FilterOperation.ENDWITH, "abra", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[1], entities[2]));

        // CONTAINS
        params = new Params();
        params.setFilter(asList(new Filter("dependent.name", FilterOperation.CONTAINS, "dab", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[1], entities[2]));

        // IS_NULL
        params = new Params();
        params.setFilter(asList(new Filter("dependent.name", FilterOperation.IS_NULL, null, null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[3]));

        // NOT_NULL
        params = new Params();
        params.setFilter(asList(new Filter("dependent.name", FilterOperation.NOT_NULL, null, null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[1], entities[2]));
    }

    @Test
    @Ignore
    public void filteringDependentTestsMultiValue() {
        //getTemplate().putMapping(SolrTestEntity.class);
        DependentEntity[] dependents = new DependentEntity[]{new DependentEntity(), new DependentEntity(), new DependentEntity(), new DependentEntity()};
        dependents[0].setName("abrasive");
        dependents[1].setName("kadabra");
        dependents[2].setName("abraka-dabra");

        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity(), new TestEntity()};
//        entities[0].setDependents(asSet(dependents[0], dependents[1]));
//        entities[1].setDependents(asSet(dependents[1]));
//        entities[2].setDependents(asSet(dependents[2]));
//        entities[3].setDependents(asSet(dependents[3]));

        store.save(asList(entities));


        Filter nameFilter = new Filter("dependents.name", FilterOperation.EQ, "kadabra", null);

        Filter nestedFilter = new Filter();
        nestedFilter.setField("dependents");
        nestedFilter.setOperation(FilterOperation.NESTED);
        nestedFilter.setFilter(asList(nameFilter));

        Filter negateFilter = new Filter();
        //negateFilter.setOperation(FilterOperation.NEGATE);
        negateFilter.setFilter(asList(nestedFilter));

        Params params = new Params();
        params.setFilter(asList(negateFilter));

        Result<TestEntity> result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[2], entities[3]));
    }

    @Test
    public void filteringANDORMultipleTest() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setStringAttribute("abrasive");
        entities[1].setStringAttribute("kadabra");
        entities[2].setStringAttribute("abraka-dabra");

        store.save(asList(entities));


        // Multiple
        Params params = new Params();
        params.setFilter(asList(
                new Filter("stringAttribute", FilterOperation.STARTWITH, "abra", null),
                new Filter("stringAttribute", FilterOperation.ENDWITH, "ive", null)
        ));

        Result<TestEntity> result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0]));

        // AND
        params = new Params();
        params.setFilter(asList(new Filter(null, FilterOperation.AND, null, asList(
                new Filter("stringAttribute", FilterOperation.STARTWITH, "abra", null),
                new Filter("stringAttribute", FilterOperation.ENDWITH, "ive", null)
        ))));

        result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0]));

        // OR
        params = new Params();
        params.addFilter(new Filter(null, FilterOperation.OR, null, asList(
                new Filter("stringAttribute", FilterOperation.STARTWITH, "abra", null),
                new Filter("stringAttribute", FilterOperation.ENDWITH, "-dabra", null)
        )));
        //params.addFilter();

        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[2]));
    }

    @Test
    public void filteringEmptyValueTest() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setStringAttribute("abrasive");
        entities[1].setStringAttribute("kadabra");
        entities[2].setStringAttribute("abraka-dabra");

        store.save(asList(entities));


        // EQ
        Params params = new Params();
        params.setFilter(asList(new Filter("stringAttribute", FilterOperation.EQ, null, null)));

        assertThrown(() -> store.findAll(params))
                .isInstanceOf(BadArgument.class);
    }

    @Test
    public void filteringEmptyTest() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setStringAttribute("abrasive");
        entities[1].setStringAttribute("kadabra");
        entities[2].setStringAttribute("abraka-dabra");

        store.save(asList(entities));


        // IN
        List<String> result = store.findEmptyInFilter();
        assertThat(result, empty());

        //NOT IN
        result = store.findEmptyNotInFilter();
        assertThat(result, containsInAnyOrder(entities[0].getId(), entities[1].getId(), entities[2].getId()));
    }

    @Test
    public void filteringEmptyOperationTest() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setStringAttribute("abrasive");
        entities[1].setStringAttribute("kadabra");
        entities[2].setStringAttribute("abraka-dabra");

        store.save(asList(entities));


        // EQ
        Params params = new Params();
        Filter filter = new Filter();
        filter.setField("stringAttribute");
        filter.setOperation(null);
        filter.setValue("test");
        params.setFilter(asList(filter));

        assertThrown(() -> store.findAll(params))
                .isInstanceOf(BadArgument.class);
    }

    @Test
    public void filteringNoFilterTest() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setStringAttribute("abrasive");
        entities[1].setStringAttribute("kadabra");
        entities[2].setStringAttribute("abraka-dabra");

        store.save(asList(entities));


        // EQ
        Params params = new Params();
        params.setFilter(null);

        Result<TestEntity> result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[1], entities[2]));
    }

    @Test
    public void filteringFoldingTest() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setStringAttribute("Čenko");
        entities[1].setStringAttribute("čenko");
        entities[2].setStringAttribute("Banan");

        store.save(asList(entities));


        // EQ exact
        Params params = new Params();
        params.setFilter(asList(new Filter("stringAttribute", FilterOperation.EQ, "čenko", null)));

        Result<TestEntity> result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[1]));

        // EQ exact
        params = new Params();
        params.setFilter(asList(new Filter("stringAttribute", FilterOperation.EQ, "Banan", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[2]));

        // EQ exact
        params = new Params();
        params.setFilter(asList(new Filter("stringAttribute", FilterOperation.EQ, "banan", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(0L));

        // EQ folding
        params = new Params();
        params.setFilter(asList(new Filter("foldingAttribute", FilterOperation.EQ, "cenko", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[1]));

        // EQ folding
        params = new Params();
        params.setFilter(asList(new Filter("foldingAttribute", FilterOperation.EQ, "Banan", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[2]));

        // EQ folding
        params = new Params();
        params.setFilter(asList(new Filter("foldingAttribute", FilterOperation.EQ, "banan", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[2]));
    }

    @Test
    @Ignore
    public void filteringNestedFolding() {
        //getTemplate().putMapping(SolrTestEntity.class);
        DependentEntity[] dependents = new DependentEntity[]{new DependentEntity(), new DependentEntity(), new DependentEntity(), new DependentEntity()};
        dependents[0].setId("Čenko");
        dependents[1].setId("čenko");
        dependents[2].setId("Banan");

        dependents[0].setName("Čenko");
        dependents[1].setName("čenko");
        dependents[2].setName("Banan");

        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity(), new TestEntity()};
//        entities[0].setDependent(dependents[0]);
//        entities[1].setDependent(dependents[1]);
//        entities[2].setDependent(dependents[2]);

        store.save(asList(entities));


        // EQ exact
        Params params = new Params();
        params.setFilter(asList(new Filter("dependent.id", FilterOperation.EQ, "čenko", null)));

        Result<TestEntity> result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[1]));

        // EQ exact
        params = new Params();
        params.setFilter(asList(new Filter("dependent.id", FilterOperation.EQ, "Banan", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[2]));

        // EQ exact
        params = new Params();
        params.setFilter(asList(new Filter("dependent.id", FilterOperation.EQ, "banan", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(0L));

        // EQ folding
        params = new Params();
        params.setFilter(asList(new Filter("dependent.name", FilterOperation.EQ, "čenko", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[1]));

        // EQ folding
        params = new Params();
        params.setFilter(asList(new Filter("dependent.name", FilterOperation.EQ, "Banan", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[2]));

        // EQ folding
        params = new Params();
        params.setFilter(asList(new Filter("dependent.name", FilterOperation.EQ, "banan", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[2]));
    }

    @Test
    public void pagingTest() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setStringAttribute("holmes");
        entities[1].setStringAttribute("string");
        entities[2].setStringAttribute("abraka-dabra");

        store.save(asList(entities));


        Params params = new Params();
        params.setPage(0);
        params.setPageSize(2);

        Result<TestEntity> result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), hasSize(2));

        params = new Params();
        params.setPage(1);
        params.setPageSize(2);

        result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), hasSize(1));
    }

    @Test
    public void pagingOverPage() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setStringAttribute("holmes");
        entities[1].setStringAttribute("string");
        entities[2].setStringAttribute("abraka-dabra");

        store.save(asList(entities));


        Params params = new Params();
        params.setPage(2);
        params.setPageSize(2);

        Result<TestEntity> result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), hasSize(0));
    }

    @Test
    public void pagingWrongPage() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setStringAttribute("holmes");
        entities[1].setStringAttribute("string");
        entities[2].setStringAttribute("abraka-dabra");

        store.save(asList(entities));


        Params params = new Params();
        params.setPage(-1);
        params.setPageSize(2);

        assertThrown(() -> store.findAll(params))
                .isInstanceOf(BadArgument.class);
    }

    @Test
    public void batchUserIdHandlingTest() throws IOException, SolrServerException {
        batchStore.reindex();
        Batch b = new Batch();
        Batch b2 = new Batch();
        batchStore.create(b, "userid");
        batchStore.save(asList(b, b2));
        Params p = new Params();
        p.setFilter(asList(new Filter("userId", FilterOperation.EQ, "userid", null)));
        Result<Batch> all = batchStore.findAll(p);
        getClient().deleteByQuery("batch", "id:(" + b.getId() + " " + b2.getId() + ")");
        getClient().commit("batch");
        assertThat(all.getItems(), hasSize(1));
        assertThat(all.getItems().get(0).getId(), is(b.getId()));
    }

    @Test
    public void emptyEqNeqTest() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setStringAttribute("holmes");
        entities[1].setStringAttribute("string");
        entities[2].setStringAttribute("abraka-dabra");

        store.save(asList(entities));
        SimpleQuery q = new SimpleQuery();
        q.addCriteria(Criteria.where(SolrArclibXmlDocument.ID));
        q.addProjectionOnField("id");
        q.addFilterQuery(new SimpleFilterQuery(SolrQueryUtils.inQuery("id", asSet())));
        Page<SolrTestEntity> res = getTemplate().query(store.getIndexCore(), q, store.getUType());
        assertThat(res.getTotalElements(), is(0L));

        q = new SimpleQuery();
        q.addCriteria(Criteria.where(SolrArclibXmlDocument.ID));
        q.addProjectionOnField("id");
        q.addFilterQuery(new SimpleFilterQuery(SolrQueryUtils.notInQuery("id", asSet())));
        res = getTemplate().query(store.getIndexCore(), q, store.getUType());
        assertThat(res.getTotalElements(), is(3L));
    }

    @Test
    public void nestedNeqQueries() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity(), new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setIntAttribute(0);
        entities[1].setIntAttribute(0);
        entities[2].setIntAttribute(10);
        entities[3].setIntAttribute(1);
        entities[4].setIntAttribute(1);
        entities[5].setIntAttribute(1);
        entities[0].setDoubleAttribute(0.0);
        entities[1].setDoubleAttribute(1.0);
        entities[2].setDoubleAttribute(1.0);
        entities[3].setDoubleAttribute(1.0);
        entities[4].setDoubleAttribute(1.0);
        entities[5].setDoubleAttribute(1.0);
        store.save(asList(entities));

        Params params = new Params();

        //except 1st
        Filter orNeqFilter = new Filter(null, FilterOperation.OR, null, asList(
                new Filter("intAttribute", FilterOperation.NEQ, "0", null),
                new Filter("doubleAttribute", FilterOperation.NEQ, "0", null)
        ));

        //all except 1st 5th 6th
        Filter andFilter = new Filter(null, FilterOperation.AND, null, asList(
                new Filter("id", FilterOperation.NEQ, entities[5].getId(), null),
                new Filter("id", FilterOperation.NEQ, entities[4].getId(), null),
                new Filter("id", FilterOperation.NEQ, entities[0].getId(), null)
        ));

        params.setFilter(asList(orNeqFilter, andFilter));

        //except 3rd
        addPrefilter(params, new Filter("intAttribute", FilterOperation.LTE, "5", null));
        addPrefilter(params, new Filter("intAttribute", FilterOperation.GTE, "-5", null));

        Result<TestEntity> result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[1],entities[3]));
    }

    @Test
    public void multiWordQuery() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setStringAttribute("Začátek krátký dlouhý Konec");
        entities[1].setStringAttribute("Začátek dlouhý Konec");
        entities[2].setStringAttribute("Začátek krátký Konec");

        store.save(asList(entities));

        Params params = new Params();
        params.setFilter(asList(new Filter("foldingAttribute", FilterOperation.CONTAINS, "krátký dlouhý", null)));
        Result<TestEntity> result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0]));

        params = new Params();
        params.setFilter(asList(new Filter("textAttribute", FilterOperation.STARTWITH, "začátek krátký ", null)));
        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[2]));

        params = new Params();
        params.setFilter(asList(new Filter("textAttribute", FilterOperation.ENDWITH, "dlouhý konec", null)));
        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[1]));

        //keyword tokenizer allows only full words search
        params = new Params();
        params.setFilter(asList(new Filter("textAttribute", FilterOperation.CONTAINS, "ký dlo", null)));
        result = store.findAll(params);
        assertThat(result.getCount(), is(0L));

        params = new Params();
        params.setFilter(asList(new Filter("foldingAttribute", FilterOperation.CONTAINS, "ký dlo", null)));
        result = store.findAll(params);
        assertThat(result.getCount(), is(0L));
    }

    @Test
    public void testPunctation() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setStringAttribute("chleba");
        entities[1].setStringAttribute("chleba.");
        entities[2].setStringAttribute("chle'ba");
        entities[3].setStringAttribute("chle ba");

        store.save(asList(entities));

        Params params = new Params();
        params.setFilter(asList(new Filter("foldingAttribute", FilterOperation.EQ, "chleba", null)));
        Result<TestEntity> result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[1]));

        params = new Params();
        params.setFilter(asList(new Filter("textAttribute", FilterOperation.EQ, "chleba", null)));
        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[1]));

        params = new Params();
        params.setFilter(asList(new Filter("textAttribute", FilterOperation.EQ, "chleba.", null)));
        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[1]));

        params = new Params();
        params.setFilter(asList(new Filter("foldingAttribute", FilterOperation.EQ, "chleba.", null)));
        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[1]));

        params = new Params();
        params.setFilter(asList(new Filter("textAttribute", FilterOperation.EQ, "chle'ba", null)));
        result = store.findAll(params);
        assertThat(result.getCount(), is(1L));

        params = new Params();
        params.setFilter(asList(new Filter("foldingAttribute", FilterOperation.EQ, "chle'ba.", null)));
        result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
    }
}
