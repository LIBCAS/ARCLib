package cz.cas.lib.arclib.fixity;

import cz.inqool.uas.exception.GeneralException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.inject.Inject;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static cz.inqool.uas.util.Utils.notNull;

@Slf4j
@Service
public class SipFixityVerifier {

    private Md5FixityCounter md5Counter;

    /**
     * Verifies fixity of files specified in SIP META XML.
     * <p>
     * Currently supports MD5 checksum type. Verification of file with other checksum type will be skipped.
     * </p>
     *
     * @param pathToXmlFile Path to SIP META XML.
     * @return List with paths to files with invalid checksum.
     * @throws IOException
     */
    public List<Path> verifySIP(Path pathToXmlFile) throws IOException {
        log.info("Verifying fixity of SIP, META XML: " + pathToXmlFile);
        notNull(pathToXmlFile, () -> {
            throw new IllegalArgumentException();
        });
        List<Path> invalidChecksumFiles = new ArrayList<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document xml = null;
        try {
            xml = factory.newDocumentBuilder().parse(pathToXmlFile.toString());
        } catch (ParserConfigurationException | SAXException e) {
            throw new GeneralException(e);
        }
        XPath xpath = getMetsXpath();

        NodeList elems = null;
        try {
            String s = "//METS:file";

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
                if (!"METS:FLocat".equals(fileLocItem.getNodeName()))
                    continue;
                String fileRelativePath = fileLocItem.getAttributes().getNamedItem("xlink:href").getNodeValue();
                Path filePath = pathToXmlFile.getParent().resolve(fileRelativePath).normalize().toAbsolutePath();
                if (checksumType.toUpperCase().equals("MD5")) {
                    if (!md5Counter.verifyFixity(filePath, checksum)) {
                        invalidChecksumFiles.add(filePath);
                    }
                } else
                    log.info("Found unsupported checksum type: " + checksumType + ", verificaion of file " + fileRelativePath + " skipped");
            }
        }
        return invalidChecksumFiles;
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

    @Inject
    public void setMd5FixityCounter(Md5FixityCounter counter) {
        this.md5Counter = counter;
    }
}