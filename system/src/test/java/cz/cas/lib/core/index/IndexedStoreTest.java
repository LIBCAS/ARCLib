package cz.cas.lib.core.index;

import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.index.solr.IndexQueryUtils;
import cz.cas.lib.core.index.dto.*;
import cz.cas.lib.core.index.nested.ChildEntity;
import cz.cas.lib.core.index.nested.ChildEntityStore;
import cz.cas.lib.core.index.nested.ParentEntity;
import cz.cas.lib.core.index.nested.ParentEntityStore;
import cz.cas.lib.core.index.solr.IndexField;
import cz.cas.lib.core.index.solr.IndexFieldType;
import helper.SrDbTest;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.Before;
import org.junit.Test;

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
 * Requires <i>test</i> core with following custom types:
 * <p>
 * {@code
 * <fieldType name="keyword_folding" class="solr.TextField" multiValued="false">
 * <analyzer>
 * <tokenizer class="solr.KeywordTokenizerFactory"/>
 * <filter class="solr.LowerCaseFilterFactory"/>
 * <filter class="solr.ASCIIFoldingFilterFactory"/>
 * </analyzer>
 * </fieldType>
 *
 * <fieldType name="standard_folding" class="solr.TextField" positionIncrementGap="100" multiValued="false">
 * <analyzer>
 * <tokenizer class="solr.StandardTokenizerFactory"/>
 * <filter class="solr.LowerCaseFilterFactory"/>
 * <filter class="solr.ASCIIFoldingFilterFactory"/>
 * </analyzer>
 * </fieldType>
 *
 * <dynamicField name="*_sort" type="keyword_folding"/>
 * }
 * </p>
 */
public class IndexedStoreTest extends SrDbTest {
    private IndexedStoreImpl store = new IndexedStoreImpl();
    private ParentEntityStore parentEntityStore = new ParentEntityStore();
    private ChildEntityStore childEntityStore = new ChildEntityStore();

    private TestEntity[] entities;

    @Before
    public void setUpIndexedStoreTest() throws Exception {
        initializeStores(store, parentEntityStore, childEntityStore);
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
        entities[0].setTextualAttribute("holmes");
        entities[0].setIntAttribute(3);
        entities[1].setTextualAttribute("string");
        entities[1].setIntAttribute(2);
        entities[2].setTextualAttribute("string");
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
        entities[0].setTextualAttribute("holmes");
        entities[1].setTextualAttribute("string");
        entities[2].setTextualAttribute("abraka-dabra");

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
        entities[0].setTextualAttribute("holmes");
        entities[1].setTextualAttribute("string");
        entities[2].setTextualAttribute("abraka-dabra");

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

        //store.saveAndIndex(asList(entities));
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
    public void sortingFoldingTest() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setTextualAttribute("ahoj");
        entities[1].setTextualAttribute("Čávo");
        entities[2].setTextualAttribute("Nazdar");
        store.save(asList(entities));
        Params params = new Params();
        Result<TestEntity> result;

        params.setSort("foldingAttribute");
        params.setOrder(Order.ASC);
        result = store.findAll(params);
        assertThat(result.getItems(), contains(entities[0], entities[1], entities[2]));
        params.setOrder(Order.DESC);
        result = store.findAll(params);
        assertThat(result.getItems(), contains(entities[2], entities[1], entities[0]));
    }

    @Test
    public void sortingTextTest() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setTextualAttribute("ahoj");
        entities[1].setTextualAttribute("Čávo");
        entities[2].setTextualAttribute("Nazdar");
        store.save(asList(entities));
        Params params = new Params();
        Result<TestEntity> result;

