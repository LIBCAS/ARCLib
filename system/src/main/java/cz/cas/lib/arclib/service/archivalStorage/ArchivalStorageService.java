package cz.cas.lib.arclib.service.archivalStorage;

import cz.cas.lib.arclib.domain.Hash;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
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
     * Exports AIP data as a .zip
     *
     * @param aipId   id of the AIP package to export
     * @param allXmls true to return all XMLs, otherwise only the latest is returned
     * @return response response from the archival storage
     * @throws ArchivalStorageException in case of any connection error or response with status code other than 2xx
     */
    public InputStream exportSingleAip(String aipId, boolean allXmls) throws ArchivalStorageException {
        if (allXmls) log.debug("Exporting AIP: " + aipId + " with all its XML versions.");
        else log.debug("Exporting AIP: " + aipId + " with the latest XML version.");

        String queryParam = allXmls ? "?all=true" : "";
        HttpComponentsClientHttpRequestFactory f = new HttpComponentsClientHttpRequestFactory();
        f.setBufferRequestBody(false);
        ClientHttpRequest request;
        try {
            request = f.createRequest(toUri(baseEndpoint + "/storage/" + aipId + queryParam), HttpMethod.GET);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        request.getHeaders().add("Authorization", "Basic " + readWriteKeypair);
        return executeRequestVerifySuccess(request);
    }

    /**
     * Exports specified XML
     *
     * @param aipId   id of the AIP package
     * @param version version number of XML, if not set the latest version is returned
     * @return response from the archival storage
     * @throws ArchivalStorageException in case of any connection error or response with status code other than 2xx
     */
    public InputStream exportSingleXml(String aipId, Integer version) throws ArchivalStorageException {
        log.debug("Exporting XML of version: " + version + " of AIP: " + aipId + ".");
        String queryParam = version != null ? "?v=" + version : "";
        HttpComponentsClientHttpRequestFactory f = new HttpComponentsClientHttpRequestFactory();
        f.setBufferRequestBody(false);
        ClientHttpRequest request;
        try {
            request = f.createRequest(toUri(baseEndpoint + "/storage/" + aipId + "/xml" + queryParam), HttpMethod.GET);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        request.getHeaders().add("Authorization", "Basic " + readWriteKeypair);
        return executeRequestVerifySuccess(request);
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
     * @throws ArchivalStorageException in case of any connection error or response with status code other than 2xx
     */
    public InputStream storeAip(String sipId, InputStream sipStream, InputStream xmlStream, Hash sipChecksum, Hash xmlChecksum) throws ArchivalStorageException {
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
        SimpleClientHttpRequestFactory fact = new SimpleClientHttpRequestFactory();
        fact.setBufferRequestBody(false);
        restTemplate.setRequestFactory(fact);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.add("Authorization", "Basic " + readWriteKeypair);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, headers);
        ArchivalStorageResponse response;
        try {
            response = restTemplate.execute(baseEndpoint + "/storage/save",
                    HttpMethod.POST,
                    restTemplate.httpEntityCallback(requestEntity),
                    res -> new ArchivalStorageResponse(res.getBody(), res.getStatusCode()));
            if (response != null && response.getStatusCode().is2xxSuccessful())
                return response.getBody();
        } catch (Exception e) {
            throw new ArchivalStorageException(e);
        }
        throw new ArchivalStorageException(response);
    }

    /**
     * Stores new version of AIP XML to Archival Storage.
     *
     * @param sipId       id of the SIP
     * @param xmlStream   stream with the ArclibXml content
     * @param xmlChecksum checksum of the ArclibXml
     * @param xmlVersion  version of the ArclibXml
     * @param sync        If <b>true</b>, Archival Storage waits for the storage process to finish before sending a response.
     *                    Otherwise the storage process at Archival Storage is asynchronous and the response is returned
     *                    immediately after some initial checks.
     * @return response with the result of the xml update
     * @throws ArchivalStorageException in case of any connection error or response with status code other than 2xx
     */
    public InputStream updateXml(String sipId, InputStream xmlStream, Hash xmlChecksum, int xmlVersion, boolean sync) throws ArchivalStorageException {
        log.debug("Updating XML of version " + xmlVersion + " of AIP: " + sipId + " at archival storage.");

        InputStreamResource xmlStreamResource = new MultipartInputStreamFileResource(xmlStream, "s");

        MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        map.add("xml", xmlStreamResource);
        map.add("checksumValue", xmlChecksum.getHashValue());
        map.add("checksumType", (xmlChecksum.getHashType().toString()).toUpperCase());
        map.add("v", xmlVersion);
        map.add("sync", sync);

        RestTemplate restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory fact = new SimpleClientHttpRequestFactory();
        fact.setBufferRequestBody(false);
        restTemplate.setRequestFactory(fact);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.add("Authorization", "Basic " + readWriteKeypair);

        Map<String, String> vars = new HashMap<>();
        vars.put("aipId", sipId);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, headers);
        ArchivalStorageResponse response;
        try {
            response = restTemplate.execute(baseEndpoint + "/storage/{aipId}/update",
                    HttpMethod.POST,
                    restTemplate.httpEntityCallback(requestEntity),
                    res -> new ArchivalStorageResponse(res.getBody(), res.getStatusCode()),
                    vars);
            if (response != null && response.getStatusCode().is2xxSuccessful())
                return response.getBody();
        } catch (Exception e) {
            throw new ArchivalStorageException(e);
        }
        throw new ArchivalStorageException(response);
    }

    /**
     * Returns the AIP state at the archival storage.
     *
     * @param aipId id of the AIP package
     * @return state of the AIP stored at archival storage
     * @throws ArchivalStorageException in case of any connection error or response with status code other than 2xx
     */
    public ObjectState getAipState(String aipId) throws ArchivalStorageException {
        log.debug("Retrieving AIP state of AIP: " + aipId + " at archival storage.");
        try {
            ClientHttpRequest request = new HttpComponentsClientHttpRequestFactory().createRequest(toUri(baseEndpoint + "/storage/" + aipId + "/state"), HttpMethod.GET);
            request.getHeaders().add("Authorization", "Basic " + readKeypair);
            InputStream response = executeRequestVerifySuccess(request);
            String responseString = IOUtils.toString(response, StandardCharsets.UTF_8);
            return ObjectState.valueOf(responseString.replace("\"", ""));
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
     * @throws ArchivalStorageException in case of any connection error or response with status code other than 2xx
     */
    public ObjectState getXmlState(String aipId, int xmlVersion) throws ArchivalStorageException {
        log.debug("Retrieving XML state of XML: " + xmlVersion + " of AIP: " + aipId + " at archival storage.");
        try {
            ClientHttpRequest request = new HttpComponentsClientHttpRequestFactory().createRequest(toUri(baseEndpoint + "/storage/" + aipId + "/xml/" + xmlVersion + "/state"), HttpMethod.GET);
            request.getHeaders().add("Authorization", "Basic " + readKeypair);
            InputStream response = executeRequestVerifySuccess(request);
            String responseString = IOUtils.toString(response, StandardCharsets.UTF_8);
            return ObjectState.valueOf(responseString.replace("\"", ""));
        } catch (IOException e) {
            throw new ArchivalStorageException(e);
        }
    }

    /**
     * Physically removes SIP from archival storage.
     *
     * @param aipId id of the SIP
     * @return response with the result of the AIP deletion
     * @throws ArchivalStorageException in case of any connection error or response with status code other than 2xx
     */
    public InputStream delete(String aipId) throws ArchivalStorageException {
        log.debug("Deleting aip " + aipId + " from archival storage.");
        ClientHttpRequest request;
        try {
            request = new HttpComponentsClientHttpRequestFactory().createRequest(toUri(baseEndpoint + "/storage/" + aipId), HttpMethod.DELETE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        request.getHeaders().add("Authorization", "Basic " + readWriteKeypair);
        return executeRequestVerifySuccess(request);
    }

    /**
     * Logically removes SIP from archival storage
     *
     * @param aipId id of the SIP
     * @return response with the result of the AIP removal
     * @throws ArchivalStorageException in case of any connection error or response with status code other than 2xx
     */
    public InputStream remove(String aipId) throws ArchivalStorageException {
        log.debug("Removing aip " + aipId + " from archival storage.");
        ClientHttpRequest request;
        try {
            request = new HttpComponentsClientHttpRequestFactory().createRequest(toUri(baseEndpoint + "/storage/" + aipId + "/remove"), HttpMethod.PUT);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        request.getHeaders().add("Authorization", "Basic " + readWriteKeypair);
        return executeRequestVerifySuccess(request);
    }

    /**
     * Rolls back AIP from archival storage
     *
     * @param aipId id of the AIP
     * @return response with the result of the AIP rollback
     * @throws ArchivalStorageException in case of any connection error or response with status code other than 2xx
     */
    public InputStream rollbackAip(String aipId) throws ArchivalStorageException {
        log.debug("Rolling back AIP " + aipId + " from archival storage.");
        ClientHttpRequest request;
        try {
            request = new HttpComponentsClientHttpRequestFactory().createRequest(toUri(baseEndpoint + "/storage/" + aipId + "/rollback"), HttpMethod.DELETE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        request.getHeaders().add("Authorization", "Basic " + readWriteKeypair);
        return executeRequestVerifySuccess(request);
    }

    /**
     * Rolls back latest AIP XML of specified AIP from archival storage
     *
     * @param aipId id of the AIP
     * @return response with the result of the AIP XML rollback
     * @throws ArchivalStorageException in case of any connection error or response with status code other than 2xx
     */
    public InputStream rollbackLatestXml(String aipId, int xmlVersion) throws ArchivalStorageException {
        log.debug("Rolling back latest AIP XML of AIP: " + aipId + " from archival storage.");
        ClientHttpRequest request;
        try {
            request = new HttpComponentsClientHttpRequestFactory().createRequest(toUri(baseEndpoint + "/storage/" + aipId + "/rollbackXml/" + xmlVersion), HttpMethod.DELETE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        request.getHeaders().add("Authorization", "Basic " + readWriteKeypair);
        return executeRequestVerifySuccess(request);
    }

    /**
     * Renews logically removed AIP
     *
     * @param aipId id of the AIP
     * @return response with the result of the AIP renewal
     * @throws ArchivalStorageException in case of any connection error or response with status code other than 2xx
     */
    public InputStream renew(String aipId) throws ArchivalStorageException {
        log.debug("Renewing aip " + aipId + " at archival storage.");
        ClientHttpRequest request;
        try {
            request = new HttpComponentsClientHttpRequestFactory().createRequest(toUri(baseEndpoint + "/storage/" + aipId + "/renew"), HttpMethod.PUT);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        request.getHeaders().add("Authorization", "Basic " + readWriteKeypair);
        return executeRequestVerifySuccess(request);
    }

    private InputStream executeRequestVerifySuccess(ClientHttpRequest req) throws ArchivalStorageException {
        ArchivalStorageResponse response;
        try {
            ClientHttpResponse res = req.execute();
            response = new ArchivalStorageResponse(res.getBody(), res.getStatusCode());
            if (response.getStatusCode().is2xxSuccessful())
                return response.getBody();
        } catch (Exception e) {
            throw new ArchivalStorageException(e);
        }
        throw new ArchivalStorageException(response);
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
