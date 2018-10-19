package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.security.authorization.Roles;
import cz.cas.lib.arclib.service.BatchService;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageServiceDebug;
import cz.cas.lib.core.exception.MissingObject;
import cz.cas.lib.core.store.Transactional;
import io.swagger.annotations.*;
import org.apache.commons.io.IOUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

import static cz.cas.lib.core.util.Utils.checkUUID;
import static cz.cas.lib.core.util.Utils.notNull;

@RestController
@Api(value = "debug", description = "Api for various debugging task, e.g. for retrieval of AIPs which were result of Ingest in debug mode")
@RequestMapping("/api/debug")
@Transactional
public class DebugApi {

    private ArchivalStorageServiceDebug archivalStorage;
    private BatchService batchService;

    @ApiOperation(value = "Gets AIP data in a .zip")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Instance does not exist")
    })
    @RequestMapping(value = "/aip/export/{aipId}", method = RequestMethod.GET)
    public void exportAip(
            @ApiParam(value = "AIP ID", required = true)
            @PathVariable("aipId") String aipId,
            @ApiParam(value = "True to return all XMLs, otherwise only the latest is returned")
            @RequestParam(value = "all", defaultValue = "false") boolean all,
            HttpServletResponse response) throws IOException {
        checkUUID(aipId);
        response.setContentType("application/zip");
        response.setStatus(200);
        response.addHeader("Content-Disposition", "attachment; filename=" + aipId + ".zip");

        InputStream aip = archivalStorage.exportSingleAip(aipId, all);
        notNull(aip, () -> new MissingObject(InputStream.class, aipId));

        IOUtils.copyLarge(aip, response.getOutputStream());
    }

    @ApiOperation(value = "Returns specified AIP XML")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Instance does not exist")
    })
    @RequestMapping(value = "/aip/export/{aipId}/xml", method = RequestMethod.GET)
    public void exportXml(
            @ApiParam(value = "AIP ID", required = true)
            @PathVariable("aipId") String aipId,
            @ApiParam(value = "Version number of XML, if not set the latest version is returned")
            @RequestParam(value = "v", defaultValue = "") Integer version,
            HttpServletResponse response) throws IOException {
        String versionSuffix = version != null ? String.valueOf(version) : "latest";
        checkUUID(aipId);
        response.setContentType("application/xml");
        response.setStatus(200);
        response.addHeader("Content-Disposition", "attachment; filename=" + aipId + "_xml_" + versionSuffix + ".xml");
        InputStream xml = archivalStorage.exportSingleXml(aipId, version);

        String errorMessage = version != null ? "ArclibXml of version " + version + " of aip with ID" + aipId :
                "Latest version of ArclibXml of aip with ID" + aipId;
        notNull(xml, () -> new MissingObject(errorMessage, aipId));

        IOUtils.copyLarge(xml, response.getOutputStream());
    }

    @ApiOperation(value = "Deletes batch and all its respective ingest workflows. Roles.ADMIN, Roles.SUPER_ADMIN, Roles.ARCHIVIST",
            notes = "Applicable only for batches processed using a producer profile in the debugging mode.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @RolesAllowed({Roles.ADMIN, Roles.SUPER_ADMIN, Roles.ARCHIVIST})
    @RequestMapping(value = "/{batchId}/forget", method = RequestMethod.PUT)
    public void forget(@ApiParam(value = "Id of the batch to forget", required = true)
                       @PathVariable("batchId") String batchId) {
        batchService.forget(batchId);
    }

    @Inject
    public void setArchivalStorage(ArchivalStorageServiceDebug archivalStorage) {
        this.archivalStorage = archivalStorage;
    }

    @Inject
    public void setBatchService(BatchService batchService) {
        this.batchService = batchService;
    }
}
