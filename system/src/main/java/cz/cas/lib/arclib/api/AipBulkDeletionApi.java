package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.export.ExportTemplate;
import cz.cas.lib.arclib.dto.AipBulkDeletionCreateDto;
import cz.cas.lib.arclib.dto.AipBulkDeletionDto;
import cz.cas.lib.arclib.exception.AipStateChangeException;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.service.AipBulkDeletionService;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;

@RestController
@Api(value = "aip bulk deletion", description = "Api for bulk deletions")
@RequestMapping("/api/bulk_deletion")
@Slf4j
public class AipBulkDeletionApi {

    private AipBulkDeletionService service;

    @ApiOperation(value = "Starts asynchronous bulk delete process. [Perm.AIP_BULK_DELETIONS_WRITE]",
            notes = "Returns count of AIPs that will be deleted (deletion is asynchronous)",
            response = ExportTemplate.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = Long.class)})
    @PreAuthorize("hasAuthority('" + Permissions.AIP_BULK_DELETIONS_WRITE + "')")
    @RequestMapping(method = RequestMethod.POST)
    public void bulkDelete(@ApiParam(value = "Single instance", required = true) @RequestBody AipBulkDeletionCreateDto request) throws IOException, AipStateChangeException {
        service.bulkDelete(request);
    }

    @ApiOperation(value = "Gets DTOs of all instances [Perm.AIP_BULK_DELETIONS_READ]",
            notes = "if the calling user is not [Perm.SUPER_ADMIN_PRIVILEGE] only AipBulkDeletions assigned to the user's producer are returned. ",
            response = AipBulkDeletionDto.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful response", response = AipBulkDeletionDto.class)})
    @PreAuthorize("hasAuthority('" + Permissions.AIP_BULK_DELETIONS_READ + "')")
    @RequestMapping(path = "/list_dtos", method = RequestMethod.GET)
    public Collection<AipBulkDeletionDto> listDtos() {
        return service.listAipBulkDeletionDtos();
    }

    @Inject
    public void setService(AipBulkDeletionService service) {
        this.service = service;
    }
}
