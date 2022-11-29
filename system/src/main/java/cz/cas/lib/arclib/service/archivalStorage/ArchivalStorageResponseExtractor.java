package cz.cas.lib.arclib.service.archivalStorage;

import cz.cas.lib.arclib.domain.export.DataReduction;
import cz.cas.lib.arclib.utils.ZipUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class ArchivalStorageResponseExtractor {

    /**
     * Extracts response of {@link ArchivalStorageService#exportSingleAip(String, boolean, DataReduction)}
     * into workspace.
     * <p>
     * Example input:
     *     <ul>
     *         <li>outerZipStream: ZIP inputstream with entries [7f7a3394-4a45-474c-9356-3aef3bbba7c8.zip, 7f7a3394-4a45-474c-9356-3aef3bbba7c8_xml_1.xml, 7f7a3394-4a45-474c-9356-3aef3bbba7c8_xml_2.xml] where 7f7a3394-4a45-474c-9356-3aef3bbba7c8.zip contains entry [7f7a3394-4a45-474c-9356-3aef3bbba7c8/aipDir]</li>
     *         <li>aipId: </li> 7f7a3394-4a45-474c-9356-3aef3bbba7c8
     *         <li>targetFolder: /some</li>
     *     </ul>
     * </p>
     * <p>
     *     Example output:
     *     <ul>
     *         <li>/some/7f7a3394-4a45-474c-9356-3aef3bbba7c8/7f7a3394-4a45-474c-9356-3aef3bbba7c8_xml_1.xml</li>
     *         <li>/some/7f7a3394-4a45-474c-9356-3aef3bbba7c8/7f7a3394-4a45-474c-9356-3aef3bbba7c8_xml_2.xml</li>
     *         <li>/some/7f7a3394-4a45-474c-9356-3aef3bbba7c8/aipDir</li>
     *     </ul>
     * </p>
     *
     * @param outerZipStream response for archival storage
     * @param targetFolder   folder to which the data are extracted
     * @return path to the unpacked root AIP data directory
     * @throws IOException
     */
    public Path extractAipAsFolderWithXmlsBySide(ZipInputStream outerZipStream, String aipId, Path targetFolder) throws IOException {
        Path aipExportFolder = targetFolder.resolve(aipId);
        ZipEntry outerZipEntry = outerZipStream.getNextEntry();
        while (outerZipEntry != null) {
            if (outerZipEntry.getName().endsWith(".zip")) {
                ZipInputStream innerZipStream = new ZipInputStream(outerZipStream);
                ZipEntry innerZipEntry = innerZipStream.getNextEntry();
                while (innerZipEntry != null) {
                    Path filePath = aipExportFolder.resolve(innerZipEntry.getName());
                    if (innerZipEntry.isDirectory()) {
                        Files.createDirectories(filePath);
                    } else {
                        Files.createDirectories(filePath.getParent());
                        ZipUtils.extractFile(innerZipStream, filePath);
                    }
                    innerZipStream.closeEntry();
                    innerZipEntry = innerZipStream.getNextEntry();
                }
            } else {
                Path someUzippedFile = aipExportFolder.resolve(Paths.get(outerZipEntry.getName()).getFileName());
                if (outerZipEntry.isDirectory()) {
                    throw new IllegalStateException("expected only files (zip + xmls) packed in the archival storage response, but there was: " + someUzippedFile);
                } else {
                    ZipUtils.extractFile(outerZipStream, someUzippedFile);
                }
            }
            outerZipStream.closeEntry();
            outerZipEntry = outerZipStream.getNextEntry();
        }
        Set<File> unpackedAipDataDirs = Arrays.stream(Objects.requireNonNull(aipExportFolder.toFile().listFiles())).filter(File::isDirectory).collect(Collectors.toSet());
        if (unpackedAipDataDirs.size() != 1) {
            throw new IllegalStateException("expected exactly one AIP DATA dir at path: " + targetFolder + " but there was: " + unpackedAipDataDirs);
        }
        return unpackedAipDataDirs.iterator().next().toPath();
    }
}
