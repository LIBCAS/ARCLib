package cz.cas.lib.arclib.service.archivalStorage;

import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
public class ArchivalStorageServiceDebug {

    private Path arcStorageData;

    /**
     * Exports AIP from archival storage.
     *
     * @param aipId   id of the AIP
     * @param allXmls if true, all ArclibXml versions all retrieved, otherwise only the latest one
     * @return zip archive with AIP containing both SIP and all the XMLs, null if the AIP does not exist
     * @throws IOException if an exception occurred during copying to workspace or some of the files with the AIP content
     *                     does not exist
     */
    public InputStream exportSingleAip(String aipId, boolean allXmls) throws IOException {
        log.debug("Exporting AIP " + aipId + " from archival storage.");

        Path sipPath = arcStorageData.resolve(aipId);

        boolean sipExists = sipPath.toFile().exists();
        boolean sipRemoved = arcStorageData.resolve(aipId + ".REMOVED").toFile().exists();
        if (!sipExists || sipRemoved) return null;

        Instant now = Instant.now();
        Path pathToZip = arcStorageData.resolve(aipId + "." + now.getEpochSecond() + ".zip");

        try (ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(pathToZip.toFile())))) {
            zipOut.putNextEntry(new ZipEntry(aipId + ".zip"));
            InputStream sipFis = new BufferedInputStream(new FileInputStream(sipPath.toFile()));
            IOUtils.copyLarge(sipFis, zipOut);
            zipOut.closeEntry();
            IOUtils.closeQuietly(sipFis);

            File[] listOfFiles = arcStorageData.toFile().listFiles();
            List<File> xmls = new ArrayList<>();
            int maxVersion = 0;
            for (int i = 0; i < listOfFiles.length; i++) {
                File f = listOfFiles[i];
                String fileName = f.getName();
                if (fileName.startsWith(aipId) && !fileName.endsWith(".zip")) {
                    Integer xmlVersion = getXmlVersion(aipId, fileName);
                    if (xmlVersion != null) {
                        if (!allXmls) {
                            if (xmlVersion > maxVersion) {
                                xmls.set(0, f);
                            }
                        } else
                            xmls.add(f);
                    }
                }
            }
            for (File xml : xmls) {
                zipOut.putNextEntry(new ZipEntry(String.format("%s_xml_%d.xml", aipId, getXmlVersion(aipId, xml.getName()))));
                InputStream xmlFis = new BufferedInputStream(new FileInputStream(xml));
                IOUtils.copyLarge(new BufferedInputStream(xmlFis), zipOut);
                zipOut.closeEntry();
                IOUtils.closeQuietly(xmlFis);
            }
        }
        return new FileInputStream(pathToZip.toFile());
    }

    /**
     * Exports ArclibXml from archival storage.
     *
     * @param aipId   id of the AIP
     * @param version version of the ArclibXml
     * @return file containing the exported ArclibXml
     * @throws IOException if the file with the ArclibXml content does not exist
     */
    public InputStream exportSingleXml(String aipId, Integer version) throws IOException {
        log.debug("Exporting ArclibXml of AIP with ID " + aipId + " from archival storage.");

        File xmlFile = null;
        if (version == null) {
            int maxVersion = 0;
            File[] listOfFiles = arcStorageData.toFile().listFiles();
            for (int i = 0; i < listOfFiles.length; i++) {
                File f = listOfFiles[i];
                String fileName = f.getName();
                if (fileName.startsWith(aipId) && !fileName.endsWith(".zip")) {
                    Integer xmlVersion = getXmlVersion(aipId, fileName);
                    if (xmlVersion > maxVersion) {
                        xmlFile = listOfFiles[i];
                    }
                }
            }
        } else
            xmlFile = arcStorageData.resolve(aipId + version).toFile();
        if (xmlFile == null || !xmlFile.exists())
            return null;
        return new FileInputStream(xmlFile);
    }

    /**
     * Stores SIP and ArclibXml to archival storage.
     *
     * @param sipId     id of the SIP
     * @param sipStream stream with the SIP content
     * @param xmlStream stream with the ArclibXml content
     */
    public void storeAip(String sipId, InputStream sipStream, InputStream xmlStream) {
        log.debug("Storing AIP with id " + sipId + " to archival storage.");

        try {
            FileUtils.copyInputStreamToFile(sipStream, arcStorageData.resolve(sipId).toFile());
            FileUtils.copyInputStreamToFile(xmlStream, arcStorageData.resolve(sipId + 1).toFile());
        } catch (IOException e) {
            throw new GeneralException("Storing of SIP " + sipId + " to archival storage failed. " +
                    "Reason: " + e.getMessage());
        }
    }

    /**
     * Stores new version of ArclibXml to archival storage.
     *
     * @param sipId      id of the SIP
     * @param xmlStream  stream with the ArclibXml content
     * @param xmlVersion version of the ArclibXml
     */
    public void updateXml(String sipId, InputStream xmlStream, int xmlVersion) {
        log.debug("Updating AIP: " + sipId + " at archival storage.");

        try {
            FileUtils.copyInputStreamToFile(xmlStream, arcStorageData.resolve(sipId + xmlVersion).toFile());
        } catch (IOException e) {
            throw new GeneralException("Failed storing of version number " + xmlVersion +
                    " of ArclibXml of SIP " + sipId + " to archival storage. " + "Reason: " + e.getMessage());
        }
        log.debug("AIP: " + sipId + " successfully updated at archival storage.");
    }

    public void deleteSipAndItsXmlsFromWorkspace(String sipId) throws IOException {
        Files.list(arcStorageData).filter(f -> FilenameUtils.getName(f.toString()).startsWith(sipId)).forEach(f ->
        {
            try {
                Files.deleteIfExists(f);
            } catch (IOException e) {
                throw new UncheckedIOException("workspace cleanup failed during deletion of file: " + f.toString(), e);
            }
        });
    }

    /**
     * Returns the AIP state at the archival storage.
     *
     * @param aipId id of the AIP
     * @return state of the AIP stored at archival storage
     */
    public ObjectState getAipState(String aipId) {
        File sip = new File(arcStorageData.resolve(aipId).toFile().toString());
        File xml = new File(arcStorageData.resolve(aipId + 1).toString());
        Boolean isSaved = sip.exists() && xml.exists();

        return isSaved ? ObjectState.ARCHIVED : ObjectState.PROCESSING;
    }

    /**
     * Returns the AIP XML state at the archival storage.
     *
     * @param aipId      id of the AIP
     * @param xmlVersion XML version
     * @return state of the AIP XML stored at archival storage
     */
    public ObjectState getXmlState(String aipId, int xmlVersion) {
        File xml = new File(arcStorageData.resolve(aipId + xmlVersion).toString());
        return xml.exists() ? ObjectState.ARCHIVED : ObjectState.PROCESSING;
    }

    /**
     * Parses version number from the AIP filename.
     *
     * @param aipId    id of the AIP
     * @param fileName filename of SIP
     * @return parsed version number
     */
    private Integer getXmlVersion(String aipId, String fileName) {
        String xmlVersionString = fileName.substring(aipId.length());
        try {
            return Integer.parseInt(xmlVersionString);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Autowired
    public void setWorkspace(@Value("${arclib.path.workspace}") String workspace, @Value("${archivalStorage.debugLocation}") String debugLocation) throws IOException {
        arcStorageData = Paths.get(workspace).resolve(debugLocation);
        Files.createDirectories(arcStorageData);
    }
}
