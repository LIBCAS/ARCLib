package cz.cas.lib.arclib.service.fixity;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cas.lib.arclib.domain.IngestToolFunction;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static cz.cas.lib.core.util.Utils.notNull;

@Slf4j
@Service
public class MetsFixityChecker extends FixityChecker {

    @Getter
    private String toolName = "ARCLib_mets_" + IngestToolFunction.fixity_check;
    @Getter
    private String toolVersion = null;

    /**
     * Verifies fixity of every file specified in METS filesec.
     * Currently supports MD5, SHA-1, SHA-256 and SHA-512.
     *
     * @param pathToMets path to the main metadata file (METS)
     */
    @Override
    public void verifySIP(Path sipWsPath, Path pathToMets, String externalId,
                          JsonNode configRoot, Map<String, Pair<String, String>> formatIdentificationResult)
            throws IOException, IncidentException {
        log.debug("Verifying fixity of SIP, METS path: " + pathToMets);
        notNull(pathToMets, () -> {
            throw new IllegalArgumentException("Path to mets is null.");
        });
        List<Path> invalidFixities = new ArrayList<>();
        List<Path> missingFiles = new ArrayList<>();
        Map<String, List<Path>> unsupportedChecksumTypes = new HashMap<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document xml;
        try {
            xml = factory.newDocumentBuilder().parse(pathToMets.toString());
        } catch (ParserConfigurationException | SAXException e) {
            throw new GeneralException(e);
        }
        XPath xpath = getMetsXpath();

        NodeList elems;
        try {
            elems = (NodeList) xpath.evaluate("//METS:file", xml, XPathConstants.NODESET);
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
                String filePathStrFromMets = fileLocItem.getAttributes().getNamedItem("xlink:href").getNodeValue();
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
            invokeUnsupportedChecksumTypeIssue(sipWsPath, unsupportedChecksumTypes, externalId, configRoot, formatIdentificationResult);
        if (!missingFiles.isEmpty())
            invokeMissingFilesIssue(sipWsPath, missingFiles, externalId, configRoot, formatIdentificationResult);
        if (!invalidFixities.isEmpty())
            invokeInvalidChecksumsIssue(sipWsPath, invalidFixities, externalId, configRoot, formatIdentificationResult);
    }

    private XPath getMetsXpath() {
        XPath xpath = XPathFactory.newInstance().newXPath();

        xpath.setNamespaceContext(new NamespaceContext() {
            public String getNamespaceURI(String prefix) {
                if (prefix == null) {
                    throw new IllegalArgumentException("No prefix provided!");
                } else if (prefix.equals("METS")) {
                    return "http://www.loc.gov/METS/";
                } else if (prefix.equals("xlink")) {
                    return "http://www.w3.org/1999/xlink";

                } else {
                    return XMLConstants.NULL_NS_URI;
                }
            }

            public String getPrefix(String namespaceURI) {
                // Not needed in this context.
                return null;
            }

            public Iterator getPrefixes(String namespaceURI) {
                // Not needed in this context.
                return null;
            }
        });
        return xpath;
    }
}