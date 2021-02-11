package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.AipQuery;
import cz.cas.lib.arclib.domain.Hash;
import cz.cas.lib.arclib.domainbase.exception.ForbiddenObject;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.dto.AipDetailDto;
import cz.cas.lib.arclib.dto.AipQueryDto;
import cz.cas.lib.arclib.exception.AipStateChangeException;
import cz.cas.lib.arclib.exception.AuthorialPackageNotLockedException;
import cz.cas.lib.arclib.exception.ForbiddenException;
import cz.cas.lib.arclib.index.IndexArclibXmlStore;
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
import io.swagger.annotations.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.dom4j.DocumentException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static cz.cas.lib.arclib.utils.ArclibUtils.hasRole;
import static cz.cas.lib.core.util.Utils.*;

@RestController
@Api(value = "aip", description = "Api for searching within ARCLib XML index, retrieving from Archival Storage and editing of ArclibXml")
@RequestMapping("/api/aip")
@Slf4j
public class AipApi extends ArchivalStoragePipe {

    @Getter
    private IndexArclibXmlStore indexArclibXmlStore;
    private AipService aipService;
    private AipQueryService aipQueryService;
    private int keepAliveUpdateTimeout;
    private ArchivalStorageService archivalStorageService;

    @ApiOperation(value = "Gets partially filled ARCLib XML index records. The result of query as well as the query itself is saved if the queryName param is filled. [Perm.AIP_RECORDS_READ]",
            notes = "If the calling user is not Roles.SUPER_ADMIN, only the respective records of the users producer are returned." +
                    "Records with state REMOVED and DELETED are returned only to the user with role Roles.SUPER_ADMIN or Roles.ADMIN.\n" +
                    "Sort fields: producer_name, user_name, aip_state, sip_version_number,  xml_version_number, created, updated, id, authorial_id\n" +
                    " ..producerId filter will have effect only for super admin account",
            response = Result.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")})
    @PreAuthorize("hasAuthority('" + Permissions.AIP_RECORDS_READ + "')")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public Result<IndexedArclibXmlDocument> list(
            @ApiParam(value = "Parameters to comply with", required = true)
            @ModelAttribute Params params,
            @ApiParam(value = "Save query")
            @RequestParam(value = "save", defaultValue = "false") boolean save,
            @ApiParam(value = "Query name")
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
        return indexArclibXmlStore.findAll(params, queryName);
    }

    @ApiOperation(value = "Gets all main fields of ARCLib XML index record together with corresponding IW entity containing SIPs folder structure. [Perm.AIP_RECORDS_READ]",
            notes = "If the calling user is not Roles.SUPER_ADMIN, the producer of the record must match the producer of the user.",
            response = AipDetailDto.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Ingest workflow not found.")})
    @PreAuthorize("hasAuthority('" + Permissions.AIP_RECORDS_READ + "')")
    @RequestMapping(value = "/{xmlId}", method = RequestMethod.GET)
    public AipDetailDto get(
            @ApiParam(value = "Ingest workflow external id", required = true)
            @PathVariable(value = "xmlId") String xmlId) throws IOException {
        return aipService.get(xmlId, userDetails);
    }

    @ApiOperation(value = "Gets AIP data in a .zip [Perm.EXPORT_FILES]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 400, message = "aipId is not a valid UUID.")})
    @PreAuthorize("hasAuthority('" + Permissions.EXPORT_FILES + "')")
    @RequestMapping(value = "/export/{aipId}", method = RequestMethod.GET)
    public void exportAip(
            @ApiParam(value = "AIP ID", required = true) @PathVariable("aipId") String aipId,
            @ApiParam(value = "True to return all XMLs, otherwise only the latest is returned")
            @RequestParam(value = "all", defaultValue = "false") boolean all,
            HttpServletResponse response) throws ArchivalStorageException, IOException {
        checkUUID(aipId);
        InputStream storageResponse = archivalStorageService.exportSingleAip(aipId, all);
        response.setContentType("application/zip");
        response.addHeader("Content-Disposition", "attachment; filename=" + ArclibUtils.getAipExportName(aipId));
        IOUtils.copyLarge(storageResponse, response.getOutputStream());
    }

    @ApiOperation(value = "Returns specified AIP XML [Perm.EXPORT_FILES]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 400, message = "aipId is not a valid UUID.")})
    @PreAuthorize("hasAuthority('" + Permissions.EXPORT_FILES + "')")
    @RequestMapping(value = "/export/{aipId}/xml", method = RequestMethod.GET)
    public void exportXml(
            @ApiParam(value = "AIP ID", required = true)
            @PathVariable("aipId") String aipId,
            @ApiParam(value = "Version number of XML, if not set the latest version is returned")
            @RequestParam(value = "v", defaultValue = "") Integer version,
            HttpServletResponse response) throws ArchivalStorageException, IOException {
        checkUUID(aipId);
        InputStream storageResponse = archivalStorageService.exportSingleXml(aipId, version);
        response.setContentType("application/xml");
        response.addHeader("Content-Disposition", "attachment; filename=" + ArclibUtils.getXmlExportName(aipId, version));
        IOUtils.copyLarge(storageResponse, response.getOutputStream());
    }

    @ApiOperation(notes = "If the AIP is in PROCESSING state or the storage is not reachable, the storage checksums are not filled and the consistent flag is set to false",
            value = "Retrieves information about AIP containing state, whether is consistent etc from the given storage. [Perm.AIP_RECORDS_READ]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "AIP successfully deleted"),
            @ApiResponse(code = 400, message = "bad request, e.g. the specified id is not a valid UUID"),
            @ApiResponse(code = 500, message = "internal server error")
    })
    @PreAuthorize("hasAuthority('" + Permissions.AIP_RECORDS_READ + "')")
    @RequestMapping(value = "/{aipId}/info", method = RequestMethod.GET)
    public void getAipInfo(
            @ApiParam(value = "AIP id", required = true) @PathVariable("aipId") String aipId,
            @ApiParam(value = "id of the logical storage", required = true) @RequestParam(value = "storageId") String storageId,
            HttpServletResponse response, HttpServletRequest request) {
        checkUUID(aipId);
        passToArchivalStorage(response, request, "/storage/" + aipId + "/info?storageId=" + storageId, HttpMethod.GET, "retrieve info about AIP: " + aipId + "at storage: " + storageId, AccessTokenType.READ);
    }

    @ApiOperation(value = "Gets saved query by ID [Perm.AIP_QUERY_RECORDS_READ]", response = AipQuery.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")})
    @PreAuthorize("hasAuthority('" + Permissions.AIP_QUERY_RECORDS_READ + "')")
    @RequestMapping(value = "/saved_query/{id}", method = RequestMethod.GET)
    public AipQuery getSavedQuery(@ApiParam(value = "Id of the instance", required = true)
                                  @PathVariable("id") String id) {
        return aipQueryService.find(id);
    }

    @ApiOperation(value = "Gets DTOs of all saved queries of the user [Perm.AIP_QUERY_RECORDS_READ]",
            response = AipQueryDto.class, responseContainer = "list")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")})
    @PreAuthorize("hasAuthority('" + Permissions.AIP_QUERY_RECORDS_READ + "')")
    @RequestMapping(value = "/saved_query_dtos", method = RequestMethod.GET)
    public List<AipQueryDto> getSavedQueryDtos() {
        return aipQueryService.listSavedQueryDtos(userDetails.getId());
    }

    @ApiOperation(value = "Deletes saved query. [Perm.AIP_QUERY_RECORDS_WRITE]",
            notes = "If the calling user is not Roles.SUPER_ADMIN, the users producer must match the producer of the saved query.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful response") })
    @PreAuthorize("hasAuthority('" + Permissions.AIP_QUERY_RECORDS_WRITE + "')")
    @RequestMapping(value = "/saved_query/{id}", method = RequestMethod.DELETE)
    public void deleteSavedQuery(@ApiParam(value = "Id of the instance", required = true)
                                 @PathVariable("id") String id) {
        AipQuery query = aipQueryService.findWithUserInitialized(id);
        notNull(query, () -> new MissingObject(aipQueryService.getClass(), id));
        if (!hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE) &&
                !userDetails.getProducerId().equals(query.getUser().getProducer().getId()))
            throw new ForbiddenObject(AipQuery.class, id);
        aipQueryService.delete(query);
        log.debug("Aip query: " + id + " has been deleted.");
    }

    @ApiOperation(value = "Logically removes AIP at archival storage. [Perm.LOGICAL_FILE_REMOVE]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "AIP successfully removed"),
            @ApiResponse(code = 400, message = "The specified aip id is not a valid UUID"),
    })
    @PreAuthorize("hasAuthority('" + Permissions.LOGICAL_FILE_REMOVE + "')")
    @RequestMapping(value = "/{aipId}/remove", method = RequestMethod.PUT)
    public void remove(@ApiParam(value = "AIP id", required = true) @PathVariable("aipId") String aipId, HttpServletResponse response) throws ArchivalStorageException, AipStateChangeException, IOException {
        checkUUID(aipId);
        aipService.changeAipState(aipId, IndexedAipState.REMOVED);
    }

