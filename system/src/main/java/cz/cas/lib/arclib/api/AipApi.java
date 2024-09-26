package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.Hash;
import cz.cas.lib.arclib.dto.AipDetailDto;
import cz.cas.lib.arclib.exception.AipStateChangeException;
import cz.cas.lib.arclib.exception.AuthorialPackageNotLockedException;
import cz.cas.lib.arclib.exception.ForbiddenException;
import cz.cas.lib.arclib.index.IndexedArclibXmlStore;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedAipState;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlDocument;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.service.AipQueryService;
import cz.cas.lib.arclib.service.AipService;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageException;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStoragePipe;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageService;
import cz.cas.lib.arclib.utils.ArclibUtils;
import cz.cas.lib.core.index.dto.Filter;
import cz.cas.lib.core.index.dto.FilterOperation;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.dom4j.DocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

import static cz.cas.lib.arclib.utils.ArclibUtils.hasRole;
import static cz.cas.lib.core.util.Utils.addPrefilter;
import static cz.cas.lib.core.util.Utils.checkUUID;

@RestController
@Tag(name = "aip", description = "Api for searching within ARCLib XML index, retrieving from Archival Storage and editing of ArclibXml")
@RequestMapping("/api/aip")
@Slf4j
public class AipApi extends ArchivalStoragePipe {

    @Getter
    private IndexedArclibXmlStore indexArclibXmlStore;
    private AipService aipService;
    private int keepAliveUpdateTimeout;
    private ArchivalStorageService archivalStorageService;
    private AipQueryService aipQueryService;

