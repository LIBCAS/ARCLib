package cz.cas.lib.arclib.service.fixity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static helper.ThrowableAssertion.assertThrown;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


public class FixityCounterTests {

    private Md5Counter md5Counter;
    private Crc32Counter crc32Counter;
    private Sha512Counter sha512Counter;

    private static final String FILE_SHA512 = "28e2bcfb7724d64c3e166ff8a541a20f4005c4e453fdc657237554da443bb0e2550304c4e7f143437a72479d30a517d61f290040220e7eeaeda21ac23caedde9";
    private static final String FILE_MD5 = "6226f7cbe59e99a90b5cef6f94f966fd";
    private static final String FILE_CRC32 = "0f149d8b";

    private static final Path FOLDER_PATH = Paths.get("src/test/resources/testFolder");
    private static final Path FILE_PATH = FOLDER_PATH.resolve("clean.txt");
    private static final Path TEST_FILE_PATH = FOLDER_PATH.resolve("tempWhichShouldBeCleaned");

    @Before
    public void setUp() {
        md5Counter = new Md5Counter();
        sha512Counter = new Sha512Counter();
        crc32Counter = new Crc32Counter();
    }

    @After
    public void after() throws IOException {
        Files.deleteIfExists(TEST_FILE_PATH);
    }

    /**
     * Tests that computed digest matches the real digest of a file and that comparison is case-insensitive.
     */
    @Test
    public void testFileOK() throws IOException {
        assertThat(md5Counter.verifyFixity(FILE_PATH, FILE_MD5), is(true));
        assertThat(crc32Counter.verifyFixity(FILE_PATH, FILE_CRC32), is(true));
        assertThat(sha512Counter.verifyFixity(FILE_PATH, FILE_SHA512), is(true));
    }

    /**
     * Tests computed digest does not match defined digest after input change.
     */
    @Test
    public void testFileChanged() throws IOException {
        byte[] fileContent = Files.readAllBytes(FILE_PATH);
        fileContent[0] = '%';
        assertThat(md5Counter.verifyFixity(new ByteArrayInputStream(fileContent), FILE_MD5), equalTo(false));
        assertThat(crc32Counter.verifyFixity(new ByteArrayInputStream(fileContent), FILE_CRC32), equalTo(false));
        assertThat(sha512Counter.verifyFixity(new ByteArrayInputStream(fileContent), FILE_SHA512), equalTo(false));
    }

    /**
     * Tests that exception is thrown when trying to compute digest on a folder.
     */
    @Test
    public void testFolderIllegalArgument() throws IOException {
        assertThrown(() -> md5Counter.verifyFixity(FOLDER_PATH, "blah")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testNotFound() throws IOException {
        assertThrown(() -> md5Counter.computeDigest(Paths.get("invalidpath"))).isInstanceOf(FileNotFoundException.class);
        assertThrown(() -> crc32Counter.computeDigest(Paths.get("invalidpath"))).isInstanceOf(FileNotFoundException.class);
    }

    @Test
    public void testNullPath() throws IOException {
        assertThrown(() -> md5Counter.computeDigest((Path) null)).isInstanceOf(IllegalArgumentException.class);
        assertThrown(() -> crc32Counter.computeDigest((Path) null)).isInstanceOf(IllegalArgumentException.class);
    }
}
