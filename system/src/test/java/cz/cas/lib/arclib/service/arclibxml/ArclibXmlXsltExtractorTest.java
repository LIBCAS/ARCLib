package cz.cas.lib.arclib.service.arclibxml;

import com.google.common.io.Resources;
import cz.cas.lib.arclib.bpm.ArclibXmlExtractorDelegate;
import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.domain.Batch;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.profiles.SipProfile;
import cz.cas.lib.arclib.service.SipProfileService;
import cz.cas.lib.arclib.store.BatchStore;
import cz.cas.lib.arclib.store.IngestWorkflowStore;
import cz.cas.lib.arclib.store.SipProfileStore;
import cz.cas.lib.core.sequence.Generator;
import cz.cas.lib.core.sequence.Sequence;
import cz.cas.lib.core.sequence.SequenceStore;
import helper.SrDbTest;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static cz.cas.lib.core.util.Utils.asList;
import static cz.cas.lib.core.util.Utils.asMap;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ArclibXmlXsltExtractorTest extends SrDbTest {
    private static final String PATH_TO_METS = "mets_7033d800-0935-11e4-beed-5ef3fc9ae867.xml";
    public static final Path WS = Paths.get("testWorkspace");
    public static final Path SIP = Paths.get("src/test/resources/SIP_PACKAGE/" +
            "7033d800-0935-11e4-beed-5ef3fc9ae867");
    public static final String INGEST_WORKFLOW_ID = "uuid1";
    public static final String EXTERNAL_ID = "ARCLIB_000000001";
    public static final String ORIGINAL_SIP_FILE_NAME = "7033d800-0935-11e4-beed-5ef3fc9ae867";
    public static final Path WS_SIP_LOCATION = Paths.get(WS.toString(), EXTERNAL_ID, SIP.getFileName().toString());


    private ArclibXmlXsltExtractor arclibXmlXsltExtractor;
    private SipProfileStore sipProfileStore;

    private BatchStore batchStore;
    private IngestWorkflowStore ingestWorkflowStore;
    private Generator generator;
    private SequenceStore sequenceStore;

    private SipProfileService sipProfileService;

    private Batch batch;
    private IngestWorkflow ingestWorkflow;
    private SipProfile sipProfile;

    @AfterClass
    public static void tearDown() throws IOException {
        FileUtils.cleanDirectory(WS.toFile());
        FileUtils.deleteDirectory(WS.toFile());
    }

    @BeforeClass
    public static void init() throws IOException {
        Files.createDirectories(WS_SIP_LOCATION);
        FileUtils.copyDirectory(SIP.toFile(), WS_SIP_LOCATION.toFile());
    }

    @Before
    public void setUp() throws IOException {
        sipProfileStore = new SipProfileStore();
        sequenceStore = new SequenceStore();

        batchStore = new BatchStore();
        batchStore.setTemplate(getTemplate());

        ingestWorkflowStore = new IngestWorkflowStore();

        initializeStores(batchStore, sipProfileStore, ingestWorkflowStore, sequenceStore);

        Sequence sequence = new Sequence();
        sequence.setCounter(2L);
        sequence.setFormat("'#'#");
        sequence.setId(ingestWorkflowStore.getSEQUENCE_ID());
        sequenceStore.save(sequence);

        Sequence sequence2 = new Sequence();
        sequence2.setCounter(2L);
        sequence2.setFormat("'#'#");
        sequence2.setId(sipProfileStore.getSEQUENCE_ID());
        sequenceStore.save(sequence2);

        generator = new Generator();
        generator.setStore(sequenceStore);
        ingestWorkflowStore.setGenerator(generator);
        sipProfileStore.setGenerator(generator);

        ingestWorkflow = new IngestWorkflow();
        ingestWorkflow.setFileName(ORIGINAL_SIP_FILE_NAME);
        ingestWorkflow.setExternalId(EXTERNAL_ID);
        ingestWorkflowStore.save(ingestWorkflow);

        batch = new Batch();
        batch.setIngestWorkflows(asList(ingestWorkflow));
        batchStore.save(batch);

        sipProfile = new SipProfile();
        String sipProfileXml = Resources.toString(this.getClass().getResource(
                "/sipProfiles/comprehensiveSipProfile.xsl"), StandardCharsets.UTF_8);
        sipProfile.setXsl(sipProfileXml);
        sipProfile.setSipMetadataPathRegex(PATH_TO_METS);
        sipProfileStore.save(sipProfile);

        sipProfileService = new SipProfileService();
        sipProfileService.setStore(sipProfileStore);

        arclibXmlXsltExtractor = new ArclibXmlXsltExtractor();
        arclibXmlXsltExtractor.setSipProfileService(sipProfileService);
    }

    @Test
    public void extractMetadataTest() throws TransformerException, IOException {
        Map<String, Object> variables = asMap(BpmConstants.ProcessVariables.batchId, batch.getId());
        variables.put(BpmConstants.ProcessVariables.latestConfig, String.format("{\"%s\":\"%s\"}", ArclibXmlExtractorDelegate.SIP_PROFILE_CONFIG_ENTRY, sipProfile.getExternalId()));
        variables.put(BpmConstants.ProcessVariables.ingestWorkflowExternalId, ingestWorkflow.getExternalId());
        variables.put(BpmConstants.Ingestion.sipFileName, ingestWorkflow.getFileName());
        variables.put(BpmConstants.ProcessVariables.sipFolderWorkspacePath,SIP.toAbsolutePath().toString());
        String extractionResult = arclibXmlXsltExtractor.extractMetadata(sipProfile.getExternalId(), variables).replace("\\", "");
        assertThat(extractionResult.contains("mets:mets"), is(true));
        assertThat(extractionResult.contains("<ARCLib:eventAgent>"), is(true));
    }
}
