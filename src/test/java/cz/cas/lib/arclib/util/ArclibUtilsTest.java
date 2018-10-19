package cz.cas.lib.arclib.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestIssue;
import cz.cas.lib.arclib.domain.packages.FolderStructure;
import cz.cas.lib.arclib.utils.ArclibUtils;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static cz.cas.lib.core.util.Utils.asList;
import static cz.cas.lib.core.util.Utils.asSet;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ArclibUtilsTest {

    public static final String ROOT = "root";

    @Test
    public void filePathsToFolderStructureTest() {
        List<String> filePaths = new ArrayList<>();

        filePaths.add("folder1/folder2/file1");
        filePaths.add("folder1/folder2/file2");
        filePaths.add("folder3/file3");
        filePaths.add("file4");
        filePaths.add("folder4");

        FolderStructure file1 = new FolderStructure(null, "file1");
        FolderStructure file2 = new FolderStructure(null, "file2");
        FolderStructure file3 = new FolderStructure(null, "file3");
        FolderStructure file4 = new FolderStructure(null, "file4");

        FolderStructure folder2 = new FolderStructure(asSet(file1, file2), "folder2");
        FolderStructure folder1 = new FolderStructure(asSet(folder2), "folder1");
        FolderStructure folder3 = new FolderStructure(asSet(file3), "folder3");
        FolderStructure folder4 = new FolderStructure(null, "folder4");

        List<FolderStructure> expectedChildren = asList(folder1, folder3, file4, folder4).stream()
                .sorted(Comparator.comparing(FolderStructure::getCaption))
                .collect(Collectors.toList());

        FolderStructure expectedFolderStructure = new FolderStructure(expectedChildren, ROOT);
        FolderStructure folderStructure = ArclibUtils.filePathsToFolderStructure(filePaths, ROOT);

        assertThat(folderStructure.getCaption(), equalTo(expectedFolderStructure.getCaption()));
        assertThat(folderStructure.getChildren().size(), equalTo(expectedFolderStructure.getChildren().size()));

        List<FolderStructure> children = folderStructure.getChildren().stream()
                .sorted(Comparator.comparing(FolderStructure::getCaption))
                .collect(Collectors.toList());

        assertThat(children.get(0).getCaption(), equalTo(expectedChildren.get(0).getCaption()));
        assertThat(children.get(children.size() - 1).getCaption(), equalTo(
                expectedChildren.get(expectedChildren.size() - 1).getCaption()));
    }

    @Test
    public void booleanConfigParserTest() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode configTrue = objectMapper.readTree("{\"testKey\":{\"testValue\":true}}");
        JsonNode configFalse = objectMapper.readTree("{\"testKey\":{\"testValue\":false}}");
        JsonNode configInvalid = objectMapper.readTree("{\"testKey\":{\"testValue\":\"blah\"}}");
        JsonNode configEmpty = objectMapper.readTree("{}");
        String configPath = "/testKey/testValue";
        IngestIssue issue = Mockito.spy(new IngestIssue());

        Boolean value = ArclibUtils.parseBooleanConfig(configTrue, configPath, issue);
        assertThat(value, is(true));
        assertThat(issue.isSolvedByConfig(), is(true));

        value = ArclibUtils.parseBooleanConfig(configInvalid, configPath, issue);
        assertThat(value, nullValue());
        assertThat(issue.isSolvedByConfig(), is(false));

        value = ArclibUtils.parseBooleanConfig(configFalse, configPath, issue);
        assertThat(value, is(false));
        assertThat(issue.isSolvedByConfig(), is(true));

        value = ArclibUtils.parseBooleanConfig(configEmpty, configPath, issue);
        assertThat(value, nullValue());
        assertThat(issue.isSolvedByConfig(), is(false));

        verify(issue, times(1)).setInvalidConfigNote(eq(configPath), anyString(), anyString(), anyString());
        verify(issue, times(1)).setMissingConfigNote(eq(configPath));
        verify(issue, times(2)).setUsedConfigNote(eq(configPath), anyString());
    }
}
