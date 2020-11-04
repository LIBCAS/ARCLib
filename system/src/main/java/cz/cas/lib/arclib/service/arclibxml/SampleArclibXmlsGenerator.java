package cz.cas.lib.arclib.service.arclibxml;

import cz.cas.lib.arclib.exception.validation.MissingNode;
import cz.cas.lib.arclib.service.fixity.Sha512Counter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.SystemUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.XPath;
import org.dom4j.io.SAXReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static cz.cas.lib.arclib.utils.ArclibUtils.*;
import static cz.cas.lib.core.util.Utils.bytesToHexString;
import static cz.cas.lib.core.util.Utils.executeProcessDefaultResultHandle;

@Slf4j
@Service
public class SampleArclibXmlsGenerator {
    public static final Path SAMPLE_DATA_FOLDER = Paths.get("system/src/main/resources/sampleData");

    private static final String REGEX = "([a-z0-9]*)";
    private static final String SHA_512_EXTENSION = ".SHA512";
    private static final Path INIT_SQL_PATH = Paths.get("system/src/main/resources/sql/arclibInit.sql");

    private static final Path SAMPLE_ARCLIB_XML_PATH_1 = Paths.get("4b/66/65/4b66655a-819a-474f-8203-6c432815df1f_xml_1");
    private static final Path SAMPLE_ARCLIB_XML_PATH_2 = Paths.get("4b/66/65/4b66655a-819a-474f-8203-6c432815df1f_xml_2");
    private static final Path SAMPLE_ARCLIB_XML_PATH_3 = Paths.get("8b/2e/fa/8b2efafd-b637-4b97-a8f7-1b97dd4ee622_xml_1");
    private static final Path SAMPLE_ARCLIB_XML_PATH_4 = Paths.get("8b/2e/fa/8b2efafd-b637-4b97-a8f7-1b97dd4ee622_xml_2");
    private static final Path SAMPLE_ARCLIB_XML_PATH_5 = Paths.get("89/f8/2d/89f82da0-af78-4461-bf92-7382050082a1_xml_1");

    private static final String ARCLIB_XML_DB_RECORD_1 = "'71571b15-53db-46cc-be84-9498a0cff8a3', '2018-03-08 08:00:00', '2018-03-08 08:00:00', null, '";
    private static final String ARCLIB_XML_DB_RECORD_2 = "'6748c4a4-0840-4bfb-8d3c-50df1882f666', '2018-03-08 08:00:00', '2018-03-08 08:00:00', null, '";
    private static final String ARCLIB_XML_DB_RECORD_3 = "'e4fe1dbf-fdb2-440d-89aa-356df3fe794a', '2018-03-08 08:00:00', '2018-03-08 08:00:00', null, '";
    private static final String ARCLIB_XML_DB_RECORD_4 = "'88f510a8-f002-4e20-b7f6-3cbaeaf28b00', '2018-03-08 08:00:00', '2018-03-08 08:00:00', null, '";
    private static final String ARCLIB_XML_DB_RECORD_5 = "'ee0cb888-2b04-48e0-b8bf-4befba5f8dc9', '2018-03-08 08:00:00', '2018-03-08 08:00:00', null, '";

    private String sourceXml;

    private Sha512Counter sha512Counter;
    private Map<String, String> uris;

    /**
     * Updates sample data at archival storage
     *
     * @param pathToSampleData          path to sample data
     * @param pathToArchivalStorageData path to data folder at archival storage
     */
    public static void updateSampleDataAtArchivalStorage(String pathToSampleData, String pathToArchivalStorageData) {
        if (SystemUtils.IS_OS_WINDOWS) {
            //copy new sample data
            executeProcessDefaultResultHandle("cmd", "/c", "xcopy", pathToSampleData, pathToArchivalStorageData, "/s", "/e", "/h", "/y");
        } else {
            //copy new sample data
            executeProcessDefaultResultHandle("/bin/bash", "-c", "cp -r " + pathToSampleData + "/* " + pathToArchivalStorageData);
            //add privileges to the created folder
            executeProcessDefaultResultHandle("/bin/bash", "-c", "chmod -R 755 " + pathToArchivalStorageData);
        }
    }

