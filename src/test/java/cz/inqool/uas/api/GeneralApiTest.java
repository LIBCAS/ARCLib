package cz.inqool.uas.api;

import cz.inqool.uas.index.dto.Filter;
import cz.inqool.uas.index.dto.FilterOperation;
import cz.inqool.uas.index.dto.Params;
import cz.inqool.uas.index.dto.Result;
import cz.inqool.uas.rest.data.DataAdapter;
import helper.ApiTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;

import static cz.inqool.uas.util.Utils.asList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class GeneralApiTest implements ApiTest {
    @Mock
    private DataAdapter<GeneralEntity> adapter;

    private GeneralApiImpl api;

    private GeneralEntity entity;

    private String missingId;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        api = new GeneralApiImpl();
        api.setAdapter(adapter);

        entity = new GeneralEntity();
        entity.setId("existing");
        entity.setStringAtt("test1");

        missingId = "missing";

        Result<GeneralEntity> result = new Result<>();
        result.setCount(2L);
        result.setItems(asList(entity));

        when(adapter.getType()).thenReturn(GeneralEntity.class);
        when(adapter.find(entity.getId())).thenReturn(entity);
        when(adapter.find(missingId)).thenReturn(null);
        when(adapter.findAll(any())).thenReturn(result);
    }

    @Test
    public void getTest() throws Exception {
        mvc(api)
                .perform(get("/api/test/general/{1}", entity.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(entity.getId()))
                .andExpect(jsonPath("$.stringAtt").value(entity.getStringAtt()));
    }

    @Test
    public void getMissingTest() throws Exception {
        mvc(api)
                .perform(get("/api/test/general/{1}", missingId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void saveTest() throws Exception {
        GeneralEntity entity = new GeneralEntity();
        entity.setId("new");

        mvc(api)
                .perform(put("/api/test/general/{1}", entity.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(entity))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(adapter, times(1)).save(entity);
    }

    @Test
    public void wrongIdSaveTest() throws Exception {
        GeneralEntity entity = new GeneralEntity();

        mvc(api)
                .perform(put("/api/test/general/{1}", "fictive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(entity))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(adapter, times(0)).save(entity);
    }

    @Test
    public void deleteTest() throws Exception {
        mvc(api)
                .perform(delete("/api/test/general/{1}", entity.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(adapter, times(1)).delete(entity);
    }

    @Test
    public void deleteMissingTest() throws Exception {
        mvc(api)
                .perform(delete("/api/test/general/{1}", missingId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(adapter, times(0)).delete(entity);
    }

    @Test
    public void listTest() throws Exception {
        Params params = new Params();
        params.setFilter(asList(
                new Filter("id", FilterOperation.EQ, entity.getId(), null)
        ));

        mvc(api)
                .perform(get("/api/test/general")
                        .param("sort", params.getSort())
                        .param("order", params.getOrder().toString())
                        .param("page", params.getPage().toString())
                        .param("pageSize", params.getPageSize().toString())
                        .param("filter[0].field", params.getFilter().get(0).getField())
                        .param("filter[0].operation", params.getFilter().get(0).getOperation().toString())
                        .param("filter[0].value", params.getFilter().get(0).getValue())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(entity.getId()))
                .andExpect(jsonPath("$.items[0].stringAtt").value(entity.getStringAtt()))
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    public void listDefaultParamsTest() throws Exception {
        Params params = new Params();
        params.setFilter(asList(
                new Filter("id", FilterOperation.EQ, entity.getId(), null)
        ));

        mvc(api)
                .perform(get("/api/test/general")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(entity.getId()))
                .andExpect(jsonPath("$.items[0].stringAtt").value(entity.getStringAtt()))
                .andExpect(jsonPath("$.count").value(2));
    }
}
