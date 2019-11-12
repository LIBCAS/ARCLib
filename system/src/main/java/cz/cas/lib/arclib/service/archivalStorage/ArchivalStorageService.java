package cz.cas.lib.arclib.service.archivalStorage;

import cz.cas.lib.arclib.domain.Hash;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class ArchivalStorageService {

    private String baseEndpoint;
    private String readKeypair;
    private String readWriteKeypair;

    /**
     * Exports AIP from archival storage.
     *
     * @param aipId   id of the
     * @param allXmls if true, all ArclibXml versions all retrieved, otherwise only the latest one
     * @return response holding the file containing the exported AIP
     * @throws ArchivalStorageException        in case of I/O errors
     */
    public ArchivalStorageResponse exportSingleAip(String aipId, boolean allXmls) throws ArchivalStorageException {
        if (allXmls) log.debug("Exporting AIP: " + aipId + " with all its XML versions.");
        else log.debug("Exporting AIP: " + aipId + " with the latest XML version.");

        String queryParam = allXmls ? "?all=true" : "";
        HttpComponentsClientHttpRequestFactory f = new HttpComponentsClientHttpRequestFactory();
        f.setBufferRequestBody(false);
        try {
            ClientHttpRequest request = f.createRequest(toUri(baseEndpoint + "/storage/" + aipId + queryParam), HttpMethod.GET);
            request.getHeaders().add("Authorization", "Basic " + readWriteKeypair);
            ClientHttpResponse response = request.execute();
            if (response.getStatusCode().is2xxSuccessful())
                log.info("AIP " + aipId + " has been exported from archival storage.");
            return new ArchivalStorageResponse(response.getBody(), response.getStatusCode());
        } catch (IOException e) {
            throw new ArchivalStorageException(e);
        }
    }

    /**
     * Exports ArclibXml from archival storage.
     *
     * @param aipId   id of the AIP
     * @param version version of the ArclibXml
     * @return response holding the file containing the exported ArclibXml
     * @throws ArchivalStorageException        in case of I/O errors
     */
    public ArchivalStorageResponse exportSingleXml(String aipId, Integer version) throws ArchivalStorageException {
        log.debug("Exporting XML of version: " + version + " of AIP: " + aipId + ".");

        String queryParam = version != null ? "?v=" + version : "";
        HttpComponentsClientHttpRequestFactory f = new HttpComponentsClientHttpRequestFactory();
        f.setBufferRequestBody(false);
        try {
            ClientHttpRequest request = f.createRequest(toUri(baseEndpoint + "/storage/" + aipId + "/xml" + queryParam), HttpMethod.GET);
            request.getHeaders().add("Authorization", "Basic " + readWriteKeypair);
            ClientHttpResponse response = request.execute();
            if (response.getStatusCode().is2xxSuccessful())
                log.info("XML of AIP with ID " + aipId + " has been exported from archival storage.");
            return new ArchivalStorageResponse(response.getBody(), response.getStatusCode());
        } catch (IOException e) {
            throw new ArchivalStorageException(e);
        }
    }

    /**
     * Stores SIP and ArclibXml to archival storage.
     *
     * @param sipId       id of the SIP
     * @param sipStream   stream with the SIP content
     * @param xmlStream   stream with the ArclibXml content
     * @param sipChecksum checksum of the SIP
     * @param xmlChecksum checksum of the ArclibXml
     * @return response with the storage result
     */
    public ResponseEntity<String> storeAip(String sipId, InputStream sipStream, InputStream xmlStream, Hash sipChecksum, Hash xmlChecksum) {
        log.debug("Storing AIP " + sipId + " to archival storage.");

        InputStreamResource sipStreamResource = new MultipartInputStreamFileResource(sipStream, "");
        InputStreamResource xmlStreamResource = new MultipartInputStreamFileResource(xmlStream, "");

        MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        map.add("sip", sipStreamResource);
        map.add("aipXml", xmlStreamResource);
        map.add("sipChecksumValue", sipChecksum.getHashValue());
        map.add("sipChecksumType", (sipChecksum.getHashType().toString()).toUpperCase());
        map.add("aipXmlChecksumValue", xmlChecksum.getHashValue());
        map.add("aipXmlChecksumType", (xmlChecksum.getHashType().toString()).toUpperCase());
        map.add("UUID", sipId);

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.add("Authorization", "Basic " + readWriteKeypair);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, headers);
        return restTemplate.exchange(baseEndpoint + "/storage/save", HttpMethod.POST, requestEntity, String.class);
    }

    /**
     * Stores new version of ArclibXml to archival storage and responds immediately without waiting for archival storage
     * to finish the storage process.
     *
     * @param sipId       id of the SIP
     * @param xmlStream   stream with the ArclibXml content
     * @param xmlChecksum checksum of the ArclibXml
     * @param xmlVersion  version of the ArclibXml
     * @param sync        if 'true' archival storage waits for the storage process to finish before sending a response
     * @return response with the result of the xml update
     */
    public ResponseEntity<String> updateXml(String sipId, InputStream xmlStream, Hash xmlChecksum, int xmlVersion, boolean sync) {
        log.debug("Updating XML of version " + xmlVersion + " of AIP: " + sipId + " at archival storage.");

        InputStreamResource xmlStreamResource = new MultipartInputStreamFileResource(xmlStream, "s");

        MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        map.add("xml", xmlStreamResource);
        map.add("checksumValue", xmlChecksum.getHashValue());
        map.add("checksumType", (xmlChecksum.getHashType().toString()).toUpperCase());
        map.add("v", xmlVersion);
        map.add("sync", sync);

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.add("Authorization", "Basic " + readWriteKeypair);

        Map<String, String> vars = new HashMap<>();
        vars.put("aipId", sipId);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, headers);
        return restTemplate.exchange(baseEndpoint + "/storage/{aipId}/update", HttpMethod.POST, requestEntity, String.class, vars);
    }

    /**
     * Returns the AIP state at the archival storage.
     *
     * @param aipId id of the AIP package
     * @return state of the AIP stored at archival storage
     * @throws ArchivalStorageException        in case of I/O errors
     */
    public ObjectState getAipState(String aipId) throws ArchivalStorageException {
        log.debug("Retrieving AIP state of AIP: " + aipId + " at archival storage.");
        HttpComponentsClientHttpRequestFactory f = new HttpComponentsClientHttpRequestFactory();
        f.setBufferRequestBody(false);
        try {
            ClientHttpRequest request = f.createRequest(toUri(baseEndpoint + "/storage/" + aipId + "/state"), HttpMethod.GET);
            request.getHeaders().add("Authorization", "Basic " + readKeypair);
            ClientHttpResponse response = request.execute();
            String responseBody = IOUtils.toString(response.getBody(), StandardCharsets.UTF_8);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new GeneralException("Error during retrieving state at archival storage of AIP with ID " + aipId +
                        ". Reason: " + responseBody);
            }
            return ObjectState.valueOf(responseBody.replace("\"", ""));
        } catch (IOException e) {
            throw new ArchivalStorageException(e);
        }
    }

    /**
     * Returns the state of the XML at the archival storage.
     *
     * @param aipId      id of the AIP
     * @param xmlVersion xml version
     * @return state of the object stored at archival storage
     * @throws ArchivalStorageException        in case of I/O errors
     */
    public ObjectState getXmlState(String aipId, int xmlVersion) throws ArchivalStorageException {
        log.debug("Retrieving XML state of XML: " + xmlVersion + " of AIP: " + aipId + " at archival storage.");
        HttpComponentsClientHttpRequestFactory f = new HttpComponentsClientHttpRequestFactory();
        f.setBufferRequestBody(false);
        try {
            ClientHttpRequest request = f.createRequest(toUri(baseEndpoint + "/storage/" + aipId + "/xml/" + xmlVersion + "/state"), HttpMethod.GET);
            request.getHeaders().add("Authorization", "Basic " + readKeypair);
            ClientHttpResponse response = request.execute();
            String responseBody = IOUtils.toString(response.getBody(), StandardCharsets.UTF_8);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new GeneralException("Error during retrieving state at archival storage of XML: " + xmlVersion + " of AIP: " + aipId +
                        ". Reason: " + responseBody);
            }
            return ObjectState.valueOf(responseBody.replace("\"", ""));
        } catch (IOException e) {
            throw new ArchivalStorageException(e);
        }
    }

    /**
     * Physically removes SIP from archival storage.
     *
     * @param aipId id of the SIP
     * @return response with the result of the AIP deletion
     * @throws ArchivalStorageException        in case of I/O errors
     */
    public ArchivalStorageResponse delete(String aipId) throws ArchivalStorageException {
        log.debug("Deleting aip " + aipId + " from archival storage.");

        HttpComponentsClientHttpRequestFactory f = new HttpComponentsClientHttpRequestFactory();
        f.setBufferRequestBody(false);
        try {
            ClientHttpRequest request = f.createRequest(toUri(baseEndpoint + "/storage/" + aipId), HttpMethod.DELETE);
            request.getHeaders().add("Authorization", "Basic " + readWriteKeypair);
            ClientHttpResponse res = request.execute();
            return new ArchivalStorageResponse(res.getBody(), res.getStatusCode());
        } catch (IOException e) {
            throw new ArchivalStorageException(e);
        }
    }

    /**
     * Logically removes SIP from archival storage
     *
     * @param aipId id of the SIP
     * @return response with the result of the AIP removal
     * @throws ArchivalStorageException        in case of I/O errors
     */
    public ArchivalStorageResponse remove(String aipId) throws ArchivalStorageException {
        log.debug("Removing aip " + aipId + " from archival storage.");

        HttpComponentsClientHttpRequestFactory f = new HttpComponentsClientHttpRequestFactory();
        f.setBufferRequestBody(false);
        try {
            ClientHttpRequest request = f.createRequest(toUri(baseEndpoint + "/storage/" + aipId + "/remove"), HttpMethod.PUT);
            request.getHeaders().add("Authorization", "Basic " + readWriteKeypair);
            ClientHttpResponse res = request.execute();
            return new ArchivalStorageResponse(res.getBody(), res.getStatusCode());
        } catch (IOException e) {
            throw new ArchivalStorageException(e);
        }
    }

    /**
     * Renews logically removed AIP
     *
     * @param aipId id of the AIP
     * @return response with the result of the AIP renewal
     * @throws ArchivalStorageException        in case of I/O errors
     */
    public ArchivalStorageResponse renew(String aipId) throws ArchivalStorageException {
        log.debug("Renewing aip " + aipId + " at archival storage.");
        HttpComponentsClientHttpRequestFactory f = new HttpComponentsClientHttpRequestFactory();
        f.setBufferRequestBody(false);
        try {
            ClientHttpRequest request = f.createRequest(toUri(baseEndpoint + "/storage/" + aipId + "/renew"), HttpMethod.PUT);
            request.getHeaders().add("Authorization", "Basic " + readWriteKeypair);
            ClientHttpResponse res = request.execute();
            return new ArchivalStorageResponse(res.getBody(), res.getStatusCode());
        } catch (IOException e) {
            throw new ArchivalStorageException(e);
        }
    }

    private URI toUri(String input) {
        try {
            return new URI(input);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("not valid URI: " + input);
        }
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
}
