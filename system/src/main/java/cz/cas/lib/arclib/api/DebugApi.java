package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.security.authorization.data.Permissions;
import cz.cas.lib.arclib.service.AuthorialPackageService;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageServiceDebug;
import cz.cas.lib.core.store.Transactional;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
@Slf4j
public class DebugApi {

    private ArchivalStorageServiceDebug archivalStorage;
    private AuthorialPackageService authorialPackageService;

    @ApiOperation(value = "Gets AIP data in a .zip [Perm.EXPORT_FILES]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Instance does not exist")
    })
    @RequestMapping(value = "/aip/export/{aipId}", method = RequestMethod.GET)
    @PreAuthorize("hasAuthority('" + Permissions.EXPORT_FILES + "')")
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

    @ApiOperation(value = "Returns specified AIP XML [Perm.EXPORT_FILES]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Instance does not exist")
    })
    @RequestMapping(value = "/aip/export/{aipId}/xml", method = RequestMethod.GET)
    @PreAuthorize("hasAuthority('" + Permissions.EXPORT_FILES + "')")
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

    @ApiOperation(value = "Deletes authorial package." +
            "Also deletes all child entities (SIPs, IWs) and for each IW, if it was the only one in the batch deletes also the batch. [Perm.BATCH_PROCESSING_WRITE]",
            notes = "Applicable only for authorial packages processed using a producer profile in the debugging mode.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Instance does not exist")})
    @PreAuthorize("hasAuthority('" + Permissions.BATCH_PROCESSING_WRITE + "')")
    @RequestMapping(value = "/authorial_package/{authorialPackageId}/forget", method = RequestMethod.PUT)
    public void forgetAuthorialPackage(@ApiParam(value = "Id of the authorial package to forget", required = true)
                                       @PathVariable("authorialPackageId") String authorialPackageId) {
        authorialPackageService.forgetAuthorialPackage(authorialPackageId);
    }

    @Inject
    public void setArchivalStorage(ArchivalStorageServiceDebug archivalStorage) {
        this.archivalStorage = archivalStorage;
    }

    @Inject
    public void setAuthorialPackageService(AuthorialPackageService authorialPackageService) {
        this.authorialPackageService = authorialPackageService;
    }
}
