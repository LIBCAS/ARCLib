package cz.cas.lib.arclib.util;

import cz.cas.lib.arclib.utils.ZipUtils;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ZipUtilsTest {

    private static final Path WS = Paths.get("testWorkspace");
    private static final Path RS = Paths.get("src/test/resources");
    private static final String SIP_FOLDER_STR = "testFolder";
    private static final String SIP_FILE_STR = "clean.txt";
    private static final Path SIP_FOLDER = RS.resolve(SIP_FOLDER_STR);
    private static final Path SIP_ZIP = Paths.get("src/test/resources/testFolder.zip");

    @Before
    public void before() throws IOException {
        Files.createDirectories(WS);
    }

    @After
    public void after() throws IOException {
        FileUtils.cleanDirectory(WS.toFile());
        Files.deleteIfExists(WS);
    }

    @Test
    public void testZipThanUnzip() throws IOException {
        byte[] bytes = ZipUtils.zipToByteArray(SIP_FOLDER);
        Path write = Files.write(WS.resolve("zipToByteArrayTest.zip"), bytes);
        ZipUtils.unzipSip(write, WS, "dummyIwId");
        assertThat(WS.resolve(SIP_FOLDER_STR).toFile().isDirectory(), is(true));
        assertThat(WS.resolve(SIP_FOLDER_STR).resolve(SIP_FILE_STR).toFile().isFile(), is(true));
    }

    @Test
    public void testUnZip() throws IOException {
        ZipUtils.unzipSip(SIP_ZIP, WS, "dummyIwId");
        assertThat(WS.resolve(SIP_FOLDER_STR).toFile().isDirectory(), is(true));
        assertThat(WS.resolve(SIP_FOLDER_STR).resolve(SIP_FILE_STR).toFile().isFile(), is(true));
    }
}
