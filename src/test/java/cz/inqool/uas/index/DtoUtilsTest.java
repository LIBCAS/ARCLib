package cz.inqool.uas.index;

import cz.inqool.uas.index.dto.DtoUtils;
import cz.inqool.uas.index.dto.Result;
import org.junit.Test;

import java.util.Iterator;
import java.util.function.Function;

import static cz.inqool.uas.util.Utils.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class DtoUtilsTest {
    @Test
    public void transformTest() {
        Result result = new Result();
        result.setItems(asList(true, false, true));

        Function<Boolean, Boolean> negateFunction = (Boolean b) -> !b;

        Result transformedResult = DtoUtils.transform(result, negateFunction);

        Result expectedResult = new Result();
        expectedResult.setItems(asList(false, true, false));

        assertThat(transformedResult.getCount(), is(expectedResult.getCount()));

        Iterator iterator = expectedResult.getItems().iterator();
        Iterator iterator1 = transformedResult.getItems().iterator();

        Object next = iterator.next();
        Object next1 = iterator1.next();
        assertThat(next, is(next1));

        next = iterator.next();
        next1 = iterator1.next();
        assertThat(next, is(next1));

        next = iterator.next();
        next1 = iterator1.next();
        assertThat(next, is(next1));
    }
}