        params.setSort("textAttributeWithStringCpyField");
        params.setOrder(Order.ASC);
        assertThrown(() -> store.findAll(params)).isInstanceOf(UnsupportedSearchParameterException.class);
    }

    @Test
    //requires test core to have dynamic *_sort field declared
    //the dynamic field doesn't have to be of collation type, keyword_folding is OK for test to pass
    public void sortingCopyFieldTest() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setTextualAttribute("ahoj");
        entities[1].setTextualAttribute("Čávo");
        entities[2].setTextualAttribute("Nazdar");
        store.save(asList(entities));
        Params params = new Params();
        Result<TestEntity> result;

        params.setSort("stringAttribute");
        params.setOrder(Order.ASC);
        result = store.findAll(params);
        assertThat(result.getItems(), contains(entities[2], entities[0], entities[1]));
        params.setOrder(Order.DESC);
        result = store.findAll(params);
        assertThat(result.getItems(), contains(entities[1], entities[0], entities[2]));

        params.setSort("customSortStringAttribute");
        params.setOrder(Order.ASC);
        result = store.findAll(params);
        assertThat(result.getItems(), contains(entities[0], entities[1], entities[2]));
        params.setOrder(Order.DESC);
        result = store.findAll(params);
        assertThat(result.getItems(), contains(entities[2], entities[1], entities[0]));
    }

    @Test
    public void filteringStringTest() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setTextualAttribute("abrasive");
        entities[1].setTextualAttribute("kadabra");
        entities[2].setTextualAttribute("abraka-dabra");

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

        // ENDSWITH
        params = new Params();
        params.setFilter(asList(new Filter("stringAttribute", FilterOperation.ENDWITH, "ra", null)));

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

        //IN
        params = new Params();
        params.setFilter(asList(new Filter("stringAttribute", FilterOperation.IN, "abrasive,abraka-dabra", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[2]));
    }

    @Test
    public void filteringFoldingTest() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setTextualAttribute("Čenko");
        entities[1].setTextualAttribute("čenko");
        entities[2].setTextualAttribute("Banan");
        entities[3].setTextualAttribute(null);

        store.save(asList(entities));
        Params params = new Params();
        Result<TestEntity> result;

        // EQ
        params.setFilter(asList(new Filter("foldingAttribute", FilterOperation.EQ, "čenko", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[1]));

        // NEQ
        params = new Params();
        params.setFilter(asList(new Filter("foldingAttribute", FilterOperation.NEQ, "čenko", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[2], entities[3]));

        // STARTSWITH
        params = new Params();
        params.setFilter(asList(new Filter("foldingAttribute", FilterOperation.STARTWITH, "če", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[1]));

        // ENDSWITH
        params = new Params();
        params.setFilter(asList(new Filter("foldingAttribute", FilterOperation.ENDWITH, "ko", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[1]));

        // CONTAINS
        params = new Params();
        params.setFilter(asList(new Filter("foldingAttribute", FilterOperation.CONTAINS, "na", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[2]));

        // IS_NULL
        params = new Params();
        params.setFilter(asList(new Filter("foldingAttribute", FilterOperation.IS_NULL, null, null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[3]));

        // NOT_NULL
        params = new Params();
        params.setFilter(asList(new Filter("foldingAttribute", FilterOperation.NOT_NULL, null, null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[1], entities[2]));
    }

    @Test
    public void filteringTextTest() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setTextualAttribute("Čenko");
        entities[1].setTextualAttribute("čenko");
        entities[2].setTextualAttribute("Banan");
        entities[3].setTextualAttribute(null);

        store.save(asList(entities));
        Params params = new Params();
        Result<TestEntity> result;

        // EQ
        params.setFilter(asList(new Filter("textAttribute", FilterOperation.EQ, "čenko", null)));
        Params paramscpy = params;
        assertThrown(() -> store.findAll(paramscpy)).isInstanceOf(UnsupportedSearchParameterException.class);

        params.setFilter(asList(new Filter("textAttributeWithStringCpyField", FilterOperation.EQ, "čenko", null)));
        result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[1]));

        // NEQ
        params = new Params();
        params.setFilter(asList(new Filter("textAttribute", FilterOperation.NEQ, "čenko", null)));
        Params paramscpy2 = params;
        assertThrown(() -> store.findAll(paramscpy2)).isInstanceOf(UnsupportedSearchParameterException.class);

        params.setFilter(asList(new Filter("textAttributeWithStringCpyField", FilterOperation.NEQ, "Čenko", null)));
        result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), containsInAnyOrder(entities[1], entities[2], entities[3]));

        // STARTSWITH
        params.setFilter(asList(new Filter("textAttribute", FilterOperation.EQ, "čenko", null)));
        Params paramscpy3 = params;
        assertThrown(() -> store.findAll(paramscpy3)).isInstanceOf(UnsupportedSearchParameterException.class);

        params = new Params();
        params.setFilter(asList(new Filter("textAttributeWithStringCpyField", FilterOperation.STARTWITH, "če", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[1]));

        // ENDSWITH
        params.setFilter(asList(new Filter("textAttribute", FilterOperation.EQ, "čenko", null)));
        Params paramscpy4 = params;
        assertThrown(() -> store.findAll(paramscpy4)).isInstanceOf(UnsupportedSearchParameterException.class);

        params = new Params();
        params.setFilter(asList(new Filter("textAttributeWithStringCpyField", FilterOperation.ENDWITH, "ko", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[1]));

        // CONTAINS
        params = new Params();
        params.setFilter(asList(new Filter("textAttribute", FilterOperation.CONTAINS, "banan", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[2]));

        params = new Params();
        params.setFilter(asList(new Filter("textAttribute", FilterOperation.CONTAINS, "na", null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(0L));

        // IS_NULL
        params = new Params();
        params.setFilter(asList(new Filter("textAttribute", FilterOperation.IS_NULL, null, null)));

        result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[3]));

        // NOT_NULL
        params = new Params();
        params.setFilter(asList(new Filter("textAttribute", FilterOperation.NOT_NULL, null, null)));

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
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[2]));
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
    public void netedEntitiesTest() {
        ParentEntity[] parents = new ParentEntity[]{
                new ParentEntity("parent has child x3"),
                new ParentEntity("parent has child x2"),
                new ParentEntity("parent has baby.. it does not speak yet"),
                new ParentEntity("parent has no child.. sad")};

        parentEntityStore.save(asList(parents));
        ChildEntity[] children = new ChildEntity[]{
                new ChildEntity(parents[0], "child says: Ňuňuňu"),
                new ChildEntity(parents[0], "child says: šelišel"),
                new ChildEntity(parents[0], "child says: ahoj"),
                new ChildEntity(parents[1], "boy says: nununu"),
                new ChildEntity(parents[1], "girl says: hoj"),
                new ChildEntity(parents[2], null)
        };
        childEntityStore.save(asList(children));
        parents[0].setChildren(asSet(children[0], children[1], children[2]));
        parents[1].setChildren(asSet(children[3], children[4]));
        parents[2].setChildren(asSet(children[5]));
        parentEntityStore.save(asList(parents));

        Params params;
        Result<ParentEntity> result;

        // CONTAINS
        params = new Params();
        params.setSort("attribute");
        Filter childFilter = new Filter("attribute", FilterOperation.CONTAINS, "child", asList());
        Filter nestedFilter = new Filter(childEntityStore.getIndexType(), FilterOperation.NESTED, null, asList(childFilter));
        params.setFilter(asList(nestedFilter));
        result = parentEntityStore.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), contains(parents[0]));

        // CONTAINS
        params = new Params();
        params.setSort("attribute");
        childFilter = new Filter(null, FilterOperation.AND, null, asList(
                new Filter("attribute", FilterOperation.CONTAINS, "says", asList()),
                new Filter("attribute", FilterOperation.NEQ, "child says: ahoj", asList())
        ));
        nestedFilter = new Filter(childEntityStore.getIndexType(), FilterOperation.NESTED, null, asList(childFilter));
        params.setFilter(asList(nestedFilter));
        result = parentEntityStore.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), contains(parents[0], parents[1]));

        // CONTAINS
        params = new Params();
        params.setSort("attribute");
        childFilter = new Filter(null, FilterOperation.OR, null, asList(
                new Filter("attribute", FilterOperation.CONTAINS, "boy says", asList()),
                new Filter("attribute", FilterOperation.IS_NULL, null, asList())
        ));
        nestedFilter = new Filter(childEntityStore.getIndexType(), FilterOperation.NESTED, null, asList(childFilter));
        params.setFilter(asList(nestedFilter));
        result = parentEntityStore.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), contains(parents[1], parents[2]));

        // unmapped child
        Params params1 = new Params();
        params1.setSort("attribute");
        childFilter = new Filter("attribute", FilterOperation.CONTAINS, "children", asList());
        nestedFilter = new Filter("unknown mapping", FilterOperation.NESTED, null, asList(childFilter));
        params1.setFilter(asList(nestedFilter));
        assertThrown(() -> parentEntityStore.findAll(params1)).isInstanceOf(UnsupportedSearchParameterException.class);

        //unmapped child attribute
        Params params2 = new Params();
        params2.setSort("attribute");
        childFilter = new Filter("unknown attribute", FilterOperation.CONTAINS, "children", asList());
        nestedFilter = new Filter(childEntityStore.getIndexType(), FilterOperation.NESTED, null, asList(childFilter));
        params2.setFilter(asList(nestedFilter));
        assertThrown(() -> parentEntityStore.findAll(params2)).isInstanceOf(UnsupportedSearchParameterException.class);
    }

    @Test
    public void filteringANDORMultipleTest() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setTextualAttribute("abrasive");
        entities[1].setTextualAttribute("kadabra");
        entities[2].setTextualAttribute("abraka-dabra");

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
        entities[0].setTextualAttribute("abrasive");
        entities[1].setTextualAttribute("kadabra");
        entities[2].setTextualAttribute("abraka-dabra");

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
        entities[0].setTextualAttribute("abrasive");
        entities[1].setTextualAttribute("kadabra");
        entities[2].setTextualAttribute("abraka-dabra");

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
        entities[0].setTextualAttribute("abrasive");
        entities[1].setTextualAttribute("kadabra");
        entities[2].setTextualAttribute("abraka-dabra");

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
        entities[0].setTextualAttribute("abrasive");
        entities[1].setTextualAttribute("kadabra");
        entities[2].setTextualAttribute("abraka-dabra");

        store.save(asList(entities));


        // EQ
        Params params = new Params();
        params.setFilter(null);

        Result<TestEntity> result = store.findAll(params);
        assertThat(result.getCount(), is(3L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[1], entities[2]));
    }

    @Test
    public void pagingTest() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setTextualAttribute("holmes");
        entities[1].setTextualAttribute("string");
        entities[2].setTextualAttribute("abraka-dabra");

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
        entities[0].setTextualAttribute("holmes");
        entities[1].setTextualAttribute("string");
        entities[2].setTextualAttribute("abraka-dabra");

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
        entities[0].setTextualAttribute("holmes");
        entities[1].setTextualAttribute("string");
        entities[2].setTextualAttribute("abraka-dabra");

        store.save(asList(entities));


        Params params = new Params();
        params.setPage(-1);
        params.setPageSize(2);

        assertThrown(() -> store.findAll(params))
                .isInstanceOf(BadArgument.class);
    }

    @Test
    public void paginationDisabled() {
        TestEntity[] entities = new TestEntity[]{
                new TestEntity(),
                new TestEntity(),
                new TestEntity(),
                new TestEntity(),
                new TestEntity(),
                new TestEntity(),
                new TestEntity(),
                new TestEntity(),
                new TestEntity(),
                new TestEntity(),
                new TestEntity(),
                new TestEntity(),
                new TestEntity(),};
        store.save(asList(entities));
        Params params = new Params();
        params.setPage(6);
        params.setPageSize(null);
        assertThat(store.findAll(params).getItems(), hasSize(13));
        params.setPageSize(0);
        assertThat(store.findAll(params).getItems(), hasSize(13));
        params.setPageSize(1);
        assertThat(store.findAll(params).getItems(), hasSize(1));
    }

    @Test
    public void emptyEqNeqTest() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setTextualAttribute("holmes");
        entities[1].setTextualAttribute("string");
        entities[2].setTextualAttribute("abraka-dabra");

        store.save(asList(entities));

        IndexField indexedField = new IndexField();
        indexedField.setFieldName("id");
        indexedField.setKeywordField("id");
        indexedField.setFieldType(IndexFieldType.STRING);
        SolrQuery q = new SolrQuery("*:*");
        q.addField("id");
        q.addFilterQuery(IndexQueryUtils.inQuery(indexedField, asSet()));
        QueryResponse queryResponse;
        try {
            queryResponse = getClient().query(store.getIndexCollection(), q);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
        assertThat(queryResponse.getResults().getNumFound(), is(0L));

        q = new SolrQuery("*:*");
        q.addField("id");
        q.addFilterQuery(IndexQueryUtils.notInQuery(indexedField, asSet()));
        try {
            queryResponse = getClient().query(store.getIndexCollection(), q);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
        assertThat(queryResponse.getResults().getNumFound(), is(3L));
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
        assertThat(result.getItems(), containsInAnyOrder(entities[1], entities[3]));
    }

    @Test
    public void nestedIsNullQueries() {
        TestEntity[] entities = new TestEntity[]{
                new TestEntity(),
                new TestEntity(),
                new TestEntity()};
        entities[0].setIntAttribute(0);
        entities[1].setIntAttribute(null);
        entities[2].setIntAttribute(10);
        store.save(asList(entities));

        Params params = new Params();

        Filter filter = new Filter(null, FilterOperation.OR, null, asList(
                new Filter("intAttribute", FilterOperation.IS_NULL, null, null),
                new Filter("intAttribute", FilterOperation.GT, "0", null)
        ));

        params.setFilter(asList(filter));

        Result<TestEntity> result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[1], entities[2]));
    }

    @Test
    public void multiWordQuery() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setTextualAttribute("Začátek krátký dlouhý Konec");
        entities[1].setTextualAttribute("Začátek dlouhý Konec");
        entities[2].setTextualAttribute("Začátek krátký Konec");

        store.save(asList(entities));

        Params params = new Params();
        params.setFilter(asList(new Filter("foldingAttribute", FilterOperation.CONTAINS, "kratky dlouhy", null)));
        Result<TestEntity> result = store.findAll(params);
        assertThat(result.getCount(), is(1L));

        params = new Params();
        params.setFilter(asList(new Filter("foldingAttribute", FilterOperation.CONTAINS, "ky dlo", null)));
        result = store.findAll(params);
        assertThat(result.getCount(), is(1L));

        params = new Params();
        params.setFilter(asList(new Filter("foldingAttribute", FilterOperation.CONTAINS, "my dlo", null)));
        result = store.findAll(params);
        assertThat(result.getCount(), is(0L));

        params.setFilter(asList(new Filter("textAttributeWithStringCpyField", FilterOperation.CONTAINS, "kratky dlouhy", null)));
        result = store.findAll(params);
        assertThat(result.getCount(), is(1L));

        params = new Params();
        params.setFilter(asList(new Filter("textAttributeWithStringCpyField", FilterOperation.CONTAINS, "ky dlo", null)));
        result = store.findAll(params);
        assertThat(result.getCount(), is(0L));

        params = new Params();
        params.setFilter(asList(new Filter("textAttribute", FilterOperation.CONTAINS, "kratky dlouhy", null)));
        result = store.findAll(params);
        assertThat(result.getCount(), is(1L));

        params.setFilter(asList(new Filter("stringAttribute", FilterOperation.CONTAINS, "kratky dlouhy", null)));
        result = store.findAll(params);
        assertThat(result.getCount(), is(0L));

        params = new Params();
        params.setFilter(asList(new Filter("stringAttribute", FilterOperation.CONTAINS, "ky dlo", null)));
        result = store.findAll(params);
        assertThat(result.getCount(), is(0L));

        params.setFilter(asList(new Filter("stringAttribute", FilterOperation.CONTAINS, "krátký dlouhý", null)));
        result = store.findAll(params);
        assertThat(result.getCount(), is(1L));

        params = new Params();
        params.setFilter(asList(new Filter("stringAttribute", FilterOperation.CONTAINS, "ký dlo", null)));
        result = store.findAll(params);
        assertThat(result.getCount(), is(1L));

        params = new Params();
        params.setFilter(asList(new Filter("foldingAttribute", FilterOperation.STARTWITH, "zacatek kratky ", null)));
        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[2]));

        params = new Params();
        params.setFilter(asList(new Filter("stringAttribute", FilterOperation.STARTWITH, "Začátek krátký ", null)));
        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[2]));

        params = new Params();
        params.setFilter(asList(new Filter("textAttributeWithStringCpyField", FilterOperation.ENDWITH, "dlouhý Konec", null)));
        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[1]));

        params = new Params();
        params.setFilter(asList(new Filter("stringAttribute", FilterOperation.ENDWITH, "dlouhý Konec", null)));
        result = store.findAll(params);
        assertThat(result.getCount(), is(2L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0], entities[1]));
    }

    @Test
    public void testPunctation() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setTextualAttribute("chleba");
        entities[1].setTextualAttribute("chleba.");
        entities[2].setTextualAttribute("chle'ba");
        entities[3].setTextualAttribute("chle ba");

        store.save(asList(entities));

        Params params = new Params();
        params.setFilter(asList(new Filter("foldingAttribute", FilterOperation.EQ, "chleba", null)));
        Result<TestEntity> result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0]));

        params = new Params();
        params.setFilter(asList(new Filter("textAttributeWithStringCpyField", FilterOperation.EQ, "chleba", null)));
        result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[0]));

        params = new Params();
        params.setFilter(asList(new Filter("textAttributeWithStringCpyField", FilterOperation.EQ, "chleba.", null)));
        result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[1]));

        params = new Params();
        params.setFilter(asList(new Filter("foldingAttribute", FilterOperation.EQ, "chleba.", null)));
        result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[1]));

        params = new Params();
        params.setFilter(asList(new Filter("textAttributeWithStringCpyField", FilterOperation.EQ, "chle'ba", null)));
        result = store.findAll(params);
        assertThat(result.getCount(), is(1L));

        params = new Params();
        params.setFilter(asList(new Filter("foldingAttribute", FilterOperation.EQ, "chle'ba.", null)));
        result = store.findAll(params);
        assertThat(result.getCount(), is(0L));

        params = new Params();
        params.setFilter(asList(new Filter("textAttributeWithStringCpyField", FilterOperation.EQ, "chle'ba.", null)));
        result = store.findAll(params);
        assertThat(result.getCount(), is(0L));
    }

    @Test
    public void negateQuery() {
        TestEntity[] entities = new TestEntity[]{new TestEntity(), new TestEntity(), new TestEntity()};
        entities[0].setTextualAttribute("holmes");
        entities[1].setTextualAttribute("holmes");
        entities[2].setTextualAttribute("abraka-dabra");
        entities[0].setIntAttribute(1);
        entities[1].setIntAttribute(2);
        entities[2].setIntAttribute(3);

        store.save(asList(entities));

        Params params = new Params();
        params.setFilter(asList(new Filter(null, FilterOperation.NEGATE, null, asList(new Filter("stringAttribute", FilterOperation.EQ, "holmes", null)))));
        Result<TestEntity> result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[2]));

        params = new Params();
        params.setFilter(asList(
                        new Filter(null, FilterOperation.NEGATE, null, asList(
                                new Filter("stringAttribute", FilterOperation.EQ, "holmes", null),
                                new Filter("intAttribute", FilterOperation.EQ, "1", null)
                        )), new Filter("intAttribute", FilterOperation.NEQ, "3", null)
                )

        );


        result = store.findAll(params);
        assertThat(result.getCount(), is(1L));
        assertThat(result.getItems(), containsInAnyOrder(entities[1]));
    }
}
