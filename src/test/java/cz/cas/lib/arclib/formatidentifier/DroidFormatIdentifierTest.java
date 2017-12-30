package cz.cas.lib.arclib.formatidentifier;

import cz.cas.lib.arclib.formatidentifier.droid.DroidFormatIdentifier;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static helper.ThrowableAssertion.assertThrown;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.exists;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DroidFormatIdentifierTest {

    private static final String SIP_ID = "KPW01169310";
    private static final String SIP_PATH = "SIP_packages/" + SIP_ID;

    @Autowired
    private DroidFormatIdentifier formatIdentifier;

    /**
     * Test that all the files in the SIP package are scanned (also the files located in all the subfolders)
     */
    @Test
    public void testThatAllFilesScanned() throws InterruptedException, IOException {
        Map<String, List<String>> result = formatIdentifier.analyze(SIP_PATH);

        assertThat(result.size(), is(55));
    }

    /**
     * Test the given files have been identified with the right formats
     */
    @Test
    public void analyzeRightExtensionsIdentifiedTest() throws IOException, InterruptedException {
        Map<String, List<String>> result = formatIdentifier.analyze(SIP_PATH);

        String filePath1 = ("file://METS_KPW01169310.xml");
        assertThat(result.get(filePath1), contains("fmt/101"));

        String filePath2 = ("file://desktop.ini");
        assertThat(result.get(filePath2), contains("x-fmt/421"));

        String filePath3 = ("file://KPW01169310.md5");
        assertThat(result.get(filePath3), contains("fmt/993"));

        String filePath4 = ("file://TXT/TXT_KPW01169310_0002.TXT");
        assertThat(result.get(filePath4), contains("x-fmt/111"));

        String filePath5 = ("file://userCopy/UC_KPW01169310_0001.JP2");
        assertThat(result.get(filePath5), contains("x-fmt/392"));
    }

    /**
     * Test that the right exception is thrown when an ID of a nonexistent SIP is provided
     */
    @Test
    public void analyzeNonExistentPackageTest() {
        assertThrown(() -> formatIdentifier.analyze("nonExistentPackage")).isInstanceOf(FileNotFoundException.class);
    }
}
