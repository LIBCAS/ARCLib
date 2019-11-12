package cz.cas.lib.arclib.service.formatIdentification;

import cz.cas.lib.arclib.service.formatIdentification.droid.CsvResultColumn;
import cz.cas.lib.arclib.service.formatIdentification.droid.DroidFormatIdentificationTool;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static helper.ThrowableAssertion.assertThrown;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;

public class DroidFormatIdentificationToolTest {

    private static final String SIP_ID = "KPW01169310";
    private static final Path SIP_PATH = Paths.get("src/test/resources/SIP_package", SIP_ID);
    private FormatIdentificationTool formatIdentificationTool;

    @Before
    public void setUp() {
        formatIdentificationTool = new DroidFormatIdentificationTool(CsvResultColumn.PUID);
    }

    @Test
    public void nullFilePathTest() {
        assertThrown(() -> formatIdentificationTool.analyze(null)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Test that the right exception is thrown when an ID of a nonexistent SIP is provided
     */
    @Test
    public void analyzeNonExistentPackageTest() {
        assertThrown(() -> formatIdentificationTool.analyze(Paths.get("nonExistentPackage"))).isInstanceOf(FileNotFoundException.class);
    }

    /**
     * Test that all the files in the SIP package are scanned (also the files located in all the subfolders)
     */
    @Test
    public void testThatAllFilesScanned() throws IOException {
        Map<String, List<Pair<String, String>>> result = formatIdentificationTool.analyze(SIP_PATH);

        assertThat(result.size(), is(49));
    }

    /**
     * Test the given files have been identified with the right formats
     */
    @Test
    public void analyzeRightExtensionsIdentifiedTest() throws IOException {
        Map<String, List<Pair<String, String>>> result = formatIdentificationTool.analyze(SIP_PATH);

        String filePath1 = ("METS_KPW01169310.xml");
        assertThat(result.get(filePath1), contains(Pair.of("fmt/101", "Signature")));

        String filePath2 = ("desktop.ini");
        assertThat(result.get(filePath2), contains(Pair.of("x-fmt/421", "Extension")));

        String filePath3 = ("KPW01169310.md5");
        assertThat(result.get(filePath3), contains(Pair.of("fmt/993", "Extension")));

        String filePath4 = ("TXT/TXT_KPW01169310_0002.TXT");
        assertThat(result.get(filePath4), contains(Pair.of("x-fmt/111", "Extension")));

        String filePath5 = ("userCopy/UC_KPW01169310_0001.JP2");
        assertThat(result.get(filePath5), contains(Pair.of("x-fmt/392", "Signature")));
    }
}
