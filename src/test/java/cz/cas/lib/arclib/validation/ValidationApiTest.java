package cz.cas.lib.arclib.validation;

import cz.cas.lib.arclib.api.ValidationApi;
import cz.cas.lib.arclib.domain.Sip;
import cz.cas.lib.arclib.domain.ValidationProfile;
import cz.cas.lib.arclib.store.SipStore;
import cz.cas.lib.arclib.store.ValidationProfileStore;
import helper.ApiTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ValidationApiTest implements ApiTest {
    @Inject
    private ValidationApi api;

    @Inject
    private ValidationProfileStore validationProfileStore;

    private static final String SIP_ID = "KPW01169310";
    private static final String SIP_PATH = "SIP_packages/" + SIP_ID;

    @Test
    public void validationApiTest() throws Exception {
           ValidationProfile validationProfile = new ValidationProfile();
        InputStream inputStream = getClass().getResourceAsStream("/validation/validationProfileMixedChecks.xml");
        String xml = readFromInputStream(inputStream);

        validationProfile.setXml(xml);
        validationProfileStore.save(validationProfile);

        Thread.sleep(2000);

        mvc(api).perform(put("/api/validation_api/validate/")
                .param("pathToSip", SIP_PATH)
                .param("validationProfileId", validationProfile.getId()))
                .andExpect(status().is2xxSuccessful());
    }

    private String readFromInputStream(InputStream inputStream) throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }
}
