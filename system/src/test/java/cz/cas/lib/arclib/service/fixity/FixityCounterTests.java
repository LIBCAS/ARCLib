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

    private static final String FILE_SHA512 = "efa0bf9b43ec678e524c7211089ab1a4be8fa72f8a027169219bc95572175315ef5f9866e5d47fc6af54c0e0c4cb0ae087cf524ca6222024740fe80991e82387";
    private static final String FILE_MD5 = "dc1f353409839d7fa5fb94d72cd28d65";
    private static final String FILE_CRC32 = "8823e419";

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
