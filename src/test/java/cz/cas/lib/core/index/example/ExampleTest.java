package cz.cas.lib.core.index.example;

import com.querydsl.jpa.impl.JPAQueryFactory;
import cz.cas.lib.core.index.dto.Filter;
import cz.cas.lib.core.index.dto.FilterOperation;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import helper.SrDbTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

import static cz.cas.lib.core.util.Utils.asSet;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;


public class ExampleTest extends SrDbTest {

    private MyStore store;

    @Before
    public void testSetUp() throws Exception {
        super.testSetUp();

        MockitoAnnotations.initMocks(this);

        store = new MyStore();
        store.setEntityManager(getEm());
        store.setQueryFactory(new JPAQueryFactory(getEm()));
        store.setTemplate(getTemplate());
    }

    @Test
    public void test() throws IOException, SAXException, ParserConfigurationException {
        MyObject myObject1 = new MyObject();
        myObject1.setName("hofofogo");
        myObject1.setOrder(5L);
        myObject1.setActive(true);
        myObject1.setState(MyObjectState.FST);

        MyObject myObject2 = new MyObject();
        myObject2.setName("nogo");
        myObject2.setOrder(6L);
        myObject2.setActive(false);
        myObject2.setState(MyObjectState.SND);

        try {
            store.save(asSet(myObject1, myObject2));

            Params params = new Params();

            Filter nameFilter = new Filter("name", FilterOperation.CONTAINS, "ruh", null);
            Filter nested = new Filter("state", FilterOperation.NESTED, null, asList(nameFilter));

            //Filter nameFilter = new Filter("name", FilterOperation.NEQ, "hogofog2o", null);
            //Filter orderFilter = new Filter("order", FilterOperation.EQ, "6", null);

            //Filter combined = new Filter("", FilterOperation.NEGATE, null, asList(nameFilter, orderFilter));
            //Filter simple = new Filter("", FilterOperation.NEGATE, null, asList(nameFilter));

            params.setFilter(asList(nested));
            Result<MyObject> result = store.findAll(params);

            System.err.println("count: " + result.getCount());
            result.getItems().forEach(i -> {
                System.err.println(i.getId() + ": " + i.getName());
            });


        } finally {
            store.delete(myObject1);
            store.delete(myObject2);
        }
    }
}
