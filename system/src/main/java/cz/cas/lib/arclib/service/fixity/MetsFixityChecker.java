package cz.cas.lib.arclib.service.fixity;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cas.lib.arclib.bpm.IngestTool;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cz.cas.lib.arclib.utils.ArclibUtils.*;
import static cz.cas.lib.arclib.utils.XmlUtils.createDomAndXpath;
import static cz.cas.lib.core.util.Utils.notNull;

@Slf4j
@Service
public class MetsFixityChecker extends FixityChecker {

    private Map<String, String> uris;

    /**
     * Verifies fixity of every file specified in METS filesec.
     * Currently supports MD5, SHA-1, SHA-256 and SHA-512.
     *
     * @param pathToMets path to the main metadata file (METS)
     */
    @Override
    public void verifySIP(Path sipWsPath, Path pathToMets, String externalId,
                          JsonNode configRoot, Map<String, Pair<String, String>> formatIdentificationResult, int fixityCheckToolCounter, IngestTool fixityCheckerTool)
            throws IOException, IncidentException {
        log.debug("Verifying fixity of SIP, METS path: " + pathToMets);
        notNull(pathToMets, () -> {
            throw new IllegalArgumentException("Path to mets is null.");
        });

        Map<Path, List<Path>> invalidFixitiesWrapper = new HashMap<>();
        List<Path> invalidFixities = new ArrayList<>();
        invalidFixitiesWrapper.put(pathToMets, invalidFixities);

        Map<Path, List<Path>> missingFilesWrapper = new HashMap<>();
        List<Path> missingFiles = new ArrayList<>();
        missingFilesWrapper.put(pathToMets, missingFiles);

        Map<Path, Map<String, List<Path>>> unsupportedChecksumTypesWrapper = new HashMap<>();
        Map<String, List<Path>> unsupportedChecksumTypes = new HashMap<>();
        unsupportedChecksumTypesWrapper.put(pathToMets, unsupportedChecksumTypes);

        Pair<Document, XPath> domAndXpath;
        try (FileInputStream fis = new FileInputStream(pathToMets.toFile())) {
            domAndXpath = createDomAndXpath(fis, uris);
        } catch (SAXException | ParserConfigurationException e) {
            throw new GeneralException(e);
        }
        Document xmlDom = domAndXpath.getKey();
        XPath xpath = domAndXpath.getValue();

        NodeList elems;
        try {
            elems = (NodeList) xpath.evaluate("//METS:file", xmlDom, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new GeneralException(e);
        }

        for (int i = 0; i < elems.getLength(); i++) {
            String checksumType = elems.item(i).getAttributes().getNamedItem("CHECKSUMTYPE").getNodeValue();
            String checksum = elems.item(i).getAttributes().getNamedItem("CHECKSUM").getNodeValue();
            NodeList fileLocations = elems.item(i).getChildNodes();
            for (int j = 0; j < fileLocations.getLength(); j++) {
                Node fileLocItem = fileLocations.item(j);
                if (!"METS:FLocat".equalsIgnoreCase(fileLocItem.getNodeName()))
                    continue;
                String filePathStrFromMets = fileLocItem.getAttributes().getNamedItem(XLINK + ":href").getNodeValue();
                Path filePathInWs = filePathStrFromMets.startsWith("/")
                        ? sipWsPath.resolve(filePathStrFromMets).normalize().toAbsolutePath()
                        : pathToMets.getParent().resolve(filePathStrFromMets).normalize().toAbsolutePath();

                FixityCounter counter;
                switch (checksumType.toUpperCase()) {
                    case "MD5":
                        counter = md5Counter;
                        break;
                    case "SHA-512":
                        counter = sha512Counter;
                        break;
                    case "SHA-256":
                        counter = sha256Counter;
                        break;
                    case "SHA-1":
                        counter = sha1Counter;
                        break;
                    default:
                        boolean present = unsupportedChecksumTypes.containsKey(checksumType);
                        if (present)
                            unsupportedChecksumTypes.get(checksumType).add(filePathInWs);
                        else {
                            List<Path> paths = new ArrayList<>();
                            paths.add(filePathInWs);
                            unsupportedChecksumTypes.put(checksumType, paths);
                        }
                        continue;
                }
                if (!filePathInWs.toFile().isFile()) {
                    missingFiles.add(filePathInWs);
                    continue;
                }
                byte[] computedChecksum = counter.computeDigest(filePathInWs);
                if (!counter.checkIfDigestsMatches(checksum, computedChecksum)) {
                    invalidFixities.add(filePathInWs);
                }
            }
        }
        if (!unsupportedChecksumTypes.isEmpty())
            invokeUnsupportedChecksumTypeIssue(sipWsPath, unsupportedChecksumTypesWrapper, externalId, configRoot, formatIdentificationResult, fixityCheckToolCounter, fixityCheckerTool);
        if (!missingFiles.isEmpty())
            invokeMissingFilesIssue(sipWsPath, missingFilesWrapper, externalId, configRoot, fixityCheckToolCounter, fixityCheckerTool);
        if (!invalidFixities.isEmpty())
            invokeInvalidChecksumsIssue(sipWsPath, invalidFixitiesWrapper, externalId, configRoot, formatIdentificationResult, fixityCheckToolCounter, fixityCheckerTool);
    }

    /**
     * Namespace URIs must match URIs in ARCLib XML file.
     */
    @Inject
    public void setUris(@Value("${namespaces.mets}") String mets, @Value("${namespaces.xsi}") String xsi, @Value("${namespaces.arclib}") String arclib, @Value("${namespaces" +
            ".premis}") String premis, @Value("${namespaces.oai_dc}") String oai_dc, @Value("${namespaces.dc}") String dc, @Value("${namespaces.xlink}") String xlink) {
        Map<String, String> uris = new HashMap<>();
        uris.put(METS, mets);
        uris.put(ARCLIB, arclib);
        uris.put(PREMIS, premis);
        uris.put(XSI, xsi);
        uris.put(OAIS_DC, oai_dc);
        uris.put(DC, dc);
        uris.put(XLINK, xlink);

        this.uris = uris;
    }
}