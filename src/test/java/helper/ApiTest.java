package helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.core.rest.config.ResourceExceptionHandler;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

public interface ApiTest {
    default ObjectMapper mapper() {
        return new ObjectMapper();
    }

    default String toJson(Object o) throws JsonProcessingException {
        return mapper().writeValueAsString(o);
    }

    default MockMvc mvc(Object controller) {
        return standaloneSetup(controller)
                .setControllerAdvice(new ResourceExceptionHandler())
                .build();
    }
}
