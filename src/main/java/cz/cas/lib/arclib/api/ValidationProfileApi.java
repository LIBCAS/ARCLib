package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.profiles.ValidationProfile;
import cz.cas.lib.arclib.security.authorization.Roles;
import cz.cas.lib.arclib.store.ValidationProfileStore;
import cz.cas.lib.arclib.utils.XmlUtils;
import cz.cas.lib.core.exception.BadArgument;
import cz.cas.lib.core.exception.GeneralException;
import cz.cas.lib.core.exception.MissingObject;
import cz.cas.lib.core.store.Transactional;
import io.swagger.annotations.*;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;
import org.xml.sax.SAXException;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import static cz.cas.lib.core.util.Utils.eq;
import static cz.cas.lib.core.util.Utils.notNull;

@RestController
@Api(value = "validation-profile", description = "Api for interaction with validation profiles")
@RequestMapping("/api/validation_profile")
public class ValidationProfileApi {
    @Getter
    private ValidationProfileStore store;
    private Resource validationProfileSchema;

    @ApiOperation(value = "Saves an instance. Roles.SUPER_ADMIN, Roles.ADMIN",
            notes = "Returns single instance (possibly with computed attributes)",
            response = ValidationProfile.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = ValidationProfile.class),
            @ApiResponse(code = 400, message = "Specified id does not correspond to the id of the instance")})
    @RolesAllowed({Roles.SUPER_ADMIN, Roles.ADMIN})
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    @Transactional
    public ValidationProfile save(@ApiParam(value = "Id of the instance", required = true)
                                  @PathVariable("id") String id,
                                  @ApiParam(value = "Single instance", required = true)
                                  @RequestBody ValidationProfile request) throws IOException {
        eq(id, request.getId(), () -> new BadArgument("id"));

        String validationProfileXml = request.getXml();
        try {
            XmlUtils.validateWithXMLSchema(new ByteArrayInputStream(validationProfileXml.getBytes()),
                    new InputStream[]{validationProfileSchema.getInputStream()});
        } catch (SAXException e) {
            throw new GeneralException("Provided validation profile is invalid against its respective XSD schema.", e);
        }

        return store.save(request);
    }

    @ApiOperation(value = "Deletes an instance. Roles.SUPER_ADMIN")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RolesAllowed({Roles.SUPER_ADMIN})
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @Transactional
    public void delete(@ApiParam(value = "Id of the instance", required = true)
                       @PathVariable("id") String id) {
        ValidationProfile entity = store.find(id);
        notNull(entity, () -> new MissingObject(store.getType(), id));

        store.delete(entity);
    }

    @ApiOperation(value = "Gets one instance specified by id", response = ValidationProfile.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = ValidationProfile.class),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @Transactional
    public ValidationProfile get(@ApiParam(value = "Id of the instance", required = true)
                                 @PathVariable("id") String id) {
        ValidationProfile entity = store.find(id);
        notNull(entity, () -> new MissingObject(store.getType(), id));

        return entity;
    }

    @ApiOperation(value = "Gets all instances", response = Collection.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Collection.class)})
    @RequestMapping(method = RequestMethod.GET)
    @Transactional
    public Collection<ValidationProfile> list() {
        return store.findAll();
    }

    @Inject
    public void setStore(ValidationProfileStore store) {
        this.store = store;
    }

    @Inject
    public void setValidationProfileSchema(@Value("${arclib.validationProfileSchema}") Resource validationProfileSchema) {
        this.validationProfileSchema = validationProfileSchema;
    }
}