    @ApiOperation(value = "Renews logically removed AIP at archival storage. [Perm.LOGICAL_FILE_RENEW]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "AIP successfully renewed"),
            @ApiResponse(code = 400, message = "The specified aip id is not a valid UUID"),
    })
    @PreAuthorize("hasAuthority('" + Permissions.LOGICAL_FILE_RENEW + "')")
    @RequestMapping(value = "/{aipId}/renew", method = RequestMethod.PUT)
    public void renew(@ApiParam(value = "AIP id", required = true) @PathVariable("aipId") String aipId) throws ArchivalStorageException, AipStateChangeException, IOException {
        checkUUID(aipId);
        aipService.changeAipState(aipId, IndexedAipState.ARCHIVED);
    }

    @ApiOperation(value = "Registers update of Arclib Xml of authorial package. [Perm.UPDATE_XML]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Xml update successfully registered"),
            @ApiResponse(code = 404, message = "Authorial package with the specified id not found"),
            @ApiResponse(code = 423, message = "Another update process of the XML is already in progress")
    })
    @PreAuthorize("hasAuthority('" + Permissions.UPDATE_XML + "')")
    @RequestMapping(value = "/{authorialPackageId}/register_update", method = RequestMethod.PUT)
    public void registerXmlUpdate(
            @ApiParam(value = "Authorial package id", required = true)
            @PathVariable("authorialPackageId") String authorialPackageId) throws IOException {
        aipService.registerXmlUpdate(authorialPackageId);
    }

    @ApiOperation(value = "Cancels update of Arclib Xml of authorial package. [Perm.UPDATE_XML]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Xml update successfully canceled"),
            @ApiResponse(code = 404, message = "Authorial package with the specified id not found"),
            @ApiResponse(code = 500, message = "Specified XML is not being updated")
    })
    @PreAuthorize("hasAuthority('" + Permissions.UPDATE_XML + "')")
    @RequestMapping(value = "/{authorialPackageId}/cancel_update", method = RequestMethod.PUT)
    public void cancelXmlUpdate(
            @ApiParam(value = "Authorial package id", required = true)
            @PathVariable("authorialPackageId") String authorialPackageId) {
        log.debug("Canceling XML update for authorial package " + authorialPackageId + ".");
        aipService.deactivateLock(authorialPackageId);
    }

    @ApiOperation(value = "Updates Arclib XML of AIP. Register update must be called prior to calling this method. [Perm.UPDATE_XML]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "AIP XML successfully stored"),
            @ApiResponse(code = 400, message = "the specified aip id is not a valid UUID"),
    })
    @PreAuthorize("hasAuthority('" + Permissions.UPDATE_XML + "')")
    @RequestMapping(value = "/{aipId}/update", method = RequestMethod.POST)
    public void finishXmlUpdate(
            @ApiParam(value = "XML content", required = true)
            @RequestBody String xml,
            @ApiParam(value = "AIP id", required = true)
            @PathVariable("aipId") String aipId,
            @ApiParam(value = "XML id of the latest version XML of the AIP", required = true)
            @RequestParam("xmlId") String xmlId,
            @ApiParam(value = "Hash of the XML", required = true)
            @ModelAttribute("xmlHash") Hash hash,
            @ApiParam(value = "Version of the XML", required = true)
            @RequestParam("version") int version,
            @ApiParam(value = "Reason for update", required = true)
            @RequestParam("reason") String reason) throws DocumentException, SAXException, ParserConfigurationException, IOException, AuthorialPackageNotLockedException, ArchivalStorageException {
        checkUUID(aipId);
        aipService.finishXmlUpdate(aipId, xmlId, xml, hash, version, reason);
    }

    @ApiOperation(value = "Returns keep alive timeout in seconds used during the update process. [Perm.UPDATE_XML]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")
    })
    @PreAuthorize("hasAuthority('" + Permissions.UPDATE_XML + "')")
    @RequestMapping(value = "/keep_alive_timeout", method = RequestMethod.GET)
    public int getKeepAliveUpdateTimeout() {
        return keepAliveUpdateTimeout;
    }

    @ApiOperation(value = "Refreshes keep alive update of Arclib Xml of authorial package. [Perm.UPDATE_XML]")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Authorial package with the specified id not found"),
            @ApiResponse(code = 500, message = "Specified XML is not being updated")
    })
    @PreAuthorize("hasAuthority('" + Permissions.UPDATE_XML + "')")
    @RequestMapping(value = "/{authorialPackageId}/keep_alive_update", method = RequestMethod.PUT)
    public void refreshKeepAliveUpdate(
            @ApiParam(value = "AIP id", required = true) @PathVariable("authorialPackageId") String authorialPackageId) throws AuthorialPackageNotLockedException {
        aipService.refreshKeepAliveUpdate(authorialPackageId);
    }


    @Inject
    public void setArchivalStorageService(ArchivalStorageService archivalStorageService) {
        this.archivalStorageService = archivalStorageService;
    }

    @Inject
    public void setIndexArclibXmlStore(IndexArclibXmlStore indexArclibXmlStore) {
        this.indexArclibXmlStore = indexArclibXmlStore;
    }

    @Inject
    public void setAipQueryService(AipQueryService aipQueryService) {
        this.aipQueryService = aipQueryService;
    }

    @Inject
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }

    @Inject
    public void setAipService(AipService aipService) {
        this.aipService = aipService;
    }

    @Inject
    public void setKeepAliveUpdateTimeout(@Value("${arclib.keepAliveUpdateTimeout}") int keepAliveUpdateTimeout) {
        this.keepAliveUpdateTimeout = keepAliveUpdateTimeout;
    }
}
