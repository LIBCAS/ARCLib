package cz.cas.lib.arclib.service.archivalStorage;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.zip.ZipInputStream;

import static org.hamcrest.Matchers.containsInAnyOrder;

public class ArchivalStorageExtractorTest {
    private static final Path TEST_WORKSPACE = Paths.get("testWorkspace");
    private static final Path RESOURCES = Paths.get("src/test/resources");

    private ArchivalStorageResponseExtractor extractor = new ArchivalStorageResponseExtractor();

    @Before
    public void before() throws IOException {
        Files.createDirectories(TEST_WORKSPACE);
    }

    @After
    public void after() throws IOException {
        FileUtils.cleanDirectory(TEST_WORKSPACE.toFile());
        Files.deleteIfExists(TEST_WORKSPACE);
    }

    @Test
    public void testExtractAipAsFolderWithXmlsBySide() throws IOException {
        String aipId = "7f7a3394-4a45-474c-9356-3aef3bbba7c8";
        Path targetFolder = TEST_WORKSPACE.resolve(aipId);
        try (FileInputStream stream = new FileInputStream(RESOURCES.resolve("7f7a3394-4a45-474c-9356-3aef3bbba7c8.zip").toFile())) {
            extractor.extractAipAsFolderWithXmlsBySide(new ZipInputStream(stream), aipId, targetFolder);
        }
        Assert.assertThat(Arrays.asList(targetFolder.toFile().list()), containsInAnyOrder("7f7a3394-4a45-474c-9356-3aef3bbba7c8_xml_2.xml", "7033d800-0935-11e4-beed-5ef3fc9ae867"));
    }
}
