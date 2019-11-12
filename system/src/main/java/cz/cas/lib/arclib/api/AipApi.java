package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.AipQuery;
import cz.cas.lib.arclib.domain.Hash;
import cz.cas.lib.arclib.domainbase.exception.ForbiddenObject;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.dto.AipDeletionRequestDto;
import cz.cas.lib.arclib.dto.AipDetailDto;
import cz.cas.lib.arclib.dto.AipQueryDto;
import cz.cas.lib.arclib.index.IndexArclibXmlStore;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedAipState;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlDocument;
import cz.cas.lib.arclib.security.authorization.Roles;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.service.AipQueryService;
import cz.cas.lib.arclib.service.AipService;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageException;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStoragePipe;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageResponse;
import cz.cas.lib.arclib.utils.ArclibUtils;
import cz.cas.lib.core.index.dto.Filter;
import cz.cas.lib.core.index.dto.FilterOperation;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import cz.cas.lib.core.store.Transactional;
import io.swagger.annotations.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.dom4j.DocumentException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xml.sax.SAXException;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static cz.cas.lib.arclib.utils.ArclibUtils.hasRole;
import static cz.cas.lib.core.util.Utils.*;

@RestController
@Api(value = "aip", description = "Api for searching within ARCLib XML index, retrieving from Archival Storage and editing of ArclibXml")
@RequestMapping("/api/aip")
@Slf4j
@Transactional
public class AipApi extends ArchivalStoragePipe {

    @Getter
    private IndexArclibXmlStore indexArclibXmlStore;
    private AipService aipService;
    private AipQueryService aipQueryService;
    private int keepAliveUpdateTimeout;

    @ApiOperation(value = "Gets partially filled ARCLib XML index records. The result of query as well as the query " +
            "itself is saved if the queryName param is filled.",
            notes = "If the calling user is not Roles.SUPER_ADMIN, only the respective records of the users producer are returned." +
                    "Records with state REMOVED and DELETED are returned only to the user with role Roles.SUPER_ADMIN or Roles.ADMIN.\n" +
                    "Sort fields: producer_name, user_name, aip_state, sip_version_number,  xml_version_number, created, updated, id, authorial_id\n" +
                    " ..producerId filter will have effect only for super admin account",
            response = Result.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")})
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public Result list(
            @ApiParam(value = "Parameters to comply with", required = true)
            @ModelAttribute Params params,
            @ApiParam(value = "Save query")
            @RequestParam(value = "save", defaultValue = "false") boolean save,
            @ApiParam(value = "Query name")
            @RequestParam(value = "queryName", defaultValue = "", required = false) String queryName) {
        if (!hasRole(userDetails, Roles.SUPER_ADMIN)) {
            addPrefilter(params, new Filter(IndexedArclibXmlDocument.PRODUCER_ID, FilterOperation.EQ, userDetails.getProducerId(), null));
        }
        if (!(hasRole(userDetails, Roles.ADMIN) || hasRole(userDetails, Roles.SUPER_ADMIN))) {
            addPrefilter(params, new Filter(null, FilterOperation.AND, null, asList(
                    new Filter(IndexedArclibXmlDocument.AIP_STATE, FilterOperation.NEQ, IndexedAipState.REMOVED.toString(), null),
                    new Filter(IndexedArclibXmlDocument.AIP_STATE, FilterOperation.NEQ, IndexedAipState.DELETED.toString(), null)))
            );
        }
        return indexArclibXmlStore.findAll(params, queryName);
    }

    @ApiOperation(value = "Gets all main fields of ARCLib XML index record together with corresponding IW entity containing SIPs folder structure.",
            notes = "If the calling user is not Roles.SUPER_ADMIN, the producer of the record must match the producer of the user.",
            response = AipDetailDto.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Ingest workflow not found."),
            @ApiResponse(code = 500, message = "Hash of the indexed ARCLib XML does not match the expected hash.")})
    @RequestMapping(value = "/{xmlId}", method = RequestMethod.GET)
    public AipDetailDto get(
            @ApiParam(value = "Ingest workflow external id", required = true)
            @PathVariable(value = "xmlId") String xmlId) throws IOException {
        return aipService.get(xmlId, userDetails);
    }

