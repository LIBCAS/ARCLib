package cz.inqool.uas.sequence;

import cz.inqool.uas.exception.MissingObject;
import helper.ApiTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SequenceApiTest implements ApiTest {
    @Mock
    private Generator generator;

    private SequencesApi api;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        api = new SequencesApi();
        api.setGenerator(generator);

        when(generator.generate("existing")).thenReturn("generated1");
        when(generator.generate("missing")).thenThrow(new MissingObject(Sequence.class, "missing"));
    }

    @Test
    public void generateTest() throws Exception {
        mvc(api)
                .perform(post("/api/sequences/existing/generate")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("generated1"));
    }

    @Test
    public void generateMissingTest() throws Exception {
        mvc(api)
                .perform(post("/api/sequences/missing/generate")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
