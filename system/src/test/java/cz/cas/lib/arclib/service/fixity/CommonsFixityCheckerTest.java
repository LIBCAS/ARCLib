package cz.cas.lib.arclib.service.fixity;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.arclib.bpm.IngestTool;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestIssue;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import cz.cas.lib.arclib.service.preservationPlanning.ToolService;
import cz.cas.lib.arclib.store.IngestIssueDefinitionStore;
import cz.cas.lib.arclib.store.IngestWorkflowStore;
import helper.ThrowableAssertion;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

import static helper.ThrowableAssertion.assertThrown;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class CommonsFixityCheckerTest {

    private CommonChecksumFilesChecker service = new CommonChecksumFilesChecker();
    private Path rootPath = Paths.get("src/test/resources/commonFilesFixityCheckerSip");
    private Path checksumFilePath = rootPath.resolve("folder").resolve("test.md5");

    @Mock
    private IngestWorkflowStore iwStore;
    @Mock
    private ToolService toolService;
    @Mock
    private IngestIssueDefinitionStore ingestIssueDefinitionStore;

    @Test
    public void test() throws Exception {
        MockitoAnnotations.initMocks(this);
        service.setIngestWorkflowStore(iwStore);
        service.setToolService(toolService);
        service.setIngestIssueDefinitionStore(ingestIssueDefinitionStore);
        ThrowableAssertion assertion = assertThrown(() -> service.verifySIP(rootPath, checksumFilePath, "blah", new ObjectMapper().createObjectNode(), new HashMap<>(), 1, new IngestTool() {
            @Override
            public String getToolVersion() {
                return null;
            }

            @Override
            public String getToolName() {
                return null;
            }
        }));
        assertion.isInstanceOf(IncidentException.class);
        List<IngestIssue> providedIssues = ((IncidentException) assertion.getCaught()).getProvidedIssues();
        assertThat(providedIssues, hasSize(3));
        assertThat(providedIssues.get(0).getDescription(), containsString("folder/1.txt"));
        assertThat(providedIssues.get(1).getDescription(), containsString("folder/1.txt"));
        assertThat(providedIssues.get(2).getDescription(), containsString("folder/3.txt"));
    }
}