    @Operation(summary = "Gets partially filled ARCLib XML index records. The result of query as well as the query itself is saved if the queryName param is filled. [Perm.AIP_RECORDS_READ]",
            description = "If the calling user is not Roles.SUPER_ADMIN, only the respective records of the users producer are returned." +
                    "Records with state REMOVED and DELETED are returned only to the user with role Roles.SUPER_ADMIN or Roles.ADMIN.\n" +
                    "Sort fields: producer_name, user_name, aip_state, sip_version_number,  xml_version_number, created, updated, id, authorial_id\n" +
                    " ..producerId filter will have effect only for super admin account",
            responses = {@ApiResponse(content = @Content(schema = @Schema(implementation = Result.class)))})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response")})
    @PreAuthorize("hasAuthority('" + Permissions.AIP_RECORDS_READ + "')")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public Result<IndexedArclibXmlDocument> list(
            @Parameter(description = "Parameters to comply with", required = true)
            @ModelAttribute Params params,
            @Parameter(description = "Query name")
            @RequestParam(value = "queryName", defaultValue = "", required = false) String queryName) {
        if (!hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE)) {
            addPrefilter(params, new Filter(IndexedArclibXmlDocument.PRODUCER_ID, FilterOperation.EQ, userDetails.getProducerId(), null));
        }
        if (!hasRole(userDetails, Permissions.LOGICAL_FILE_RENEW)) {
            addPrefilter(params, new Filter(IndexedArclibXmlDocument.AIP_STATE, FilterOperation.NEQ, IndexedAipState.REMOVED.toString(), null));
        }
        if (!(hasRole(userDetails, Permissions.ADMIN_PRIVILEGE) || hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE))) {
            addPrefilter(params, new Filter(IndexedArclibXmlDocument.AIP_STATE, FilterOperation.NEQ, IndexedAipState.DELETED.toString(), null));
        }
        if (!queryName.isEmpty() && !hasRole(userDetails, Permissions.AIP_QUERY_RECORDS_WRITE))
            throw new ForbiddenException("To save the search query, the " + Permissions.AIP_QUERY_RECORDS_WRITE + " is required");
        Result<IndexedArclibXmlDocument> uiPageResult = indexArclibXmlStore.findAll(params);
        if (!queryName.trim().isEmpty()) {
            aipQueryService.saveAipQuery(userDetails.getId(), params, queryName);
        }
        return uiPageResult;
    }

    @Operation(summary = "Gets all main fields of ARCLib XML index record together with corresponding IW entity containing SIPs folder structure. [Perm.AIP_RECORDS_READ]",
            description = "If the calling user is not Roles.SUPER_ADMIN, the producer of the record must match the producer of the user.",
            responses = {@ApiResponse(content = @Content(schema = @Schema(implementation = AipDetailDto.class)))})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "404", description = "Ingest workflow not found.")})
    @PreAuthorize("hasAuthority('" + Permissions.AIP_RECORDS_READ + "')")
    @RequestMapping(value = "/{xmlId}", method = RequestMethod.GET)
    public AipDetailDto getMetadata(
            @Parameter(description = "Ingest workflow external id", required = true)
            @PathVariable(value = "xmlId") String xmlId) throws IOException {
        return aipService.getMetadata(xmlId, userDetails);
    }

    @Operation(summary = "Gets AIP data in a .zip [Perm.EXPORT_FILES]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "400", description = "aipId is not a valid UUID.")})
    @PreAuthorize("hasAuthority('" + Permissions.EXPORT_FILES + "')")
    @RequestMapping(value = "/export/{aipId}", method = RequestMethod.GET)
    public void exportAip(
            @Parameter(description = "AIP ID", required = true) @PathVariable("aipId") String aipId,
            @Parameter(description = "True to return all XMLs, otherwise only the latest is returned")
            @RequestParam(value = "all", defaultValue = "false") boolean all,
            HttpServletResponse response) throws ArchivalStorageException, IOException {
        checkUUID(aipId);
        InputStream storageResponse = archivalStorageService.exportSingleAip(aipId, all, null);
        response.setContentType("application/zip");
        response.addHeader("Content-Disposition", "attachment; filename=" + ArclibUtils.getAipExportName(aipId));
        IOUtils.copyLarge(storageResponse, response.getOutputStream());
    }

    @Operation(summary = "Returns specified AIP XML [Perm.EXPORT_FILES]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "400", description = "aipId is not a valid UUID.")})
    @PreAuthorize("hasAuthority('" + Permissions.EXPORT_FILES + "')")
    @RequestMapping(value = "/export/{aipId}/xml", method = RequestMethod.GET)
    public void exportXml(
            @Parameter(description = "AIP ID", required = true)
            @PathVariable("aipId") String aipId,
            @Parameter(description = "Version number of XML, if not set the latest version is returned")
            @RequestParam(value = "v", defaultValue = "") Integer version,
            HttpServletResponse response) throws ArchivalStorageException, IOException {
        checkUUID(aipId);
        InputStream storageResponse = archivalStorageService.exportSingleXml(aipId, version);
        response.setContentType("application/xml");
        response.addHeader("Content-Disposition", "attachment; filename=" + ArclibUtils.getXmlExportName(aipId, version));
        IOUtils.copyLarge(storageResponse, response.getOutputStream());
    }

    @Operation(description = "If the AIP is in PROCESSING state or the storage is not reachable, the storage checksums are not filled and the consistent flag is set to false",
            summary = "Retrieves information about AIP containing state, whether is consistent etc from the given storage. [Perm.AIP_RECORDS_READ]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "AIP successfully deleted"),
            @ApiResponse(responseCode = "400", description = "bad request, e.g. the specified id is not a valid UUID"),
            @ApiResponse(responseCode = "500", description = "internal server error")
    })
    @PreAuthorize("hasAuthority('" + Permissions.AIP_RECORDS_READ + "')")
    @RequestMapping(value = "/{aipId}/info", method = RequestMethod.GET)
    public void getAipInfo(
            @Parameter(description = "AIP id", required = true) @PathVariable("aipId") String aipId,
            @Parameter(description = "id of the logical storage", required = true) @RequestParam(value = "storageId") String storageId,
            HttpServletResponse response, HttpServletRequest request) {
        checkUUID(aipId);
        passToArchivalStorage(response, request, "/storage/" + aipId + "/info?storageId=" + storageId, HttpMethod.GET, "retrieve info about AIP: " + aipId + "at storage: " + storageId, AccessTokenType.READ);
    }

    @Operation(summary = "Logically removes AIP at archival storage. [Perm.LOGICAL_FILE_REMOVE]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "AIP successfully removed"),
            @ApiResponse(responseCode = "400", description = "The specified aip id is not a valid UUID"),
    })
    @PreAuthorize("hasAuthority('" + Permissions.LOGICAL_FILE_REMOVE + "')")
    @RequestMapping(value = "/{aipId}/remove", method = RequestMethod.PUT)
    public void remove(@Parameter(description = "AIP id", required = true) @PathVariable("aipId") String aipId, HttpServletResponse response) throws ArchivalStorageException, AipStateChangeException, IOException {
        checkUUID(aipId);
        aipService.changeAipState(aipId, IndexedAipState.REMOVED, true);
    }

    @Operation(summary = "Renews logically removed AIP at archival storage. [Perm.LOGICAL_FILE_RENEW]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "AIP successfully renewed"),
            @ApiResponse(responseCode = "400", description = "The specified aip id is not a valid UUID"),
    })
    @PreAuthorize("hasAuthority('" + Permissions.LOGICAL_FILE_RENEW + "')")
    @RequestMapping(value = "/{aipId}/renew", method = RequestMethod.PUT)
    public void renew(@Parameter(description = "AIP id", required = true) @PathVariable("aipId") String aipId) throws ArchivalStorageException, AipStateChangeException, IOException {
        checkUUID(aipId);
        aipService.changeAipState(aipId, IndexedAipState.ARCHIVED, true);
    }

    @Operation(summary = "Registers update of Arclib Xml of authorial package. [Perm.UPDATE_XML]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Xml update successfully registered"),
            @ApiResponse(responseCode = "404", description = "Authorial package with the specified id not found"),
            @ApiResponse(responseCode = "423", description = "Another update process of the XML is already in progress")
    })
    @PreAuthorize("hasAuthority('" + Permissions.UPDATE_XML + "')")
    @RequestMapping(value = "/{authorialPackageId}/register_update", method = RequestMethod.PUT)
    public void registerXmlUpdate(
            @Parameter(description = "Authorial package id", required = true)
            @PathVariable("authorialPackageId") String authorialPackageId) throws IOException {
        aipService.registerXmlUpdate(authorialPackageId);
    }

    @Operation(summary = "Cancels update of Arclib Xml of authorial package. [Perm.UPDATE_XML]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Xml update successfully canceled"),
            @ApiResponse(responseCode = "404", description = "Authorial package with the specified id not found"),
            @ApiResponse(responseCode = "500", description = "Specified XML is not being updated")
    })
    @PreAuthorize("hasAuthority('" + Permissions.UPDATE_XML + "')")
    @RequestMapping(value = "/{authorialPackageId}/cancel_update", method = RequestMethod.PUT)
    public void cancelXmlUpdate(
            @Parameter(description = "Authorial package id", required = true)
            @PathVariable("authorialPackageId") String authorialPackageId) {
        log.debug("Canceling XML update for authorial package " + authorialPackageId + ".");
        aipService.deactivateLock(authorialPackageId, true);
    }

    @Operation(summary = "Updates Arclib XML of AIP. Register update must be called prior to calling this method. [Perm.UPDATE_XML]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "AIP XML successfully stored"),
            @ApiResponse(responseCode = "400", description = "the specified aip id is not a valid UUID"),
    })
    @PreAuthorize("hasAuthority('" + Permissions.UPDATE_XML + "')")
    @RequestMapping(value = "/{aipId}/update", method = RequestMethod.POST)
    public void finishXmlUpdate(
            @Parameter(description = "XML content", required = true)
            @RequestBody String xml,
            @Parameter(description = "AIP id", required = true)
            @PathVariable("aipId") String aipId,
            @Parameter(description = "XML id of the latest version XML of the AIP", required = true)
            @RequestParam("xmlId") String xmlId,
            @Parameter(description = "Hash of the XML", required = true)
            @ModelAttribute("xmlHash") Hash hash,
            @Parameter(description = "Version of the XML", required = true)
            @RequestParam("version") int version,
            @Parameter(description = "Reason for update", required = true)
            @RequestParam("reason") String reason) throws DocumentException, SAXException, ParserConfigurationException, IOException, AuthorialPackageNotLockedException, ArchivalStorageException {
        checkUUID(aipId);
        aipService.finishXmlUpdate(aipId, xmlId, xml, hash, version, reason);
    }

    @Operation(summary = "Returns keep alive timeout in seconds used during the update process. [Perm.UPDATE_XML]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response")
    })
    @PreAuthorize("hasAuthority('" + Permissions.UPDATE_XML + "')")
    @RequestMapping(value = "/keep_alive_timeout", method = RequestMethod.GET)
    public int getKeepAliveUpdateTimeout() {
        return keepAliveUpdateTimeout;
    }

    @Operation(summary = "Refreshes keep alive update of Arclib Xml of authorial package. [Perm.UPDATE_XML]")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "404", description = "Authorial package with the specified id not found"),
            @ApiResponse(responseCode = "500", description = "Specified XML is not being updated")
    })
    @PreAuthorize("hasAuthority('" + Permissions.UPDATE_XML + "')")
    @RequestMapping(value = "/{authorialPackageId}/keep_alive_update", method = RequestMethod.PUT)
    public void refreshKeepAliveUpdate(
            @Parameter(description = "AIP id", required = true) @PathVariable("authorialPackageId") String authorialPackageId) throws AuthorialPackageNotLockedException {
        aipService.refreshKeepAliveUpdate(authorialPackageId);
    }


    @Autowired
    public void setArchivalStorageService(ArchivalStorageService archivalStorageService) {
        this.archivalStorageService = archivalStorageService;
    }

    @Autowired
    public void setIndexArclibXmlStore(IndexedArclibXmlStore indexArclibXmlStore) {
        this.indexArclibXmlStore = indexArclibXmlStore;
    }

    @Autowired
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }

    @Autowired
    public void setAipService(AipService aipService) {
        this.aipService = aipService;
    }

    @Autowired
    public void setKeepAliveUpdateTimeout(@Value("${arclib.keepAliveUpdateTimeout}") int keepAliveUpdateTimeout) {
        this.keepAliveUpdateTimeout = keepAliveUpdateTimeout;
    }

    @Autowired
    public void setAipQueryService(AipQueryService aipQueryService) {
        this.aipQueryService = aipQueryService;
    }
}
