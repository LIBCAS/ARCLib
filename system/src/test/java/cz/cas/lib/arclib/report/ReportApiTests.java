package cz.cas.lib.arclib.report;

import cz.cas.lib.arclib.api.ReportApi;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.core.store.Transactional;
import helper.ApiTest;
import helper.TransformerFactoryWorkaroundTest;
import helper.auth.WithMockCustomUser;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;


import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
@Rollback(false)
@WithMockCustomUser(permissions = {Permissions.REPORT_TEMPLATE_RECORDS_READ, Permissions.REPORT_TEMPLATE_RECORDS_WRITE})
public class ReportApiTests extends TransformerFactoryWorkaroundTest implements ApiTest {

    @Autowired
    private ReportApi api;
    @Autowired
    private ReportStore reportStore;
    @Rule
    public TestName name = new TestName();

    private static final String BASE = "/api/report";
    private static final Path TEMPLATES_PATH = Paths.get("./src/test/resources/report-templates");
    private static final String TEMPLATE_NAME = "model";
    private static final Path TEMPLATE_PATH = TEMPLATES_PATH.resolve(TEMPLATE_NAME + ".jrxml");
    private static final Path TEMPLATE_COMPILED_PATH = TEMPLATES_PATH.resolve(TEMPLATE_NAME + "-compiled");
    private static final String TEMPLATE_ID = "8f719ff7-8756-4101-9e87-42391ced37f1";

    @Before
    public void before() throws IOException {
        reportStore.findAll().stream().forEach(record -> reportStore.delete(record));
        Report report = new Report(TEMPLATE_ID, TEMPLATE_NAME, new String(Files.readAllBytes(TEMPLATE_PATH), StandardCharsets.UTF_8), Files.readAllBytes(TEMPLATE_COMPILED_PATH), false);
        Report report2 = new Report("31ef91f2-e442-424b-931f-651008bad175", "templ2", "", "".getBytes(), false);
        Report report3 = new Report("7e174baa-5174-48b6-bfde-a38d77b0c11a", "templ3", "", "".getBytes(), false);
        Report report4 = new Report("929f1ec3-22c5-4641-b93e-619404135e09", "templ4", "", "".getBytes(), false);
        Report report5 = new Report("18ded44f-a17e-47f4-b69a-36d8134a5300", "templ5", "", "".getBytes(), false);
        reportStore.save(report);
        reportStore.save(report2);
        reportStore.save(report3);
        reportStore.save(report4);
        reportStore.save(report5);
    }

    @Test
    public void saveValidTemplate() throws Exception {
        String reportId = UUID.randomUUID().toString();
        Report rep = new Report(
                reportId,
                name.getMethodName(),
                IOUtils.toString(new FileInputStream(TEMPLATE_PATH.toFile())),
                null, false);
        mvc(api)
                .perform(put(BASE + "/{reportId}", reportId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(rep)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(reportId)));
    }

    @Test
    public void saveTemplateUnsupportedParameterType() throws Exception {
        String reportId = UUID.randomUUID().toString();
        Report rep = new Report(
                reportId,
                name.getMethodName(),
                IOUtils.toString(new FileInputStream(TEMPLATES_PATH.resolve("unsupported-param.jrxml").toFile())),
                null, false);
        mvc(api)
                .perform(put(BASE + "/{reportId}", reportId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(rep)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void saveTemplateInvalidDefaultValue() throws Exception {
        String reportId = UUID.randomUUID().toString();
        Report rep = new Report(
                reportId,
                name.getMethodName(),
                IOUtils.toString(new FileInputStream(TEMPLATES_PATH.resolve("invalid-value.jrxml").toFile())),
                null, false);
        mvc(api)
                .perform(put(BASE + "/{reportId}", reportId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(rep)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Rollback
    public void saveTemplateDuplicitName() throws Exception {
        String reportId = UUID.randomUUID().toString();
        Report rep = new Report(
                reportId,
                TEMPLATE_NAME,
                IOUtils.toString(new FileInputStream(TEMPLATE_PATH.toFile())),
                null, false);
        mvc(api)
                .perform(put(BASE + "/{reportId}", reportId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(rep)))
                .andExpect(status().isConflict());
    }

    @Test
    public void getReportDetail() throws Exception {
        mvc(api)
                .perform(get(BASE + "/{reportId}", TEMPLATE_ID))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    public void getReportHTML() throws Exception {
        String res = mvc(api)
                .perform(get(BASE + "/{reportId}/HTML", TEMPLATE_ID))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(res.contains("templ5"), is(true));
        assertThat(res.contains("templ3"), is(true));
        assertThat(res.contains("18ded44"), is(true));
        assertThat(res.contains("18ded44f"), is(false));
    }

    @Test
    public void getReportCSVWithCustomParams() throws Exception {
        String res = mvc(api)
                .perform(get(BASE + "/{reportId}/csv", TEMPLATE_ID).param("like", "templ5").param("chars", "4"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(res.contains("templ5"), is(true));
        assertThat(res.contains("templ3"), is(false));
        assertThat(res.contains("18de"), is(true));
        assertThat(res.contains("18ded"), is(false));
    }

    @Test
    public void getReportUnknownParam() throws Exception {
        mvc(api)
                .perform(get(BASE + "/{reportId}/PDF", TEMPLATE_ID).param("blah", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getReportInvalidParamValue() throws Exception {
        mvc(api)
                .perform(get(BASE + "/{reportId}/PDF", TEMPLATE_ID).param("limit", "blah"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getReportUnsupportedFormat() throws Exception {
        mvc(api)
                .perform(get(BASE + "/{reportId}/XLS", TEMPLATE_ID))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getInvalidUUID() throws Exception {
        mvc(api)
                .perform(get(BASE + "/{reportId}/XLS", "blah"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getTemplateNotFound() throws Exception {
        mvc(api)
                .perform(get(BASE + "/{reportId}/XLSX", "8f719ff7-0000-4101-9e87-42391ced37f1"))
                .andExpect(status().isNotFound());
    }
}
