package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.security.authorization.Roles;
import cz.cas.lib.arclib.security.user.UserDetails;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@RestController
@Api(value = "Api for administration of the logical storages of the Archival Storage",
        description = "These endpoints are identical to the endpoints of the Archival Storage + adds authorization." +
                " See documentation of Archival Storage for documentation of input and output data of these endpoints.")
@RequestMapping("/api/arcstorage/administration/storage")
@RolesAllowed({Roles.SUPER_ADMIN})
@Slf4j
public class ArchivalStorageAdministrationApi {

    private String baseEndpoint;
    private String adminBearerToken;
    private UserDetails userDetails;

    @RequestMapping(method = RequestMethod.GET)
    public void getAll(HttpServletResponse response, HttpServletRequest request) {
        passToArchivalStorage(response, request, "/administration/storage", HttpMethod.GET, "retrieve all storages");
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{id}")
    public void getOne(@ApiParam(value = "id of the logical storage", required = true) @PathVariable("id") String id,
                       HttpServletResponse response, HttpServletRequest request) {
        passToArchivalStorage(response, request, "/administration/storage/" + id, HttpMethod.GET, "retrieve data of storage with id: " + id);
    }

    @RequestMapping(method = RequestMethod.POST)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "body", required = true, paramType = "body")
    })
    public void attachStorage(HttpServletResponse response, HttpServletRequest request) {
        passToArchivalStorage(response, request, "/administration/storage", HttpMethod.POST, "attach new storage");
    }

    @RequestMapping(value = "/sync/{id}", method = RequestMethod.POST)
    public void continueSync(
            @ApiParam(value = "id of the synchronization status entity", required = true) @PathVariable("id") String id,
            HttpServletResponse response, HttpServletRequest request) {
        passToArchivalStorage(response, request, "/administration/storage/sync/" + id, HttpMethod.POST, "continue with synchronization with id: " + id + " of storage");
    }

    @RequestMapping(value = "/sync/{id}", method = RequestMethod.GET)
    public void getSyncStatusOfStorage(
            @ApiParam(value = "id of the storage", required = true) @PathVariable("id") String id,
            HttpServletResponse response, HttpServletRequest request) {
        passToArchivalStorage(response, request, "/administration/storage/sync/" + id, HttpMethod.GET, "retrieve synchronization status storage with id: " + id);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "body", required = true, paramType = "body")
    })
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public void update(HttpServletResponse response, HttpServletRequest request) {
        passToArchivalStorage(response, request, "/administration/storage/update", HttpMethod.POST, "update storage");
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void delete(
            @ApiParam(value = "id of the logical storage", required = true) @PathVariable("id") String id,
            HttpServletResponse response, HttpServletRequest request) {
        passToArchivalStorage(response, request, "/administration/storage/" + id, HttpMethod.DELETE, "delete storage with id: " + id);
    }


    private void passToArchivalStorage(HttpServletResponse response, HttpServletRequest request, String path, HttpMethod httpMethod, String operation) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.setErrorHandler(new ResponseErrorHandler() {
                @Override
                public boolean hasError(ClientHttpResponse response) throws IOException {
                    return false;
                }

                @Override
                public void handleError(ClientHttpResponse response) throws IOException {

                }
            });
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + adminBearerToken);
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            byte[] requestBody = IOUtils.toByteArray(request.getInputStream());
            HttpEntity<byte[]> requestEntity = new HttpEntity<byte[]>(requestBody, headers);
            ResponseEntity<byte[]> exchange = restTemplate.exchange(baseEndpoint + path, httpMethod, requestEntity, byte[].class);
            response.setStatus(exchange.getStatusCodeValue());
            if (exchange.getBody() != null) {
                IOUtils.copy(new ByteArrayInputStream(exchange.getBody()), response.getOutputStream());
            }
            String responseBody = exchange.getBody() == null? "empty":IOUtils.toString(exchange.getBody());
            String logMsg = "user: " + userDetails.getUsername() + ", operation: " + operation + " request body: " + IOUtils.toString(requestBody) + "response body: " + responseBody;
            if (exchange.getStatusCode().is2xxSuccessful()) {
                log.info(logMsg);
            } else
                log.error(logMsg);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Inject
    public void setBaseEndpoint(@Value("${archivalStorage.api}") String baseEndpoint) {
        this.baseEndpoint = baseEndpoint;
    }

    @Inject
    public void setAdminBearerToken(@Value("${archivalStorage.authorization.bearer.admin}") String adminBearerToken) {
        this.adminBearerToken = adminBearerToken;
    }

    @Inject
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }
}
