package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.ingestWorkflow.IngestEvent;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestIssue;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.preservationPlanning.Format;
import cz.cas.lib.arclib.domain.preservationPlanning.FormatDefinition;
import cz.cas.lib.arclib.domain.preservationPlanning.IngestIssueDefinition;
import cz.cas.lib.arclib.domain.preservationPlanning.Tool;
import helper.SrDbTest;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class IngestEventStoreTest extends SrDbTest {

    private IngestEventStore eventStore = new IngestEventStore();
    private IngestIssueStore issueStore = new IngestIssueStore();
    private IngestWorkflowStore ingestWorkflowStore = new IngestWorkflowStore();
    private ToolStore toolStore = new ToolStore();
    private IngestIssueDefinitionStore ingestIssueDefinitionStore = new IngestIssueDefinitionStore();
    private FormatDefinitionStore formatDefinitionStore = new FormatDefinitionStore();
    private FormatStore formatStore = new FormatStore();


    @Before
    public void setUp() {
        initializeStores(eventStore,
                issueStore,
                ingestWorkflowStore,
                toolStore,
                ingestIssueDefinitionStore,
                formatDefinitionStore,
                formatStore);
    }

    @Test
    public void inheritanceTest() {
        IngestWorkflow iw = new IngestWorkflow();
        iw.setExternalId("1");
        ingestWorkflowStore.save(iw);
        Tool tool = toolStore.save(new Tool());
        IngestIssueDefinition issueDef = new IngestIssueDefinition();
        issueDef.setNumber("1");
        ingestIssueDefinitionStore.save(issueDef);
        Format format = formatStore.save(new Format());
        FormatDefinition fDef = new FormatDefinition();
        fDef.setFormat(format);
        formatDefinitionStore.save(fDef);
        issueStore.save(new IngestIssue(iw, tool, issueDef, fDef, null, false));
        eventStore.save(new IngestEvent());

        assertThat(eventStore.findAll().size(), is(2));
    }
}
