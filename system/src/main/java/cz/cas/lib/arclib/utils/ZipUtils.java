package cz.cas.lib.arclib.utils;

import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@Slf4j
public class ZipUtils {
    /**
     * Size of the buffer to read/write data
     */
    private static final int BUFFER_SIZE = 4096;

    /**
     * Extracts a SIP ZIP file specified by the zipFilePath to a directory specified by
     * destDirectory (will be created if does not exists)
     *
     * @param zipInput
     * @param destDirectory
     * @return name of the root folder
     * @throws IOException
     * @throws cz.cas.lib.arclib.domainbase.exception.GeneralException if there is not exactly one root folder inside the ZIP
     */
    public static String unzipSip(Path zipInput, Path destDirectory, String ingestWorkflowLogId) {
        try (ZipFile zipFile = new ZipFile(zipInput.toFile())) {
            Files.createDirectories(destDirectory);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            Set<String> rootDirNames = new HashSet<>();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path filePath = destDirectory.resolve(entry.getName());
                int rootDirSeparatorIdx = entry.getName().indexOf('/');
                if (rootDirSeparatorIdx != -1) {
                    rootDirNames.add(entry.getName().substring(0, rootDirSeparatorIdx));
                }
                if (!entry.isDirectory()) {
                    Files.createDirectories(filePath.getParent());
                    extractFile(zipFile.getInputStream(entry), filePath);
                } else {
                    Files.createDirectories(filePath);
                }
            }
            if (rootDirNames.size() != 1) {
                throw new GeneralException("Invalid input ZIP format. ZIP has to include exactly one root folder. But " + rootDirNames.size() + " were found (" + Arrays.toString(rootDirNames.toArray()) + ")");
            }
            log.debug("SIP content for ingest workflow external id " + ingestWorkflowLogId + " in zip archive has been" +
                    " extracted to workspace.");
            return rootDirNames.iterator().next();
        } catch (Exception e) {
            throw new GeneralException("Unable to unzip SIP content for ingest workflow external id "
                    + ingestWorkflowLogId + " to path: " + destDirectory.toAbsolutePath().toString() + ". See log for causing exception stacktrace. If you see errors like ..malformed.. or ..invalid CEN header.. the cause may be that name of some zipped file/folder contains non-standard characters and entries names are not encoded in UTF-8", e);
        }
    }

    /**
     * Extracts a zip entry (file entry)
     *
     * @param zipIn
     * @param filePath
     * @throws IOException
     */
    public static void extractFile(InputStream zipIn, Path filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath.toFile()));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }

    /**
     * This method zips whole directory into bytearay, first entry is the source directory itself
     * Note that all content is read into memory
     *
     * @param sourceDirPath
     * @return zipped folder as a byte array
     */
    public static byte[] zipToByteArray(Path sourceDirPath) throws IOException {
        byte[] packed;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ZipOutputStream zs = new ZipOutputStream(bos)) {
            String rootPath = sourceDirPath.getFileName().toString() + "/";
            zs.putNextEntry(new ZipEntry(rootPath));
            Files.walk(sourceDirPath)
                    .forEach(path -> {
                        String entryName = rootPath + sourceDirPath.relativize(path);
                        if (Files.isDirectory(path)) {
                            entryName = entryName + "/";
                        }
                        ZipEntry zipEntry = new ZipEntry(entryName);
                        try {
                            zs.putNextEntry(zipEntry);
                            Files.copy(path, zs);
                            zs.closeEntry();
                        } catch (IOException e) {
                            System.err.println(e);
                        }
                    });
            zs.close();
            packed = bos.toByteArray();
        }
        return packed;
    }

    public static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            for (File childFile : children) {
                zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
            }
            return;
        }
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
    }
}
