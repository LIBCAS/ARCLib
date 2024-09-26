package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.dto.AipBulkDeletionCreateDto;
import cz.cas.lib.arclib.dto.AipBulkDeletionDto;
import cz.cas.lib.arclib.exception.AipStateChangeException;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.service.AipBulkDeletionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;

@RestController
@Tag(name = "aip bulk deletion", description = "Api for bulk deletions")
@RequestMapping("/api/bulk_deletion")
@Slf4j
public class AipBulkDeletionApi {

    private AipBulkDeletionService service;

    @Operation(summary = "Starts asynchronous bulk delete process. [Perm.AIP_BULK_DELETIONS_WRITE]",
            description = "Returns count of AIPs that will be deleted (deletion is asynchronous)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response")})
    @PreAuthorize("hasAuthority('" + Permissions.AIP_BULK_DELETIONS_WRITE + "')")
    @RequestMapping(method = RequestMethod.POST)
    public void bulkDelete(@Parameter(description = "Single instance", required = true) @RequestBody AipBulkDeletionCreateDto request) throws IOException, AipStateChangeException {
        service.bulkDelete(request);
    }

    @Operation(summary = "Gets DTOs of all instances [Perm.AIP_BULK_DELETIONS_READ]",
            description = "if the calling user is not [Perm.SUPER_ADMIN_PRIVILEGE] only AipBulkDeletions assigned to the user's producer are returned. ")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successful response", content = @Content(array = @ArraySchema(schema = @Schema(implementation = AipBulkDeletionDto.class))))})
    @PreAuthorize("hasAuthority('" + Permissions.AIP_BULK_DELETIONS_READ + "')")
    @RequestMapping(path = "/list_dtos", method = RequestMethod.GET)
    public Collection<AipBulkDeletionDto> listDtos() {
        return service.listAipBulkDeletionDtos();
    }

    @Autowired
    public void setService(AipBulkDeletionService service) {
        this.service = service;
    }
}
