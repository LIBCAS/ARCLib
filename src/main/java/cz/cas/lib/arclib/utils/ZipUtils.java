package cz.cas.lib.arclib.utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtils {
    /**
     * Size of the buffer to read/write data
     */
    private static final int BUFFER_SIZE = 4096;

    /**
     * Extracts a zip file specified by the zipFilePath to a directory specified by
     * destDirectory (will be created if does not exists)
     *
     * @param zipInput
     * @param destDirectory
     * @throws IOException
     */
    public static void unzip(InputStream zipInput, Path destDirectory) throws IOException {
        Files.createDirectories(destDirectory);
        ZipInputStream zipIn = new ZipInputStream(zipInput);
        ZipEntry entry = zipIn.getNextEntry();
        while (entry != null) {
            Path filePath = destDirectory.resolve(entry.getName());
            if (!entry.isDirectory()) {
                Files.createDirectories(filePath.getParent());
                extractFile(zipIn, filePath);
            } else {
                Files.createDirectories(filePath);
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
    }

    /**
     * Extracts a zip entry (file entry)
     *
     * @param zipIn
     * @param filePath
     * @throws IOException
     */
    private static void extractFile(ZipInputStream zipIn, Path filePath) throws IOException {
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
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(rootPath + sourceDirPath.relativize(path).toString());
                        try {
                            zs.putNextEntry(zipEntry);
                            Files.copy(path, zs);
                            zs.closeEntry();
                        } catch (IOException e) {
                            System.err.println(e);
                        }
                    });
            packed = bos.toByteArray();
        }
        return packed;
    }
}
