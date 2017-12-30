package cz.inqool.uas.sequence;

import cz.inqool.uas.rest.DictionaryApi;
import cz.inqool.uas.security.Permissions;
import io.swagger.annotations.*;
import lombok.Getter;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;

/**
 * Api for generating formatted numbers from sequences.
 */
@RolesAllowed(Permissions.SEQUENCE)
@RestController
@Api(value = "sequence", description = "Api for generating formatted numbers from sequences (main attribute: name).")
@RequestMapping("/api/sequences")
public class SequencesApi implements DictionaryApi<Sequence> {

    private Generator generator;

    @Getter
    private SequenceStore adapter;

    /**
     * Generates next formatted number from the sequence.
     * @param id Id of the {@link Sequence}
     * @return Formatted number
     * @throws cz.inqool.uas.exception.MissingObject If {@link Sequence} is not found
     */
    @ApiOperation(value = "Generates next formatted number from the sequence.", notes = "Returns formatted number.",
            response = String.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = String.class),
            @ApiResponse(code = 404, message = "Sequence not found")})
    @RequestMapping(value = "/{id}/generate", method = RequestMethod.POST)
    public String generate(@ApiParam(value = "Id of the sequence", required = true)
                               @PathVariable("id") String id) {
        return generator.generate(id);
    }

    @Inject
    public void setGenerator(Generator generator) {
        this.generator = generator;
    }

    @Inject
    public void setAdapter(SequenceStore store) {
        this.adapter = store;
    }
}