    /**
     * Deletes and recreates the data folder at archival storage
     *
     * @param pathToArchivalStorageData path to data folder at archival storage
     */
    public static void cleanUpDataAtArchivalStorage(String pathToArchivalStorageData) {
        if (SystemUtils.IS_OS_WINDOWS) {
            //delete original folder
            executeProcessDefaultResultHandle("cmd", "/c", "rmdir", "/s", "/q", pathToArchivalStorageData);
            //recreate the folder
            executeProcessDefaultResultHandle("cmd", "/c", "mkdir", pathToArchivalStorageData);
        } else {
            //delete original folder
            executeProcessDefaultResultHandle("/bin/bash", "-c", "rm -rf " + pathToArchivalStorageData + "/*");
            //recreate the folder
            executeProcessDefaultResultHandle("/bin/bash", "-c", "mkdir " + pathToArchivalStorageData);
            //add privileges to the created folder
            executeProcessDefaultResultHandle("/bin/bash", "-c", "chmod -R 755 " + pathToArchivalStorageData);
        }
    }

    /**
     * Generates a group of sample ArclibXmls based on the given ArclibXml
     *
     * @param pathToSourceArclibXml path to the source ArclibXml
     * @throws DocumentException
     * @throws IOException
     */
    @Transactional
    public void generateSampleXmls(String pathToSourceArclibXml) throws DocumentException, IOException {
        log.info("Generating sample ArclibXml data.");

        byte[] sourceArclibXml = Files.readAllBytes(Paths.get(pathToSourceArclibXml));
        sourceXml = new String(sourceArclibXml, StandardCharsets.UTF_8);

        String initSql = new String(Files.readAllBytes(INIT_SQL_PATH), StandardCharsets.UTF_8);

        initSql = updateArclibXml("ARCLIB_900000003", "4b66655a-819a-474f-8203-6c432815df1f",
                "authorialId3", 1, 1, "initial version", "initial version",
                SAMPLE_ARCLIB_XML_PATH_1, initSql, ARCLIB_XML_DB_RECORD_1);
        initSql = updateArclibXml("ARCLIB_900000004", "4b66655a-819a-474f-8203-6c432815df1f",
                "authorialId3", 1, 2, "initial version", "ARCLIB_900000003",
                SAMPLE_ARCLIB_XML_PATH_2, initSql, ARCLIB_XML_DB_RECORD_2);
        initSql = updateArclibXml("ARCLIB_900000005", "8b2efafd-b637-4b97-a8f7-1b97dd4ee622",
                "authorialId3", 2, 1, "4b66655a-819a-474f-8203-6c432815df1f", "initial version",
                SAMPLE_ARCLIB_XML_PATH_3, initSql, ARCLIB_XML_DB_RECORD_3);
        initSql = updateArclibXml("ARCLIB_900000006", "8b2efafd-b637-4b97-a8f7-1b97dd4ee622",
                "authorialId3", 2, 2, "4b66655a-819a-474f-8203-6c432815df1f", "ARCLIB_900000005",
                SAMPLE_ARCLIB_XML_PATH_4, initSql, ARCLIB_XML_DB_RECORD_4);
        initSql = updateArclibXml("ARCLIB_900000007", "89f82da0-af78-4461-bf92-7382050082a1",
                "authorialId4", 1, 1, "initial version", "initial version",
                SAMPLE_ARCLIB_XML_PATH_5, initSql, ARCLIB_XML_DB_RECORD_5);

        Files.write(INIT_SQL_PATH, initSql.getBytes());
        log.debug("Initialisation SQL file updated with new ARCLibXml hashes.");
    }

    private String updateArclibXml(String xmlId, String sipId, String authorialId, int sipVersionNumber,
                                   int xmlVersionNumber, String sipVersionOf, String xmlVersionOf, Path sampleArclibXmlPath,
                                   String initSql, String arclibXmlDbRecord) throws IOException, DocumentException {
        String xml = generateArclibXml(xmlId, sipId, authorialId, sipVersionNumber, xmlVersionNumber, sipVersionOf, xmlVersionOf);

        //update XML in sampleData folder
        Files.write(SAMPLE_DATA_FOLDER.resolve(sampleArclibXmlPath), xml.getBytes());
        log.debug("Sample ArclibXml successfully written to sample data folder.");

        //compute SHA512 hash
        String hash = bytesToHexString(sha512Counter.computeDigest(new ByteArrayInputStream(xml.getBytes())));

        //update hash in sampleData folder
        Files.write(SAMPLE_DATA_FOLDER.resolve(Paths.get(sampleArclibXmlPath.toString() + SHA_512_EXTENSION)), hash.getBytes());

        //update hash in init.sql
        initSql = initSql.replaceFirst(arclibXmlDbRecord + REGEX, arclibXmlDbRecord + hash);
        return initSql;
    }

