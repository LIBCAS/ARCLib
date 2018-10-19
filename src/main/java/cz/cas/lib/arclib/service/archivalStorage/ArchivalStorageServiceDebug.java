package cz.cas.lib.arclib.service.archivalStorage;

import cz.cas.lib.core.exception.GeneralException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
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

    public static final String ARC_STORAGE_DATA = "arcStorageData";
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
        Path sipPath = arcStorageData.resolve(aipId);

        boolean sipExists = sipPath.toFile().exists();
        boolean sipRemoved = arcStorageData.resolve(aipId + ".REMOVED").toFile().exists();
        if (!sipExists || sipRemoved) return null;

        Instant now = Instant.now();
        Path pathToZip = arcStorageData.resolve(aipId + "." + now.getEpochSecond() + ".zip");

        try (ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(pathToZip.toFile())))) {
            zipOut.putNextEntry(new ZipEntry(aipId));
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
                zipOut.putNextEntry(new ZipEntry(String.format("%s_xml_%d", aipId, getXmlVersion(aipId, xml.getName()))));
                InputStream xmlFis = new BufferedInputStream(new FileInputStream(xml));
                IOUtils.copyLarge(new BufferedInputStream(xmlFis), zipOut);
                zipOut.closeEntry();
                IOUtils.closeQuietly(xmlFis);
            }
        }
        log.info("AIP " + aipId + " has been exported from archival storage.");
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
        log.info("ArclibXml of AIP with ID " + aipId + " has been exported from archival storage.");
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
        try {
            FileUtils.copyInputStreamToFile(xmlStream, arcStorageData.resolve(sipId + xmlVersion).toFile());
        } catch (IOException e) {
            throw new GeneralException("Failed storing of version number " + xmlVersion +
                    " of ArclibXml of SIP " + sipId + " to archival storage. " + "Reason: " + e.getMessage());
        }
    }

    /**
     * Physicaly removes SIP from archival storage.
     *
     * @param aipId id of the AIP
     */
    public void delete(String aipId) {
        try {
            Files.delete(arcStorageData.resolve(aipId));
        } catch (IOException e) {
            throw new GeneralException("Deleting of SIP " + aipId + " at archival storage failed. Reason: " + e.getMessage());
        }
        log.info("SIP " + aipId + " has been deleted from archival storage.");
    }

    /**
     * Logically removes SIP from archival storage.
     *
     * @param aipId id of the SIP
     */
    public void remove(String aipId) {
        if (arcStorageData.resolve(aipId).toFile().renameTo(arcStorageData.resolve(aipId + ".REMOVED").toFile())) {
            log.info("SIP " + aipId + " has been removed from archival storage.");
        } else {
            throw new GeneralException("SIP " + aipId + " has failed to be removed from archival storage.");
        }
    }

    /**
     * Renews logically removed AIP.
     *
     * @param aipId id of the AIP
     */
    public void renew(String aipId) {
        if (arcStorageData.resolve(aipId + ".REMOVED").toFile().renameTo(arcStorageData.resolve(aipId).toFile())) {
            log.info("SIP " + aipId + " has been renewed at archival storage.");
        } else {
            throw new GeneralException("SIP " + aipId + " has failed to be renewed at archival storage.");
        }
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

        ObjectState state = isSaved ? ObjectState.ARCHIVED : ObjectState.PROCESSING;
        return state;
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

    @Inject
    public void setWorkspace(@Value("${arclib.path.workspace}") String workspace) throws IOException {
        arcStorageData = Paths.get(workspace).resolve(ARC_STORAGE_DATA);
        Files.createDirectories(arcStorageData);
    }
}
