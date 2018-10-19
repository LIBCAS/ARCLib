package cz.cas.lib.core.dictionary;

import cz.cas.lib.core.domain.DomainObject;
import cz.cas.lib.core.exception.BadArgument;
import cz.cas.lib.core.exception.MissingObject;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import cz.cas.lib.core.store.Transactional;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.List;

import static cz.cas.lib.core.util.Utils.eq;
import static cz.cas.lib.core.util.Utils.notNull;

/**
 * Provides API methods for manipulation with dictionary values.
 */
public class UDictionaryValueApi {
    private UDictionaryValueService valueService;

    /**
     * Saves DictionaryValue to existing Dictionary.
     *
     * <p>
     * Specified id should correspond to {@link DomainObject#id} otherwise exception is thrown.
     * </p>
     *
     * @param dictionaryId Id of the dictionary instance
     * @param id           Id of the dictionary value instance
     * @param request      Single instance
     * @return Single instance (possibly with computed attributes)
     * @throws BadArgument if specified id does not correspond to {@link DomainObject#id}
     */
    @ApiOperation(value = "Saves a DictionaryValue", response = DictionaryValue.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 400, message = "Specified id does not correspond to the id of the instance")})
    @RequestMapping(value = "/{dictionaryId}/values/{id}", method = RequestMethod.PUT)
    @Transactional
    public DictionaryValue saveValue(
            @ApiParam(value = "Id of the existing dictionary", required = true) @PathVariable("dictionaryId") String dictionaryId,
            @ApiParam(value = "Id of the instance", required = true) @PathVariable("id") String id,
            @ApiParam(value = "Single instance", required = true) @RequestBody DictionaryValue request
    ) {
        eq(id, request.getId(), () -> new BadArgument("id"));

        return valueService.save(dictionaryId, request);
    }

    /**
     * Gets one DictionaryValue specified by id.
     *
     * @param dictionaryId Id of the dictionary instance
     * @param id           Id of the instance
     * @return Single instance
     * @throws MissingObject if instance does not exists
     */
    @ApiOperation(value = "Gets a DictionaryValue specified by id", response = DictionaryValue.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RequestMapping(value = "/{dictionaryId}/values/{id}", method = RequestMethod.GET)
    @Transactional
    public DictionaryValue getValue(
            @ApiParam(value = "Id of the existing dictionary", required = true) @PathVariable("dictionaryId") String dictionaryId,
            @ApiParam(value = "Id of the instance", required = true) @PathVariable("id") String id
    ) {
        DictionaryValue entity = valueService.find(dictionaryId, id);
        notNull(entity, () -> new MissingObject(DictionaryValue.class, id));

        return entity;
    }

    /**
     * Gets all DictionaryValues of existing Dictionary that respect the selected {@link Params}.
     *
     * <p>
     * Though {@link Params} one could specify filtering, sorting and paging. For further explanation
     * see {@link Params}.
     * </p>
     * <p>
     * Returning also the total number of instances passed through the filtering phase.
     * </p>
     *
     * @param dictionaryId Id of the dictionary instance
     * @param params       Parameters to comply with
     * @return Sorted {@link List} of instances with total number
     */
    @ApiOperation(value = "Gets all DictionaryValues of existing Dictionary that respect the selected parameters", response = Result.class)
    @ApiResponses(value = @ApiResponse(code = 200, message = "Successful response"))
    @RequestMapping(value = "/{dictionaryId}/values", method = RequestMethod.GET)
    @Transactional
    public Result<DictionaryValue> listValues(
            @ApiParam(value = "Id of the existing dictionary", required = true) @PathVariable("dictionaryId") String dictionaryId,
            @ApiParam(value = "Parameters to comply with", required = true) @ModelAttribute Params params
    ) {
        return valueService.findAll(dictionaryId, params);
    }

    /**
     * Gets all DictionaryValues of existing Dictionary that respect the selected {@link Params}.
     *
     * <p>
     * Though {@link Params} one could specify filtering, sorting and paging. For further explanation
     * see {@link Params}.
     * </p>
     * <p>
     * Returning also the total number of instances passed through the filtering phase.
     * </p>
     *
     * @param dictionaryId Id of the dictionary instance
     * @param params       Parameters to comply with
     * @return Sorted {@link List} of instances with total number
     */
    @ApiOperation(value = "Gets all DictionaryValues of existing Dictionary that respect the selected parameters", response = Result.class)
    @ApiResponses(value = @ApiResponse(code = 200, message = "Successful response"))
    @RequestMapping(value = "/{dictionaryId}/values/parametrized", method = RequestMethod.POST)
    @Transactional
    public Result<DictionaryValue> listValuesPost(
            @ApiParam(value = "Id of the existing dictionary", required = true) @PathVariable("dictionaryId") String dictionaryId,
            @ApiParam(value = "Parameters to comply with", required = true) @RequestBody Params params
    ) {
        return listValues(dictionaryId, params);
    }

    /**
     * Deletes a DictionaryValue.
     *
     * @param dictionaryId Id of the dictionary instance
     * @param id           Id of the instance
     * @throws MissingObject if specified instance is not found
     */
    @ApiOperation(value = "Deletes a DictionaryValue")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RequestMapping(value = "/{dictionaryId}/values/{id}", method = RequestMethod.DELETE)
    @Transactional
    public void delete(
            @ApiParam(value = "Id of the existing dictionary", required = true) @PathVariable("dictionaryId") String dictionaryId,
            @ApiParam(value = "Id of the instance", required = true) @PathVariable("id") String id
    ) {
        DictionaryValue entity = valueService.find(dictionaryId, id);
        notNull(entity, () -> new MissingObject(DictionaryValue.class, id));

        valueService.delete(dictionaryId, entity);
    }

    /**
     * Gets all DictionaryValues that respect the selected prefix and belongs to specified Dictionary.
     *
     * <p>
     * Used for dictionary selects.
     * </p>
     *
     * @param dictionaryId Id of the dictionary instance
     * @param prefix       Prefix to comply with
     * @return Sorted {@link List} of instances
     */
    @ApiOperation(value = "Gets all instances that respect the selected prefix and belongs to specified Dictionary", response = Result.class)
    @ApiResponses(value = @ApiResponse(code = 200, message = "Successful response"))
    @RequestMapping(value = "/{dictionaryId}/values/prefixed", method = RequestMethod.GET)
    @Transactional
    public List<DictionaryValue> prefixedValue(
            @ApiParam(value = "Id of the existing dictionary", required = true) @PathVariable("dictionaryId") String dictionaryId,
            @ApiParam(value = "Parameters to comply with", required = true) @RequestParam("prefix") String prefix
    ) {
        return valueService.findAllPrefixed(dictionaryId, prefix);
    }

    /**
     * Gets all DictionaryValues that contains the specified string and belongs to specified Dictionary.
     *
     * <p>
     * Used for dictionary selects.
     * </p>
     *
     * @param dictionaryId Id of the dictionary instance
     * @param q            String to contain
     * @return Sorted {@link List} of instances
     */
    @ApiOperation(value = "Gets all DictionaryValues that contains the specified string and belongs to specified Dictionary", response = Result.class)
    @ApiResponses(value = @ApiResponse(code = 200, message = "Successful response"))
    @RequestMapping(value = "/{dictionaryId}/values/containing", method = RequestMethod.GET)
    @Transactional
    public List<DictionaryValue> containingValue(
            @ApiParam(value = "Id of the existing dictionary", required = true) @PathVariable("dictionaryId") String dictionaryId,
            @ApiParam(value = "Parameters to comply with", required = true) @RequestParam("q") String q
    ) {
        return valueService.findAllContaining(dictionaryId, q);
    }

    /**
     * Gets all DictionaryValues values
     *
     * <p>
     * Used for dictionary selects.
     * </p>
     *
     * @param dictionaryId Id of the dictionary instance
     * @return Sorted {@link List} of instances
     */
    @ApiOperation(value = "Gets all DictionaryValues that belongs to specified Dictionary", response = Result.class)
    @ApiResponses(value = @ApiResponse(code = 200, message = "Successful response"))
    @RequestMapping(value = "/{dictionaryId}/values/all", method = RequestMethod.GET)
    @Transactional
    public List<DictionaryValue> allValue(
            @ApiParam(value = "Id of the existing dictionary", required = true) @PathVariable("dictionaryId") String dictionaryId
    ) {
        return valueService.findAll(dictionaryId);
    }

    /**
     * Gets one instance specified by code.
     *
     * @param dictionaryId Id of the dictionary instance
     * @param code         Code of the instance
     * @return Single instance
     * @throws MissingObject if instance does not exists
     */
    @ApiOperation(value = "Gets one instance specified by code", response = Dictionary.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Dictionary.class),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RequestMapping(value = "/{dictionaryId}/values/coded", method = RequestMethod.GET)
    @Transactional
    public DictionaryValue getCoded(
            @ApiParam(value = "Id of the existing dictionary", required = true) @PathVariable("dictionaryId") String dictionaryId,
            @ApiParam(value = "Code of the instance", required = true) @RequestParam("code") String code
    ) {
        DictionaryValue entity = valueService.findByCode(dictionaryId, code);
        notNull(entity, () -> new MissingObject(DictionaryValue.class, code));

        return entity;
    }

    @Inject
    public void setValueService(UDictionaryValueService valueService) {
        this.valueService = valueService;
    }
}
