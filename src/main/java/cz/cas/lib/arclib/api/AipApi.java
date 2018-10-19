package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.AipQuery;
import cz.cas.lib.arclib.domain.Hash;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.packages.AipDeletionRequest;
import cz.cas.lib.arclib.dto.AipDetailDto;
import cz.cas.lib.arclib.index.IndexArclibXmlStore;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlDocumentState;
import cz.cas.lib.arclib.index.solr.arclibxml.SolrArclibXmlDocument;
import cz.cas.lib.arclib.security.authorization.Roles;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.service.AipService;
import cz.cas.lib.arclib.store.AipQueryStore;
import cz.cas.lib.arclib.utils.ArclibUtils;
import cz.cas.lib.core.exception.ForbiddenObject;
import cz.cas.lib.core.exception.MissingObject;
import cz.cas.lib.core.index.dto.Filter;
import cz.cas.lib.core.index.dto.FilterOperation;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import cz.cas.lib.core.store.Transactional;
import io.swagger.annotations.*;
import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.dom4j.DocumentException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.bind.annotation.*;
import org.xml.sax.SAXException;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static cz.cas.lib.arclib.utils.ArclibUtils.hasRole;
import static cz.cas.lib.core.util.Utils.*;

@RestController
@Api(value = "aip", description = "Api for searching within ARCLib XML index and retrieving from Archival Storage")
@RequestMapping("/api/aip")
@Transactional
public class AipApi {

    @Getter
    private IndexArclibXmlStore indexArclibXmlStore;
    private AipService aipService;
    private AipQueryStore aipQueryStore;
    private UserDetails userDetails;
    private int keepAliveUpdateTimeout;

