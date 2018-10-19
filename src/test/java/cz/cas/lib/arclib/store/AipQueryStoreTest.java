package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.AipQuery;
import cz.cas.lib.core.index.dto.*;
import helper.DbTest;
import org.junit.Before;
import org.junit.Test;

import static cz.cas.lib.core.util.Utils.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class AipQueryStoreTest extends DbTest {

    private AipQueryStore store;

    @Before
    public void setUp() {
        store = new AipQueryStore();
        initializeStores(store);
    }

    @Test
    public void storeTest() {
        AipQuery query = new AipQuery();
        query.setName("q");
        Params params = new Params();
        params.addFilter(new Filter("field", FilterOperation.EQ, "value", asList()));
        params.setPageSize(10);
        params.addSorting(new SortSpecification("blah", Order.ASC));
        query.setQuery(params);
        store.save(query);

        AipQuery fromDb = store.find(query.getId());
        assertThat(fromDb, is(query));
    }
}
