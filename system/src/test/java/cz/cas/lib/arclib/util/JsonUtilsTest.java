package cz.cas.lib.arclib.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.arclib.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Slf4j
@RunWith(SpringRunner.class)
public class JsonUtilsTest {

    @Test
    public void simpleMergeTest() throws IOException {
        log.info("");
        log.info("Simple merge test: ");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode targetJson = mapper.readTree("{\n" +
                "  \"atribut1\": \"hodnota1\",\n" +
                "  \"atribut2\": \"hodnota2\"\n" +
                "}");
        log.info("Application JSON: " + targetJson);

        JsonNode sourceJson = mapper.readTree("{\n" +
                "  \"atribut2\": \"hodnota3\",\n" +
                "  \"atribut3\": \"hodnota4\"\n" +
                "}");
        log.info("Batch JSON: " + sourceJson);

        JsonNode mergedJson = JsonUtils.merge(targetJson, sourceJson);
        log.info("Result JSON: " + mergedJson);


        JsonNode expectedResult = mapper.readTree("{\"atribut1\":\"hodnota1\",\"atribut2\":\"hodnota3\"," +
                "\"atribut3\":\"hodnota4\"}}");

        assertThat(mergedJson, is(expectedResult));
    }

    @Test
    public void deepMergeTest() throws IOException {
        log.info("");
        log.info("Deep merge test: ");

        ObjectMapper mapper = new ObjectMapper();

        JsonNode targetJson = mapper.readTree("{\n" +
                "  \"atribut1\": \"hodnota1\",\n" +
                "  \"atribut2\": \"hodnota2\",\n" +
                "  \"atribut3\": {\n" +
                "    \"vnorenyAtribut1\": \"hodnota3\",\n" +
                "    \"vnorenyAtribut2\": \"hodnota4\"\n" +
                "  }\n" +
                "}");
        log.info("Application JSON: " + targetJson);

        JsonNode sourceJson = mapper.readTree("{\n" +
                "  \"atribut3\": {\n" +
                "    \"vnorenyAtribut1\": \"hodnota5\"\n" +
                "  }\n" +
                "}");
        log.info("Batch JSON: " + sourceJson);

        JsonNode mergedJson = JsonUtils.merge(targetJson, sourceJson);
        log.info("Result JSON: " + mergedJson);

        JsonNode expectedResult = mapper.readTree("{\"atribut1\":\"hodnota1\",\"atribut2\":\"hodnota2\"," +
                "\"atribut3\":{\"vnorenyAtribut1\":\"hodnota5\",\"vnorenyAtribut2\":\"hodnota4\"}}");

        assertThat(mergedJson, is(expectedResult));
    }

    @Test
    public void changedOrder() throws IOException {
        String overridingStr = "{\"atr1\":{\"nest1\":\"val1\",\"nes2\":\"val2\"},\"atr2\":3}";
        String overriddenStr = "{\"atr2\":4,\"atr1\":{\"nes2\":\"val5\",\"nest1\":\"val6\"},\"atr4\":7}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode overriding = mapper.readTree(overridingStr);
        JsonNode overridden = mapper.readTree(overriddenStr);
        JsonNode mergedJson = JsonUtils.merge(overridden, overriding);
        JsonNode expectedResult = mapper.readTree("{\"atr2\":3,\"atr1\":{\"nes2\":\"val2\",\"nest1\":\"val1\"},\"atr4\":7}}");
        assertThat(mergedJson, is(expectedResult));
    }

    @Test
    public void mergeArrayUniquePrimitives() throws IOException {
        String overridingStr = "{\"atr1\":[\"val1\"]}";
        String overriddenStr = "{\"atr1\":[\"val2\"]}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode overriding = mapper.readTree(overridingStr);
        JsonNode overridden = mapper.readTree(overriddenStr);
        JsonNode mergedJson = JsonUtils.merge(overridden, overriding);
        JsonNode expectedResult = mapper.readTree("{\"atr1\":[\"val2\",\"val1\"]}");
        assertThat(mergedJson, is(expectedResult));
    }

    @Test
    public void mergeArrayDuplicatedPrimitives() throws IOException {
        String overridingStr = "{\"atr1\":[\"val1\"]}";
        String overriddenStr = "{\"atr1\":[\"val1\"]}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode overriding = mapper.readTree(overridingStr);
        JsonNode overridden = mapper.readTree(overriddenStr);
        JsonNode mergedJson = JsonUtils.merge(overridden, overriding);
        JsonNode expectedResult = mapper.readTree("{\"atr1\":[\"val1\"]}");
        assertThat(mergedJson, is(expectedResult));
    }

    @Test
    public void mergeArrayComplex() throws IOException {
        String overridingStr = "{\"atr1\":[{\"nest1\":1},{\"nest2\":2}]}";
        String overriddenStr = "{\"atr1\":[{\"nest1\":1},{\"nest2\":3},{\"nest3\":4}]}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode overriding = mapper.readTree(overridingStr);
        JsonNode overridden = mapper.readTree(overriddenStr);
        JsonNode mergedJson = JsonUtils.merge(overridden, overriding);
        JsonNode expectedResult = mapper.readTree("{\"atr1\":[{\"nest1\":1},{\"nest2\":3},{\"nest3\":4},{\"nest2\":2}]}");
        assertThat(mergedJson, is(expectedResult));
    }
}