    private String generateArclibXml(String xmlId, String sipId, String authorialId, int sipVersionNumber,
                                     int xmlVersionNumber, String sipVersionOf, String xmlVersionOf) throws DocumentException, IOException {
        SAXReader reader = new SAXReader();
        Document doc = reader.read(new ByteArrayInputStream(sourceXml.getBytes(StandardCharsets.UTF_8)));

        //update 'XML id'
        XPath mestHdrPath = doc.createXPath("/METS:mets/METS:metsHdr");
        Element metsHdrElement = (Element) mestHdrPath.selectSingleNode(doc);
        if (metsHdrElement == null) throw new MissingNode(mestHdrPath.getText());
        metsHdrElement.addAttribute("ID", xmlId);

        //update 'SIP id'
        XPath metsPath = doc.createXPath("/METS:mets");
        Element metsElement = (Element) metsPath.selectSingleNode(doc);
        if (metsElement == null) throw new MissingNode(metsPath.getText());
        metsElement.addAttribute("OBJID", sipId);

        //update 'Authorial id'
        Element sipIdentifierElement = metsHdrElement.addElement("METS:altRecordID");
        sipIdentifierElement.addText(authorialId);

        //update 'xml version number'
        XPath xmlVersionNumberPath = doc.createXPath(
                "/mets:mets/mets:amdSec/mets:digiprovMD[@ID='ARCLIB_SIP_INFO']/mets:mdWrap/mets:xmlData/arclib:sipInfo/arclib:xmlVersionNumber");
        xmlVersionNumberPath.setNamespaceURIs(uris);
        Element xmlVersionNumberElement = (Element) xmlVersionNumberPath.selectSingleNode(doc);
        if (xmlVersionNumberElement == null) throw new MissingNode(xmlVersionNumberPath.getText());
        xmlVersionNumberElement.setText(String.valueOf(xmlVersionNumber));

        //update 'xml version of'
        XPath xmlVersionOfPath = doc.createXPath(
                "/mets:mets/mets:amdSec/mets:digiprovMD[@ID='ARCLIB_SIP_INFO']/mets:mdWrap/mets:xmlData/arclib:sipInfo/arclib:xmlVersionOf");
        xmlVersionOfPath.setNamespaceURIs(uris);
        Element xmlVersionOfElement = (Element) xmlVersionOfPath.selectSingleNode(doc);
        if (xmlVersionOfElement == null) throw new MissingNode(xmlVersionOfPath.getText());
        xmlVersionOfElement.setText(xmlVersionOf);

        //update 'sip version number'
        XPath sipVersionNumberPath = doc.createXPath(
                "/mets:mets/mets:amdSec/mets:digiprovMD[@ID='ARCLIB_SIP_INFO']/mets:mdWrap/mets:xmlData/arclib:sipInfo/arclib:sipVersionNumber");
        sipVersionNumberPath.setNamespaceURIs(uris);
        Element sipVersionNumberElement = (Element) sipVersionNumberPath.selectSingleNode(doc);
        if (sipVersionNumberElement == null) throw new MissingNode(sipVersionNumberPath.getText());
        sipVersionNumberElement.setText(String.valueOf(sipVersionNumber));

        //update 'sip version of'
        XPath sipVersionOfPath = doc.createXPath(
                "/mets:mets/mets:amdSec/mets:digiprovMD[@ID='ARCLIB_SIP_INFO']/mets:mdWrap/mets:xmlData/arclib:sipInfo/arclib:sipVersionOf");
        sipVersionOfPath.setNamespaceURIs(uris);
        Element sipVersionOfElement = (Element) sipVersionOfPath.selectSingleNode(doc);
        if (sipVersionOfElement == null) throw new MissingNode(sipVersionOfPath.getText());
        sipVersionOfElement.setText(sipVersionOf);

        return prettyPrint(doc);
    }

    @Inject
    public void setSha512Counter(Sha512Counter sha512Counter) {
        this.sha512Counter = sha512Counter;
    }

    @Inject
    public void setUris(@Value("${namespaces.mets}") String mets, @Value("${namespaces.xsi}") String xsi, @Value("${namespaces.arclib}") String arclib, @Value("${namespaces" +
            ".premis}") String premis, @Value("${namespaces.oai_dc}") String oai_dc, @Value("${namespaces.dc}") String dc) {
        Map<String, String> uris = new HashMap<>();
        uris.put(METS, mets);
        uris.put(ARCLIB, arclib);
        uris.put(PREMIS, premis);
        uris.put(XSI, xsi);
        uris.put(OAIS_DC, oai_dc);
        uris.put(DC, dc);

        this.uris = uris;
    }
}