    @ApiOperation(value = "Gets partially filled ARCLib XML index records. The result of query as well as the query " +
            "itself is saved if requested.",
            notes = "If the calling user is not Roles.SUPER_ADMIN, only the respective records of the users producer are returned." +
                    "Records with state REMOVED and DELETED are returned only to the user with role Roles.SUPER_ADMIN or Roles.ADMIN.\n" +
                    "Filter/Sort fields: producer_id, user_id, state, label, type, sip_id, created, id, authorial_id," +
                    " sip_version_number, sip_version_of, xml_version_number, xml_version_of\n" +
                    "Filter only fields: document, generic_dc, specific_dc, extracted_format_file_format, " +
                    "extracted_format_format_registry_key, extracted_format_format_registry_name, extracted_format_file_count," +
                    " extracted_format_creating_application_name, extracted_format_creating_application_version," +
                    " extracted_format_date_created_by_application, extracted_format_preservation_level_value, " +
                    "extracted_format_scanner_model_serial_no, identified_format_format_registry_key," +
                    "identified_format_format_registry_name, identified_format_file_count, identified_format_creating_application_name, " +
                    "identified_format_creating_application_version, identified_format_date_created_by_application," +
                    "device_id, device_file_count, img_metadata_date_created, img_metadata_image_producer, " +
                    "img_metadata_scanner_model_serial_no, img_metadata_arc_event_count," +
                    "creating_application_creating_application_name, creating_application_creating_application_version, " +
                    "creating_application_date_created_by_application, creating_application_event_count, " +
                    "premis_event_outcome, premis_event_agent_id, premis_linking_agent_identifier_type, " +
                    "premis_linking_agent_identifier_value, premis_event_detail, premis_event_type, premis_event_date_time, " +
                    "premis_event_identifier_type, premis_event_identifier_value, arc_event_type, arc_event_agent_name, " +
                    "linking_device_id, scanner_model_serial_no, scanning_software_name, arc_event_date, arc_event_count," +
                    " event_ingestion, event_validation\n" +
                    " ..producerId filter will have effect only for super admin account",
            response = Result.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")})
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public Result list(
            @ApiParam(value = "Parameters to comply with", required = true)
            @ModelAttribute Params params,
            @ApiParam(value = "Save query")
            @RequestParam(value = "save", defaultValue = "false") boolean save) {
        if (!hasRole(userDetails, Roles.SUPER_ADMIN)) {
            addPrefilter(params, new Filter(SolrArclibXmlDocument.PRODUCER_ID, FilterOperation.EQ, userDetails.getProducerId(), null));
        }
        if (!(hasRole(userDetails, Roles.ADMIN) || hasRole(userDetails, Roles.SUPER_ADMIN))) {
            addPrefilter(params, new Filter(null, FilterOperation.AND, null, asList(
                    new Filter(SolrArclibXmlDocument.STATE, FilterOperation.NEQ, IndexedArclibXmlDocumentState.REMOVED.toString(), null),
                    new Filter(SolrArclibXmlDocument.STATE, FilterOperation.NEQ, IndexedArclibXmlDocumentState.DELETED.toString(), null)))
            );
        }
        return indexArclibXmlStore.findAll(params, save);
    }

    @ApiOperation(value = "Gets all fields of ARCLib XML index record together with corresponding IW entity containing SIPs folder structure.",
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
        Map<String, Object> arclibXmlIndexDocument = indexArclibXmlStore.findArclibXmlIndexDocument(xmlId);
        notNull(arclibXmlIndexDocument, () -> new MissingObject(SolrArclibXmlDocument.class, xmlId));

        String producerId = (String) ((ArrayList) arclibXmlIndexDocument.get(SolrArclibXmlDocument.PRODUCER_ID)).get(0);
        if (!hasRole(userDetails, Roles.SUPER_ADMIN) && !userDetails.getProducerId().equals(producerId)) {
            throw new ForbiddenObject(IngestWorkflow.class, xmlId);
        }
        return aipService.get(xmlId);
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
            HttpServletResponse response) throws IOException, URISyntaxException {
        checkUUID(aipId);
        response.setContentType("application/zip");
        response.setStatus(200);
        response.addHeader("Content-Disposition", "attachment; filename=" + ArclibUtils.getAipExportName(aipId));
        ClientHttpResponse clientHttpResponse = aipService.exportSingleAip(aipId, all);
        try {
            IOUtils.copyLarge(clientHttpResponse.getBody(), response.getOutputStream());
        } catch (IOException e) {
            if (!(e instanceof FileNotFoundException))
                throw e;
        } finally {
            response.setStatus(clientHttpResponse.getStatusCode().value());
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
            HttpServletResponse response) throws URISyntaxException, IOException {
        checkUUID(aipId);
        response.setContentType("application/xml");
        response.setStatus(200);
        response.addHeader("Content-Disposition", "attachment; filename=" + ArclibUtils.getXmlExportName(aipId, version));
        ClientHttpResponse clientHttpResponse = aipService.exportSingleXml(aipId, version);
        try {
            IOUtils.copyLarge(clientHttpResponse.getBody(), response.getOutputStream());
        } catch (IOException e) {
            if (!(e instanceof FileNotFoundException))
                throw e;
        } finally {
            response.setStatus(clientHttpResponse.getStatusCode().value());
        }
    }

    @ApiOperation(value = "Gets all saved queries of the user",
            response = AipQuery.class, responseContainer = "list")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")})
    @RequestMapping(value = "/saved_query", method = RequestMethod.GET)
    @Transactional
    public List<AipQuery> getSavedQueries() {
        return aipQueryStore.findQueriesOfUser(userDetails.getId());
    }

    @ApiOperation(value = "Deletes saved query. Roles.ADMIN, Roles.SUPER_ADMIN",
            notes = "If the calling user is not Roles.SUPER_ADMIN, the users producer must match the producer of the saved query.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")})
    @RolesAllowed({Roles.ADMIN, Roles.SUPER_ADMIN})
    @RequestMapping(value = "/saved_query/{id}", method = RequestMethod.DELETE)
    @Transactional
    public void deleteSavedQuery(@ApiParam(value = "Id of the instance", required = true)
                                 @PathVariable("id") String id) {
        AipQuery query = aipQueryStore.find(id);
        notNull(query, () -> new MissingObject(aipQueryStore.getClass(), id));
        if (!hasRole(userDetails, Roles.SUPER_ADMIN) &&
                !userDetails.getProducerId().equals(query.getUser().getProducer().getId()))
            throw new ForbiddenObject(AipQuery.class, id);
        aipQueryStore.delete(query);
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
            HttpServletResponse response) throws IOException, URISyntaxException {
        checkUUID(aipId);
        ClientHttpResponse clientHttpResponse = aipService.removeAip(aipId);
        try {
            IOUtils.copy(clientHttpResponse.getBody(), response.getOutputStream());
        } catch (IOException e) {
            if (!(e instanceof FileNotFoundException))
                throw e;
        } finally {
            response.setStatus(clientHttpResponse.getStatusCode().value());
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
            throws IOException, URISyntaxException {
        checkUUID(aipId);
        ClientHttpResponse clientHttpResponse = aipService.renewAip(aipId);
        try {
            IOUtils.copy(clientHttpResponse.getBody(), response.getOutputStream());
        } catch (IOException e) {
            if (!(e instanceof FileNotFoundException))
                throw e;
        } finally {
            response.setStatus(clientHttpResponse.getStatusCode().value());
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

    @ApiOperation(value = "Registers update of Arclib Xml of AIP. Roles.SUPER_ADMIN, Roles.EDITOR")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "AIP update successfully registered"),
            @ApiResponse(code = 404, message = "AIP with the specified id not found"),
            @ApiResponse(code = 500, message = "Another update process of the AIP is already in progress")
    })
    @RolesAllowed({Roles.SUPER_ADMIN, Roles.EDITOR})
    @RequestMapping(value = "/{aipId}/register_update", method = RequestMethod.PUT)
    public void registerXmlUpdate(
            @ApiParam(value = "AIP id", required = true)
            @PathVariable("aipId") String aipId) throws IOException {
        aipService.registerXmlUpdate(aipId);
    }

    @ApiOperation(value = "Cancels update of Arclib Xml of AIP. Roles.SUPER_ADMIN, Roles.EDITOR")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "AIP update successfully canceled"),
            @ApiResponse(code = 404, message = "AIP with the specified id not found"),
            @ApiResponse(code = 500, message = "Specified AIP is not being updated")
    })
    @RolesAllowed({Roles.SUPER_ADMIN, Roles.EDITOR})
    @RequestMapping(value = "/{aipId}/cancel_update", method = RequestMethod.PUT)
    public void cancelXmlUpdate(
            @ApiParam(value = "AIP id", required = true)
            @PathVariable("aipId") String aipId) {
        aipService.cancelXmlUpdate(aipId);
    }

    @ApiOperation(value = "Returns keep alive timeout in seconds used during the update process.")
    @RequestMapping(value = "/keep_alive_timeout", method = RequestMethod.GET)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")
    })
    public int getKeepAliveUpdateTimeout() {
        return keepAliveUpdateTimeout;
    }

    @ApiOperation(value = "Refreshes keep alive update of Arclib Xml of AIP. Roles.SUPER_ADMIN, Roles.EDITOR")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response"),
            @ApiResponse(code = 404, message = "AIP with the specified id not found"),
            @ApiResponse(code = 500, message = "Specified AIP is not being updated")
    })
    @RolesAllowed({Roles.SUPER_ADMIN, Roles.EDITOR})
    @RequestMapping(value = "/{aipId}/keep_alive_update", method = RequestMethod.PUT)
    public void refreshKeepAliveUpdate(
            @ApiParam(value = "AIP id", required = true) @PathVariable("aipId") String aipId) {
        aipService.refreshKeepAliveUpdate(aipId);
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
            HttpServletResponse response) throws IOException, DocumentException, SAXException, ParserConfigurationException {
        checkUUID(aipId);

        ResponseEntity<String> archivalStorageResponse = aipService.finishXmlUpdate(aipId, xmlId, xml, hash, version, reason);
        try {
            String body = archivalStorageResponse.getBody();
            if (body != null) IOUtils.copy(new ByteArrayInputStream(body.getBytes()), response.getOutputStream());
        } catch (IOException e) {
            if (!(e instanceof FileNotFoundException))
                throw e;
        } finally {
            response.setStatus(archivalStorageResponse.getStatusCode().value());
        }
    }

    @ApiOperation(value = "Gets requests for AIP deletion waiting to be resolved that have not yet been acknowledged " +
            "by the current user. Roles.DELETION_ACKNOWLEDGE, Roles.SUPER_ADMIN",
            response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response")})
    @RolesAllowed({Roles.DELETION_ACKNOWLEDGE, Roles.SUPER_ADMIN})
    @RequestMapping(value = "/list_deletion_requests", method = RequestMethod.GET)
    public List<AipDeletionRequest> listDeletionRequests() {
        return aipService.listNonAcknowledgedDeletionRequests();
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
    public void setAipQueryStore(AipQueryStore aipQueryStore) {
        this.aipQueryStore = aipQueryStore;
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
