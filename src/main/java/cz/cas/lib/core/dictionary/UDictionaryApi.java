package cz.cas.lib.core.dictionary;

import cz.cas.lib.core.exception.MissingObject;
import cz.cas.lib.core.rest.DictionaryApi;
import cz.cas.lib.core.store.Transactional;
import io.swagger.annotations.*;
import lombok.Getter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;

import static cz.cas.lib.core.util.Utils.notNull;

/**
 * Api for universal dynamic dictionaries.
 */
@RestController
@Api(value = "dictionary", description = "Api for universal dynamic dictionaries (main attribute: name).")
@RequestMapping("/api/dictionaries")
public class UDictionaryApi extends UDictionaryValueApi implements DictionaryApi<Dictionary> {

    @Getter
    private UDictionaryService adapter;

    @Inject
    public void setAdapter(UDictionaryService service) {
        this.adapter = service;
    }

    /**
     * Gets one instance specified by code.
     *
     * @param code Code of the instance
     * @return Single instance
     * @throws MissingObject if instance does not exists
     */
    @ApiOperation(value = "Gets one instance specified by code", response = Dictionary.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Dictionary.class),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RequestMapping(value = "/coded", method = RequestMethod.GET)
    @Transactional
    public Dictionary getCoded(@ApiParam(value = "Code of the instance", required = true) @RequestParam("code") String code) {
        Dictionary entity = getAdapter().findByCode(code);
        notNull(entity, () -> new MissingObject(Dictionary.class, code));

        return entity;
    }
}
