package cz.cas.lib.arclib.antivirus;

import cz.cas.lib.arclib.antivirus.clamAV.ClamSIPAntivirusScanner;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static helper.ThrowableAssertion.assertThrown;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class SIPAntivirusScannerTests {

    private static final Path QUARANTINE_FOLDER = Paths.get(System.getenv("CLAMAV"), "quarantine");
    private static final String CORRUPTED_FILE_NAME = "eicar.com";
    private static final Path CORRUPTED_FILE_REPRESENTANT = Paths.get("src/test/resources").resolve(CORRUPTED_FILE_NAME);
    private static final Path SIP = Paths.get("src/test/resources/testSIP");

    @BeforeClass
    public static void before() throws IOException {
        Files.copy(CORRUPTED_FILE_REPRESENTANT, SIP.resolve(CORRUPTED_FILE_NAME), REPLACE_EXISTING);
    }

    @AfterClass
    public static void after() throws IOException {
        Files.deleteIfExists(QUARANTINE_FOLDER.resolve(CORRUPTED_FILE_NAME));
    }

    @Test
    public void nullFilePathTest() throws InterruptedException, SIPAntivirusScannerException, IOException {
        SIPAntivirusScanner scanner = new ClamSIPAntivirusScanner();
        assertThrown(() -> scanner.scan(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void fileNotFoundPathTest() throws InterruptedException, SIPAntivirusScannerException, IOException {
        SIPAntivirusScanner scanner = new ClamSIPAntivirusScanner();
        assertThrown(() -> scanner.scan("invalid path")).isInstanceOf(FileNotFoundException.class);
    }

    /**
     * Tests that AV scan process recognizes corrupted file inside folder and move it to quarantine folder. Called on folder.
     */
    @Test()
    public void corruptedFolderTest() throws InterruptedException, SIPAntivirusScannerException, IOException {
        SIPAntivirusScanner scanner = new ClamSIPAntivirusScanner();
        List<Path> corruptedFiles = scanner.scan(SIP.toAbsolutePath().toString());
        assertThat(corruptedFiles, hasSize(1));
        assertThat(corruptedFiles, containsInAnyOrder(SIP.resolve(CORRUPTED_FILE_NAME).toAbsolutePath()));
        scanner.moveToQuarantine(corruptedFiles);
        assertThat(Files.list(QUARANTINE_FOLDER).count(), equalTo(1L));
        assertThat(Files.exists(QUARANTINE_FOLDER.resolve(CORRUPTED_FILE_NAME)), equalTo(true));
        assertThat(Files.notExists(SIP.resolve(CORRUPTED_FILE_NAME)), equalTo(true));
    }

    /**
     * Tests that AV scan process does not evaluate clean file as corrupted and therefore does not move it to quarantine folder. Called on single file.
     */
    @Test()
    public void okFileTest() throws InterruptedException, SIPAntivirusScannerException, IOException {
        SIPAntivirusScanner scanner = new ClamSIPAntivirusScanner();
        List<Path> corruptedFiles = scanner.scan(SIP.resolve("clean.txt").toAbsolutePath().toString());
        assertThat(corruptedFiles, empty());
    }
}