    @ApiOperation(value = "Gets AIP data in a .zip")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 400, message = "aipId is not a valid UUID.")})
    @RequestMapping(value = "/export/{aipId}", method = RequestMethod.GET)
    public void exportAip(
            @ApiParam(value = "AIP ID", required = true) @PathVariable("aipId") String aipId,
            @ApiParam(value = "True to return all XMLs, otherwise only the latest is returned")
            @RequestParam(value = "all", defaultValue = "false") boolean all,
            HttpServletResponse response) throws ArchivalStorageException {
        checkUUID(aipId);
        ArchivalStorageResponse storageResponse = aipService.exportSingleAip(aipId, all);
        try {
            HttpStatus status = storageResponse.getStatusCode();
            if (status.is2xxSuccessful()) {
                response.setContentType("application/zip");
                response.addHeader("Content-Disposition", "attachment; filename=" + ArclibUtils.getAipExportName(aipId));
            } else
                response.setContentType("text/plain");
            response.setStatus(status.value());
            IOUtils.copyLarge(storageResponse.getBody(), response.getOutputStream());
        } catch (IOException e) {
            throw new ArchivalStorageException(e);
        }
    }

    @ApiOperation(value = "Returns specified AIP XML")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 400, message = "aipId is not a valid UUID.")})
    @RequestMapping(value = "/export/{aipId}/xml", method = RequestMethod.GET)
    public void exportXml(
            @ApiParam(value = "AIP ID", required = true)
            @PathVariable("aipId") String aipId,
            @ApiParam(value = "Version number of XML, if not set the latest version is returned")
            @RequestParam(value = "v", defaultValue = "") Integer version,
            HttpServletResponse response) throws ArchivalStorageException {
        checkUUID(aipId);
        ArchivalStorageResponse storageResponse = aipService.exportSingleXml(aipId, version);
        try {
            HttpStatus status = storageResponse.getStatusCode();
            if (status.is2xxSuccessful()) {
                response.setContentType("application/xml");
                response.addHeader("Content-Disposition", "attachment; filename=" + ArclibUtils.getXmlExportName(aipId, version));
            } else
                response.setContentType("text/plain");
            response.setStatus(status.value());
            IOUtils.copyLarge(storageResponse.getBody(), response.getOutputStream());
        } catch (IOException e) {
            throw new ArchivalStorageException(e);
        }
    }

    @ApiOperation(notes = "If the AIP is in PROCESSING state or the storage is not reachable, the storage checksums are not filled and the consistent flag is set to false", value = "Retrieves information about AIP containing state, whether is consistent etc from the given storage.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "AIP successfully deleted"),
            @ApiResponse(code = 400, message = "bad request, e.g. the specified id is not a valid UUID"),
            @ApiResponse(code = 500, message = "internal server error")
    })
    @RolesAllowed({Roles.SUPER_ADMIN})
    @RequestMapping(value = "/{aipId}/info", method = RequestMethod.GET)
    public void getAipInfo(
            @ApiParam(value = "AIP id", required = true) @PathVariable("aipId") String aipId,
            @ApiParam(value = "id of the logical storage", required = true) @RequestParam(value = "storageId") String storageId,
            HttpServletResponse response, HttpServletRequest request) {
        checkUUID(aipId);
        passToArchivalStorage(response, request, "/storage/" + aipId + "/info?storageId=" + storageId, HttpMethod.GET, "retrieve info about AIP: " + aipId + "at storage: " + storageId, AccessTokenType.READ);
    }

    @ApiOperation(value = "Gets all saved queries of the user",
            response = AipQuery.class, responseContainer = "list")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")})
    @RequestMapping(value = "/saved_query", method = RequestMethod.GET)
    @Transactional
    public List<AipQuery> getSavedQueries() {
        return aipQueryService.findQueriesOfUser(userDetails.getId());
    }

    @ApiOperation(value = "Gets DTOs of all saved queries of the user",
            response = AipQuery.class, responseContainer = "list")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")})
    @RequestMapping(value = "/saved_query_dtos", method = RequestMethod.GET)
    @Transactional
    public List<AipQueryDto> getSavedQueryDtos() {
        return aipQueryService.listSavedQueryDtos(userDetails.getId());
    }

    @ApiOperation(value = "Deletes saved query. Roles.ADMIN, Roles.SUPER_ADMIN",
            notes = "If the calling user is not Roles.SUPER_ADMIN, the users producer must match the producer of the saved query.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")})
    @RolesAllowed({Roles.ADMIN, Roles.SUPER_ADMIN})
    @RequestMapping(value = "/saved_query/{id}", method = RequestMethod.DELETE)
    public void deleteSavedQuery(@ApiParam(value = "Id of the instance", required = true)
                                 @PathVariable("id") String id) {
        AipQuery query = aipQueryService.find(id);
        notNull(query, () -> new MissingObject(aipQueryService.getClass(), id));
        if (!hasRole(userDetails, Roles.SUPER_ADMIN) &&
                !userDetails.getProducerId().equals(query.getUser().getProducer().getId()))
            throw new ForbiddenObject(AipQuery.class, id);
        aipQueryService.delete(query);
        log.debug("Aip query: " + id + " has been deleted.");
    }

    @ApiOperation(value = "Logically removes AIP at archival storage. Roles.ADMIN, Roles.SUPER_ADMIN, Roles.ARCHIVIST")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "AIP successfully removed"),
            @ApiResponse(code = 400, message = "The specified aip id is not a valid UUID"),
    })
    @RolesAllowed({Roles.ADMIN, Roles.SUPER_ADMIN, Roles.ARCHIVIST})
    @RequestMapping(value = "/{aipId}/remove", method = RequestMethod.PUT)
    public void remove(
            @ApiParam(value = "AIP id", required = true) @PathVariable("aipId") String aipId,
            HttpServletResponse response) throws ArchivalStorageException {
        checkUUID(aipId);
        ArchivalStorageResponse storageResponse = aipService.removeAip(aipId);
        int status = 500;
        try {
            status = storageResponse.getStatusCode().value();
            IOUtils.copy(storageResponse.getBody(), response.getOutputStream());
        } catch (IOException e) {
            throw new ArchivalStorageException(e);
        } finally {
            response.setStatus(status);
        }
    }

    @ApiOperation(value = "Renews logically removed AIP at archival storage. Roles.ADMIN, Roles.SUPER_ADMIN, Roles.ARCHIVIST")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "AIP successfully renewed"),
            @ApiResponse(code = 400, message = "The specified aip id is not a valid UUID"),
    })
    @RolesAllowed({Roles.ADMIN, Roles.SUPER_ADMIN, Roles.ARCHIVIST})
    @RequestMapping(value = "/{aipId}/renew", method = RequestMethod.PUT)
    public void renew(
            @ApiParam(value = "AIP id", required = true)
            @PathVariable("aipId") String aipId, HttpServletResponse response)
            throws ArchivalStorageException {
        checkUUID(aipId);
        int status = 500;
        ArchivalStorageResponse storageResponse = aipService.renewAip(aipId);
        try {
            status = storageResponse.getStatusCode().value();
            IOUtils.copy(storageResponse.getBody(), response.getOutputStream());
        } catch (IOException e) {
            throw new ArchivalStorageException(e);
        } finally {
            response.setStatus(status);
        }
    }

    @ApiOperation(value = "Creates deletion request for deletion of AIP at archival storage. Roles.ADMIN, Roles.SUPER_ADMIN, Roles.ARCHIVIST")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "AIP successfully deleted"),
            @ApiResponse(code = 400, message = "The specified aip id is not a valid UUID"),
            @ApiResponse(code = 409, message = "Deletion request for the user and aip id already exists.")
    })
    @RolesAllowed({Roles.ADMIN, Roles.SUPER_ADMIN, Roles.ARCHIVIST})
    @RequestMapping(value = "/{aipId}", method = RequestMethod.DELETE)
    public void delete(
            @ApiParam(value = "AIP id", required = true)
            @PathVariable("aipId") String aipId) {
        checkUUID(aipId);
        aipService.createDeletionRequest(aipId);
    }

    @ApiOperation(value = "Registers update of Arclib Xml of authorial package. Roles.SUPER_ADMIN, Roles.EDITOR")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Xml update successfully registered"),
            @ApiResponse(code = 404, message = "Authorial package with the specified id not found"),
            @ApiResponse(code = 423, message = "Another update process of the XML is already in progress")
    })
    @RolesAllowed({Roles.SUPER_ADMIN, Roles.EDITOR})
    @RequestMapping(value = "/{authorialPackageId}/register_update", method = RequestMethod.PUT)
    public void registerXmlUpdate(
            @ApiParam(value = "Authorial package id", required = true)
            @PathVariable("authorialPackageId") String authorialPackageId) throws IOException {
        aipService.registerXmlUpdate(authorialPackageId);
    }

    @ApiOperation(value = "Cancels update of Arclib Xml of authorial package. Roles.SUPER_ADMIN, Roles.EDITOR")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Xml update successfully canceled"),
            @ApiResponse(code = 404, message = "Authorial package with the specified id not found"),
            @ApiResponse(code = 500, message = "Specified XML is not being updated")
    })
    @RolesAllowed({Roles.SUPER_ADMIN, Roles.EDITOR})
    @RequestMapping(value = "/{authorialPackageId}/cancel_update", method = RequestMethod.PUT)
    public void cancelXmlUpdate(
            @ApiParam(value = "Authorial package id", required = true)
            @PathVariable("authorialPackageId") String authorialPackageId) {
        aipService.cancelXmlUpdate(authorialPackageId);
    }

    @ApiOperation(value = "Returns keep alive timeout in seconds used during the update process.")
    @RequestMapping(value = "/keep_alive_timeout", method = RequestMethod.GET)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")
    })
    public int getKeepAliveUpdateTimeout() {
        return keepAliveUpdateTimeout;
    }

    @ApiOperation(value = "Refreshes keep alive update of Arclib Xml of authorial package. Roles.SUPER_ADMIN, Roles.EDITOR")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "Authorial package with the specified id not found"),
            @ApiResponse(code = 500, message = "Specified XML is not being updated")
    })
    @RolesAllowed({Roles.SUPER_ADMIN, Roles.EDITOR})
    @RequestMapping(value = "/{authorialPackageId}/keep_alive_update", method = RequestMethod.PUT)
    public void refreshKeepAliveUpdate(
            @ApiParam(value = "AIP id", required = true) @PathVariable("authorialPackageId") String authorialPackageId) {
        aipService.refreshKeepAliveUpdate(authorialPackageId);
    }

    @ApiOperation(value = "Updates Arclib XML of AIP. Register update must be called prior to calling this method. " +
            "Roles.SUPER_ADMIN, Roles.EDITOR")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "AIP XML successfully stored"),
            @ApiResponse(code = 400, message = "the specified aip id is not a valid UUID"),
    })
    @RolesAllowed({Roles.SUPER_ADMIN, Roles.EDITOR})
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
            @RequestParam("reason") String reason,
            HttpServletResponse response) throws DocumentException, SAXException, ParserConfigurationException, ArchivalStorageException, IOException {
        checkUUID(aipId);
        ResponseEntity<String> archivalStorageResponse = aipService.finishXmlUpdate(aipId, xmlId, xml, hash, version, reason);
        try {
            String body = archivalStorageResponse.getBody();
            if (body != null) IOUtils.copy(new ByteArrayInputStream(body.getBytes()), response.getOutputStream());
        } catch (IOException e) {
            throw new ArchivalStorageException(e);
        } finally {
            response.setStatus(archivalStorageResponse.getStatusCode().value());
        }
    }

    @ApiOperation(value = "Gets requests for AIP deletion waiting to be resolved that have not yet been acknowledged " +
            "by the current user. Roles.DELETION_ACKNOWLEDGE, Roles.SUPER_ADMIN",
            response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")})
    @RequestMapping(value = "/list_deletion_requests", method = RequestMethod.GET)
    public List<AipDeletionRequestDto> listDeletionRequests() {
        return aipService.listDeletionRequests();
    }

    @ApiOperation(value = "Acknowledge deletion request. Roles.DELETION_ACKNOWLEDGE, Roles.SUPER_ADMIN",
            response = Result.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")})
    @RolesAllowed({Roles.DELETION_ACKNOWLEDGE, Roles.SUPER_ADMIN})
    @RequestMapping(value = "{deletionRequestId}/acknowledge_deletion", method = RequestMethod.PUT)
    public void acknowledgeDeletion(
            @ApiParam(value = "Deletion request id", required = true)
            @PathVariable("deletionRequestId") String deletionRequestId) {
        aipService.acknowledgeDeletion(deletionRequestId);
    }

    @ApiOperation(value = "Disacknowledge deletion request. Roles.DELETION_ACKNOWLEDGE, Roles.SUPER_ADMIN",
            response = Result.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")})
    @RolesAllowed({Roles.DELETION_ACKNOWLEDGE, Roles.SUPER_ADMIN})
    @RequestMapping(value = "{deletionRequestId}/disacknowledge_deletion", method = RequestMethod.PUT)
    public void disacknowledgeDeletion(
            @ApiParam(value = "Deletion request id", required = true)
            @PathVariable("deletionRequestId") String deletionRequestId) {
        aipService.disacknowledgeDeletion(deletionRequestId);
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
