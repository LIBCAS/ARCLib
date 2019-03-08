package cz.cas.lib.arclib.service.archivalStorage;

import cz.cas.lib.arclib.security.user.UserDetails;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@Service
@Slf4j
public class ArchivalStoragePipe {

    protected String baseEndpoint;
    protected String readKeypair;
    protected String readWriteKeypair;
    protected String adminKeypair;
    protected UserDetails userDetails;

    /**
     * passes simple request to archival storage and fill the response with the archval storage response
     * should be used only for requests which returns JSON body
     *
     * @param response
     * @param request
     * @param path
     * @param httpMethod
     * @param operation
     * @param accessTokenType
     */
    public void passToArchivalStorage(HttpServletResponse response, HttpServletRequest request, String path, HttpMethod httpMethod, String operation, AccessTokenType accessTokenType) {
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
            HttpHeaders requestHeaders = new HttpHeaders();
            String tokenToUse;
            switch (accessTokenType) {
                case READ:
                    tokenToUse = readKeypair;
                    break;
                case READ_WRITE:
                    tokenToUse = readWriteKeypair;
                    break;
                default:
                    tokenToUse = adminKeypair;
            }
            requestHeaders.add("Authorization", "Basic " + tokenToUse);
            requestHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            byte[] requestBody = IOUtils.toByteArray(request.getInputStream());
            HttpEntity<byte[]> requestEntity = new HttpEntity<byte[]>(requestBody, requestHeaders);
            ResponseEntity<byte[]> exchange = restTemplate.exchange(baseEndpoint + path, httpMethod, requestEntity, byte[].class);
            if (exchange.getBody() != null) {
                IOUtils.copy(new ByteArrayInputStream(exchange.getBody()), response.getOutputStream());
            }
            response.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE);
            response.setStatus(exchange.getStatusCodeValue());
            String responseBody = exchange.getBody() == null ? "empty" : IOUtils.toString(exchange.getBody());
            String logMsg = "user: " + userDetails.getUsername() + ", operation: " + operation + " request body: " + IOUtils.toString(requestBody) + "response body: " + responseBody;
            if (exchange.getStatusCode().is2xxSuccessful()) {
                log.info(logMsg);
            } else
                log.error(logMsg);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public enum AccessTokenType {
        ADMIN, READ, READ_WRITE
    }

    @Inject
    public void setBaseEndpoint(@Value("${archivalStorage.api}") String baseEndpoint) {
        this.baseEndpoint = baseEndpoint;
    }

    @Inject
    public void setReadKeypair(@Value("${archivalStorage.authorization.basic.read}") String readKeypair) {
        this.readKeypair = readKeypair;
    }

    @Inject
    public void setReadWriteKeypair(@Value("${archivalStorage.authorization.basic.readWrite}") String readWriteKeypair) {
        this.readWriteKeypair = readWriteKeypair;
    }

    @Inject
    public void setAdminKeypair(@Value("${archivalStorage.authorization.basic.admin}") String adminKeypair) {
        this.adminKeypair = adminKeypair;
    }

    @Inject
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }
}
