package cz.cas.lib.arclib.api;

import cz.inqool.uas.store.Transactional;
import lombok.Getter;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;

import static cz.inqool.uas.util.Utils.asMap;

@RestController
@RequestMapping("/api/validation_api")
public class ValidationApi {
    @Getter
    private RuntimeService runtimeService;

    @RequestMapping(value = "/validate", method = RequestMethod.PUT)
    @Transactional
    public void validateSip(@RequestParam("pathToSip") String pathToSip, @RequestParam("validationProfileId") String
    validationProfileId) {
        runtimeService.startProcessInstanceByKey("ValidateSip", asMap("pathToSip", pathToSip,
                "validationProfileId", validationProfileId))
                .getProcessInstanceId();
    }

    @Inject
    public void setRuntimeService(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }
}
