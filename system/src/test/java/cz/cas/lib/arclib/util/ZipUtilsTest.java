package cz.cas.lib.arclib.util;

import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.utils.ZipUtils;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static helper.ThrowableAssertion.assertThrown;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ZipUtilsTest {

    private static final Path TEST_WORKSPACE = Paths.get("testWorkspace");
    private static final Path RESOURCES = Paths.get("src/test/resources");
    private static final String SIP_FOLDER_STR = "testFolder";
    private static final String SIP_FILE_STR = "clean.txt";
    private static final Path SIP_FOLDER = RESOURCES.resolve(SIP_FOLDER_STR);
    private static final Path SIP_ZIP = Paths.get("src/test/resources/testFolder.zip");

    // .zip file created on Windows machine that contains file with diacritics in its name
    private static final String DIACRITICS_WINDOWS_ZIP_NAME = "zipSDiakritikou";
    private static final Path DIACRITICS_WINDOWS_ZIP = RESOURCES.resolve(DIACRITICS_WINDOWS_ZIP_NAME + ".zip");

    // folder for creating .zip by local machine with diacritics
    private static final String LOCAL_DIACRITICS_FOLDER_NAME = "testovacíčů-foldřýk";
    private static final String LOCAL_DIACRITICS_FILE_NAME = "czech-name-file-ěščřžýáíéůĚŠČŘŽÝÁÍÉŮ.txt";
    private static final Path LOCAL_DIACRITICS_FOLDER = RESOURCES.resolve(LOCAL_DIACRITICS_FOLDER_NAME);
    private static final Path LOCAL_DIACRITICS_FOLDER_ZIP = TEST_WORKSPACE.resolve(LOCAL_DIACRITICS_FOLDER_NAME + ".zip");


    @Before
    public void before() throws IOException {
        Files.createDirectories(TEST_WORKSPACE);
    }

    @After
    public void after() throws IOException {
        FileUtils.cleanDirectory(TEST_WORKSPACE.toFile());
        Files.deleteIfExists(TEST_WORKSPACE);
    }

    /**
     * There is a problem when a .zip is created on Windows and some file contains diacritics in the name.
     * Zipping in Windows chooses Code Page 437 to encode file names for backwards compatibility.
     * Unzipping such .zip files on UNIX machines with Java11 raises an error
     * `ZipException: invalid CEN header (bad entry name)` because the default expected encoding is UTF-8.
     *
     * https://stackoverflow.com/questions/55393956/java-unzip-folder-with-german-characters-in-filenames
     */
    @Test
    public void unzipWindowsEncodedZipWithDiacritics() {
        Throwable exception = assertThrown(() -> ZipUtils.unzipSip(DIACRITICS_WINDOWS_ZIP, TEST_WORKSPACE, "dummyIwId")).isInstanceOf(GeneralException.class).getCaught();
        String msg = exception.getCause().toString();
        assertThat(msg.contains("CEN header") || msg.contains("malformed"), is(true));
    }

    @Test
    public void zipAndUnzipDiacriticsFolder() throws IOException {
        byte[] bytesOfDiacriticsZipFolder = ZipUtils.zipToByteArray(LOCAL_DIACRITICS_FOLDER);
        Path zippedFolderInTestWorkspace = Files.write(LOCAL_DIACRITICS_FOLDER_ZIP, bytesOfDiacriticsZipFolder);
        ZipUtils.unzipSip(zippedFolderInTestWorkspace, TEST_WORKSPACE, "dummyIwId");

        assertThat(TEST_WORKSPACE.resolve(LOCAL_DIACRITICS_FOLDER_NAME).toFile().isDirectory(), is(true));
        assertThat(TEST_WORKSPACE.resolve(LOCAL_DIACRITICS_FOLDER_NAME).resolve(LOCAL_DIACRITICS_FILE_NAME).toFile().isFile(), is(true));
    }

    @Test
    public void testZipThanUnzip() throws IOException {
        byte[] bytes = ZipUtils.zipToByteArray(SIP_FOLDER);
        Path write = Files.write(TEST_WORKSPACE.resolve("zipToByteArrayTest.zip"), bytes);
        ZipUtils.unzipSip(write, TEST_WORKSPACE, "dummyIwId");
        assertThat(TEST_WORKSPACE.resolve(SIP_FOLDER_STR).toFile().isDirectory(), is(true));
        assertThat(TEST_WORKSPACE.resolve(SIP_FOLDER_STR).resolve(SIP_FILE_STR).toFile().isFile(), is(true));
    }

    @Test
    public void testZipThanUnzipWithoutDirEntries() throws IOException {
        byte[] bytes = zipWithoutRootDirEntryToByteArray(SIP_FOLDER);
        Path write = Files.write(TEST_WORKSPACE.resolve("zipToByteArrayTest.zip"), bytes);
        ZipUtils.unzipSip(write, TEST_WORKSPACE, "dummyIwId");
        assertThat(TEST_WORKSPACE.resolve(SIP_FOLDER_STR).toFile().isDirectory(), is(true));
        assertThat(TEST_WORKSPACE.resolve(SIP_FOLDER_STR).resolve(SIP_FILE_STR).toFile().isFile(), is(true));
    }

    @Test
    public void testUnZip() throws IOException {
        ZipUtils.unzipSip(SIP_ZIP, TEST_WORKSPACE, "dummyIwId");
        assertThat(TEST_WORKSPACE.resolve(SIP_FOLDER_STR).toFile().isDirectory(), is(true));
        assertThat(TEST_WORKSPACE.resolve(SIP_FOLDER_STR).resolve(SIP_FILE_STR).toFile().isFile(), is(true));
    }

    private static byte[] zipWithoutRootDirEntryToByteArray(Path sourceDirPath) throws IOException {
        byte[] packed;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ZipOutputStream zs = new ZipOutputStream(bos)) {
            String rootPath = sourceDirPath.getFileName().toString() + "/";
            zs.putNextEntry(new ZipEntry("empty.txt"));
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
            zs.close();
            packed = bos.toByteArray();
        }
        return packed;
    }
}
