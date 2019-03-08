package cz.cas.lib.arclib.service.archivalStorage;

import cz.cas.lib.arclib.domain.Hash;
import lombok.extern.slf4j.Slf4j;
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
     * @throws IOException        in case of I/O errors
     * @throws URISyntaxException if the URI format of the request is wrong
     */
    public ClientHttpResponse exportSingleAip(String aipId, boolean allXmls) throws IOException, URISyntaxException {
        if (allXmls) log.debug("Exporting AIP: " + aipId + " with all its XML versions.");
        else log.debug("Exporting AIP: " + aipId + " with the latest XML version.");

        String queryParam = allXmls ? "?all=true" : "";
        HttpComponentsClientHttpRequestFactory f = new HttpComponentsClientHttpRequestFactory();
        f.setBufferRequestBody(false);
        ClientHttpRequest request = f.createRequest(new URI(baseEndpoint + "/storage/" + aipId + queryParam), HttpMethod.GET);
        request.getHeaders().add("Authorization", "Basic " + readWriteKeypair);
        return request.execute();
    }

    /**
     * Exports ArclibXml from archival storage.
     *
     * @param aipId   id of the AIP
     * @param version version of the ArclibXml
     * @return response holding the file containing the exported ArclibXml
     * @throws URISyntaxException if the URI format of the request is wrong
     * @throws IOException        in case of I/O errors
     */
    public ClientHttpResponse exportSingleXml(String aipId, Integer version) throws URISyntaxException, IOException {
        log.debug("Exporting XML of version: " + version + " of AIP: " + aipId + ".");

        String queryParam = version != null ? "?v=" + version : "";
        HttpComponentsClientHttpRequestFactory f = new HttpComponentsClientHttpRequestFactory();
        f.setBufferRequestBody(false);
        ClientHttpRequest request = f.createRequest(new URI(baseEndpoint + "/storage/" + aipId + "/xml" + queryParam), HttpMethod.GET);
        request.getHeaders().add("Authorization", "Basic " + readWriteKeypair);
        return request.execute();
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
     * @throws IOException        in case of I/O errors
     * @throws URISyntaxException if the URI format of the request is wrong
     */
    public ClientHttpResponse getAipState(String aipId) throws IOException, URISyntaxException {
        log.debug("Retrieving AIP state of AIP: " + aipId + " at archival storage.");

        HttpComponentsClientHttpRequestFactory f = new HttpComponentsClientHttpRequestFactory();
        f.setBufferRequestBody(false);
        ClientHttpRequest request = f.createRequest(new URI(baseEndpoint + "/storage/" + aipId + "/state"), HttpMethod.GET);
        request.getHeaders().add("Authorization", "Basic " + readKeypair);

        return request.execute();
    }

    /**
     * Physically removes SIP from archival storage.
     *
     * @param aipId id of the SIP
     * @return response with the result of the AIP deletion
     * @throws IOException        in case of I/O errors
     * @throws URISyntaxException if the URI format of the request is wrong
     */
    public ClientHttpResponse delete(String aipId) throws URISyntaxException, IOException {
        log.debug("Deleting aip " + aipId + " from archival storage.");

        HttpComponentsClientHttpRequestFactory f = new HttpComponentsClientHttpRequestFactory();
        f.setBufferRequestBody(false);
        ClientHttpRequest request = f.createRequest(new URI(baseEndpoint + "/storage/" + aipId), HttpMethod.DELETE);
        request.getHeaders().add("Authorization", "Basic " + readWriteKeypair);

        return request.execute();
    }

    /**
     * Logically removes SIP from archival storage
     *
     * @param aipId id of the SIP
     * @return response with the result of the AIP removal
     * @throws IOException        in case of I/O errors
     * @throws URISyntaxException if the URI format of the request is wrong
     */
    public ClientHttpResponse remove(String aipId) throws IOException, URISyntaxException {
        log.debug("Removing aip " + aipId + " from archival storage.");

        HttpComponentsClientHttpRequestFactory f = new HttpComponentsClientHttpRequestFactory();
        f.setBufferRequestBody(false);
        ClientHttpRequest request = f.createRequest(new URI(baseEndpoint + "/storage/" + aipId + "/remove"), HttpMethod.PUT);
        request.getHeaders().add("Authorization", "Basic " + readWriteKeypair);

        return request.execute();
    }

    /**
     * Renews logically removed AIP
     *
     * @param aipId id of the AIP
     * @return response with the result of the AIP renewal
     * @throws IOException        in case of I/O errors
     * @throws URISyntaxException if the URI format of the request is wrong
     */
    public ClientHttpResponse renew(String aipId) throws URISyntaxException, IOException {
        log.debug("Renewing aip " + aipId + " at archival storage.");

        HttpComponentsClientHttpRequestFactory f = new HttpComponentsClientHttpRequestFactory();
        f.setBufferRequestBody(false);
        ClientHttpRequest request = f.createRequest(new URI(baseEndpoint + "/storage/" + aipId + "/renew"), HttpMethod.PUT);
        request.getHeaders().add("Authorization", "Basic " + readWriteKeypair);

        return request.execute();
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
