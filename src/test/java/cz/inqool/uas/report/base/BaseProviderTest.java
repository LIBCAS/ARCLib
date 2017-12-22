package cz.inqool.uas.report.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.inqool.uas.config.ObjectMapperProducer;
import cz.inqool.uas.exception.GeneralException;
import cz.inqool.uas.report.provider.BaseProvider;
import org.junit.Before;
import org.junit.Test;

import java.util.Base64;
import java.util.Map;

import static cz.inqool.uas.util.Utils.asMap;
import static helper.ThrowableAssertion.assertThrown;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class BaseProviderTest {
    private MyProvider provider;

    @Before
    public void setUp() throws Exception {

        ObjectMapperProducer producer = new ObjectMapperProducer();
        ObjectMapper mapper = producer.objectMapper(false, false);

        provider = new MyProvider();
        provider.setObjectMapper(mapper);
    }

    @Test
    public void simpleTest() {
        Map<String, Object> result = provider.provide(asMap("value", "test1"));
        assertThat(result.get("value"), is("test1"));
        assertThat(result.get("result"), is("test1"));
    }

    @Test
    public void nullTest() {
        assertThrown(() -> provider.provide((Map<String, Object>) null))
                .isInstanceOf(GeneralException.class);
    }

    @Test
    public void convertFailedTest() {
        assertThrown(() -> provider.provide(asMap("value", Base64.getDecoder())))
                .isInstanceOf(GeneralException.class);
    }

    private static class MyProvider extends BaseProvider<Input> {

        MyProvider() {
            super(Input.class);
        }

        @Override
        public String getName() {
            return "Name";
        }

        @Override
        protected Map<String, Object> provide(Input input) {
            return asMap("result", input.value);
        }
    }

    private static class Input {
        public String value;
    }
}
