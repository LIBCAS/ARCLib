package cz.cas.lib.arclib.bpm.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import cz.cas.lib.arclib.bpm.ArclibXmlGeneratorDelegate;
import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.domain.*;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestIssue;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflowState;
import cz.cas.lib.arclib.domain.packages.AuthorialPackage;
import cz.cas.lib.arclib.domain.packages.FolderStructure;
import cz.cas.lib.arclib.domain.packages.Sip;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.domain.profiles.SipProfile;
import cz.cas.lib.arclib.index.solr.arclibxml.SolrArclibXmlStore;
import cz.cas.lib.arclib.security.user.UserDelegate;
import cz.cas.lib.arclib.service.arclibxml.ArclibXmlGenerator;
import cz.cas.lib.arclib.service.arclibxml.ArclibXmlXsltExtractor;
import cz.cas.lib.arclib.service.fixity.Sha512Counter;
import cz.cas.lib.arclib.store.*;
import cz.cas.lib.arclib.utils.ArclibUtils;
import cz.cas.lib.core.sequence.Generator;
import cz.cas.lib.core.sequence.Sequence;
import cz.cas.lib.core.sequence.SequenceStore;
import cz.cas.lib.core.util.Utils;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static cz.cas.lib.core.util.Utils.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

@Deployment(resources = "bpmn/arclibXmlGenerator.bpmn")
public class ArclibXmlGeneratorDelegateTest extends DelegateTest {

    private static final String PROCESS_INSTANCE_KEY = "arclibXmlGeneratorProcess";
    private static final String PATH_TO_METS = "mets_7033d800-0935-11e4-beed-5ef3fc9ae867.xml";
    private static final String SIP_FILE_NAME = SIP.getFileName().toString();
    private static final String CORE_NAME = "arclib_xml_test";

    private static final Integer SIP_VERSION = 2;
    private static final Integer XML_VERSION = 3;
    private static final String PRODUCER_ID = "2354235FD2";
    private static final String VALIDATION_PROFILE_ID = "3523423523";
    private static final String FILE_NAME = "7033d800-0935-11e4-beed-5ef3fc9ae867";

    private static final String SIP_HASH = "50FDE99373B04363727473D00AE938A4F4DEBFD0AFB1D428337D81905F6863B3CC303BB3" +
            "31FFB3361085C3A6A2EF4589FF9CD2014C90CE90010CD3805FA5FBC6";

    String filePath1 = "dmdsec/amd_mets_7033d800-0935-11e4-beed-5ef3fc9ae867_0005.xml";
    String filePath2 = "amdsec/amd_mets_7033d800-0935-11e4-beed-5ef3fc9ae867_0006.xml";
    String filePath3 = "amdsec/amd_mets_7033d800-0935-11e4-beed-5ef3fc9ae867_0007.xml";

    private ArclibXmlXsltExtractor extractor;
    private ArclibXmlGenerator arclibXmlGenerator;
    private Generator generator;
    private Sha512Counter sha512Counter;

    private IngestWorkflowStore ingestWorkflowStore;
    private SipProfileStore sipProfileStore;
    private SipStore sipStore;
    private ProducerProfileStore producerProfileStore;
    private ProducerStore producerStore;
    private AuthorialPackageStore authorialPackageStore;
    private BatchStore batchStore;
    private SolrArclibXmlStore solrArclibXmlStore;
    private UserStore userStore;
    private AipQueryStore aipQueryStore;
    private IngestIssueStore ingestIssueStore;
    private SequenceStore sequenceStore;
    private HashStore hashStore;

    private ArclibXmlGeneratorDelegate arclibXmlGeneratorDelegate;
    private SipProfile sipProfile;
    private IngestWorkflow ingestWorkflow;
    private IngestWorkflow relatedIngestWorkflow;
    private Batch batch;
    private UserDelegate userDelegate;
    private User user;
    private Sip sip;
    private Sip previousVersionSip;
    private AuthorialPackage authorialPackage;

    @Before
    public void before() throws IOException {
        hashStore = new HashStore();

        aipQueryStore = new AipQueryStore();
        sequenceStore = new SequenceStore();

        solrArclibXmlStore = new SolrArclibXmlStore();
        solrArclibXmlStore.setAipQueryStore(aipQueryStore);
        solrArclibXmlStore.setCoreName(CORE_NAME);
        solrArclibXmlStore.setSolrTemplate(getArclibXmlSolrTemplate());
        solrArclibXmlStore.setUris("http://www.loc.gov/METS/",
                "http://arclib.lib.cas.cz/ARCLIB_XML",
                "info:lc/xmlns/premis-v2",
                "http://www.openarchives.org/OAI/2.0/oai_dc/",
                "http://purl.org/dc/elements/1.1/",
                "http://purl.org/dc/terms/");

        userStore = new UserStore();
        userStore.setTemplate(getTemplate());

        ingestWorkflowStore = new IngestWorkflowStore();
        sipProfileStore = new SipProfileStore();
        sipStore = new SipStore();
        authorialPackageStore = new AuthorialPackageStore();
        producerStore = new ProducerStore();

        batchStore = new BatchStore();
        batchStore.setTemplate(getTemplate());

        ingestWorkflowStore.setGenerator(generator);

        producerProfileStore = new ProducerProfileStore();
        producerProfileStore.setTemplate(getTemplate());

        ingestIssueStore = new IngestIssueStore();
        ingestIssueStore.setTemplate(getTemplate());

        initializeStores(hashStore, sequenceStore, ingestIssueStore, aipQueryStore, ingestWorkflowStore, sipProfileStore,
                sipStore, batchStore, producerProfileStore, producerStore, authorialPackageStore, userStore);

        generator = new Generator();
        generator.setStore(sequenceStore);

        sha512Counter = new Sha512Counter();

        Sequence sequence = new Sequence();
        sequence.setCounter(2L);
        sequence.setFormat("'#'#");
        sequence.setId(ingestWorkflowStore.getSEQUENCE_ID());
        sequenceStore.save(sequence);

        Sequence sequence2 = new Sequence();
        sequence2.setCounter(2L);
        sequence2.setFormat("'#'#");
        sequence2.setId(producerProfileStore.getSEQUENCE_ID());
        sequenceStore.save(sequence2);

        producerProfileStore.setGenerator(generator);
        ingestWorkflowStore.setGenerator(generator);

        extractor = new ArclibXmlXsltExtractor();
        extractor.setSipProfileStore(sipProfileStore);
        extractor.setWorkspace(WS.toString());

        arclibXmlGenerator = new ArclibXmlGenerator();
        arclibXmlGenerator.setUris("http://www.loc.gov/METS/",
                "http://arclib.lib.cas.cz/ARCLIB_XML",
                "info:lc/xmlns/premis-v2",
                "http://www.openarchives.org/OAI/2.0/oai_dc/",
                "http://purl.org/dc/elements/1.1/",
                "http://purl.org/dc/terms/"
        );
        arclibXmlGenerator.setArclibVersion("1.0");
        arclibXmlGenerator.setIngestWorkflowStore(ingestWorkflowStore);
        arclibXmlGenerator.setIngestIssueStore(ingestIssueStore);

        user = new User();
        userDelegate = new UserDelegate(user);

        solrArclibXmlStore.setUserDetails(userDelegate);

        arclibXmlGeneratorDelegate = new ArclibXmlGeneratorDelegate();
        arclibXmlGeneratorDelegate.setObjectMapper(new ObjectMapper());
        arclibXmlGeneratorDelegate.setArclibXmlXsltExtractor(extractor);
        arclibXmlGeneratorDelegate.setWorkspace(WS.toString());
        arclibXmlGeneratorDelegate.setIndexArclibXmlStore(solrArclibXmlStore);
        arclibXmlGeneratorDelegate.setArclibXmlGenerator(arclibXmlGenerator);
        arclibXmlGeneratorDelegate.setIngestWorkflowStore(ingestWorkflowStore);
        arclibXmlGeneratorDelegate.setSha512Counter(sha512Counter);

        sipProfile = new SipProfile();
        String sipProfileXml = Resources.toString(this.getClass().getResource(
                "/sipProfiles/comprehensiveSipProfile.xsl"), StandardCharsets.UTF_8);
        sipProfile.setXsl(sipProfileXml);
        sipProfile.setSipMetadataPath(PATH_TO_METS);
        sipProfileStore.save(sipProfile);

        Producer producer = new Producer();
        producer.setName("digital library of organization XYZ a.s.");
        producerStore.save(producer);

        ProducerProfile producerProfile = new ProducerProfile();
        producerProfile.setProducer(producer);
        producerProfileStore.save(producerProfile);

        batch = new Batch();
        batch.setProducerProfile(producerProfile);
        batch.setState(BatchState.PROCESSING);
        batchStore.save(batch);

        flushCache();

        authorialPackage = new AuthorialPackage();
        authorialPackage.setAuthorialId("this is an authorial id Y3DF3FDG");
        authorialPackageStore.save(authorialPackage);

        previousVersionSip = new Sip();
        sipStore.save(previousVersionSip);

        flushCache();

        Hash sipHash = new Hash(SIP_HASH, HashType.Sha512);
        hashStore.save(sipHash);

        FolderStructure folderStructure = ArclibUtils.filePathsToFolderStructure(asList(filePath1, filePath2, filePath3), FILE_NAME);
        sip = new Sip();
        sip.setHashes(asSet(sipHash));
        sip.setAuthorialPackage(authorialPackage);
        sip.setPreviousVersionSip(previousVersionSip);
        sip.setVersionNumber(SIP_VERSION);
        sip.setFolderStructure(folderStructure);
        sipStore.save(sip);

        relatedIngestWorkflow = new IngestWorkflow();
        ingestWorkflowStore.save(relatedIngestWorkflow);

        ingestWorkflow = new IngestWorkflow();
        ingestWorkflow.setSip(sip);
        ingestWorkflow.setId(INGEST_WORKFLOW_ID);
        ingestWorkflow.setOriginalFileName(SIP_FILE_NAME);
        ingestWorkflow.setBatch(batch);
        ingestWorkflow.setRelatedWorkflow(relatedIngestWorkflow);
        ingestWorkflow.setExternalId(EXTERNAL_ID);
        ingestWorkflow.setVersioningLevel(VersioningLevel.NO_VERSIONING);
        ingestWorkflow.setProcessingState(IngestWorkflowState.PROCESSED);
        ingestWorkflow.setXmlVersionNumber(XML_VERSION);
        ingestWorkflowStore.save(ingestWorkflow);

        Mocks.register("arclibXmlGeneratorDelegate", arclibXmlGeneratorDelegate);
    }

    @Test
    public void testArclibXmlGeneratorDelegate() throws InterruptedException {
        Map<String, Object> variables = asMap(BpmConstants.ProcessVariables.batchId, batch.getId());

        TreeMap<String, Utils.Pair<String, String>> identifiedFormats = new TreeMap<>();

        identifiedFormats.put(filePath1, new Utils.Pair("fmt/248", "extension"));
        identifiedFormats.put(filePath2, new Utils.Pair<>("x-fmt/111", "content"));
        identifiedFormats.put(filePath3, new Utils.Pair<>("fmt/248", "extension"));

        List<Utils.Pair<String, String>> filePathsToSizes = asList(
                new Pair(filePath1, "23523423"),
                new Pair(filePath2, "5342235"),
                new Pair(filePath3, "632432"));

        Utils.Triplet<String, String, String> file1 = new Utils.Triplet(
                filePath1, "MD5", "2a6d072e0f741b52148abe308bb506d8");
        Utils.Triplet<String, String, String> file2 = new Utils.Triplet(
                filePath2, "MD5", "d906978fbcd8c9488d604ccc30f323fd");
        Utils.Triplet<String, String, String> file3 = new Utils.Triplet(
                filePath3, "MD5", "e4b3c5adf26ebe80cf0a718be5ace66f");

        variables.put(BpmConstants.MessageDigestCalculation.checksumSha512,
                "554677FCE42A7FB189BEF9A9B520183EF80A4EF828964FBA2D1C49C4A771D898CFFC6B1967FF706AB0EE48E5FAF22E7D340AD1FD751C5B99BD2CD9F51CCD3126");
        variables.put(BpmConstants.MessageDigestCalculation.checksumCrc32, "28cc2aae");
        variables.put(BpmConstants.MessageDigestCalculation.checksumMd5, "6d80ab7440370f3c68c75fa4642fb24a");

        variables.put(BpmConstants.ProcessVariables.ingestWorkflowId, INGEST_WORKFLOW_ID);
        variables.put(BpmConstants.ProcessVariables.ingestWorkflowExternalId, EXTERNAL_ID);
        variables.put(BpmConstants.ProcessVariables.sipProfileId, sipProfile.getId());
        variables.put(BpmConstants.ProcessVariables.sipVersion, SIP_VERSION);
        variables.put(BpmConstants.ProcessVariables.xmlVersion, XML_VERSION);
        variables.put(BpmConstants.ProcessVariables.assignee, user.getId());
        variables.put(BpmConstants.ProcessVariables.sipId, sip.getId());
        variables.put(BpmConstants.ProcessVariables.producerId, PRODUCER_ID);
        variables.put(BpmConstants.ProcessVariables.debuggingModeActive, true);

        variables.put(BpmConstants.Ingestion.originalSipFileName, SIP_FILE_NAME);
        variables.put(BpmConstants.Ingestion.filePathsAndFileSizes, filePathsToSizes);
        variables.put(BpmConstants.Ingestion.success, true);
        variables.put(BpmConstants.Ingestion.dateTime, "2018-05-11T10:27:00Z");
        variables.put(BpmConstants.Ingestion.sizeInBytes, 512312123L);
        variables.put(BpmConstants.Ingestion.authorialId, authorialPackage.getAuthorialId());
        variables.put(BpmConstants.Ingestion.rootDirFilesAndFixities, new ArrayList());

        variables.put(BpmConstants.Validation.validationProfileId, VALIDATION_PROFILE_ID);
        variables.put(BpmConstants.Validation.success, true);
        variables.put(BpmConstants.Validation.dateTime, "2018-05-11T10:27:17Z");

        variables.put(BpmConstants.FormatIdentification.toolName, "DROID");
        variables.put(BpmConstants.FormatIdentification.toolVersion, "DROID: version: 6.4, Signature files: 1. Type: Binary Version: 93 File name: DROID_SignatureFile_V93.xml 2. Type: Container Version: 20171130 File name: container-signature-20171130.xml");
        variables.put(BpmConstants.FormatIdentification.dateTime, "2018-05-11T10:27:27Z");
        variables.put(BpmConstants.FormatIdentification.mapOfFilesToFormats, identifiedFormats);

        variables.put(BpmConstants.FixityCheck.filePathsAndFixities, asList(file1, file2, file3));

        IngestIssue issue = new IngestIssue();
        issue.setSolvedByConfig(false);
        issue.setConfigNote("invalid config: [{}] at: /formatIdentification/pathsAndFormats " +
                "supported values: [List of pairs of paths to files and their respective file formats, e.g." +
                " [ {\"filePath\":\"this/is/a/filepath\", \"format\":\"fmt/101\"}, {\"filePath\":" +
                "\"this/is/another/filepath\", \"format\":\"fmt/993\"} ]]");
        issue.setTaskExecutor(BpmConstants.FormatIdentification.class);
        issue.setIssue("File at path: this/is/a/file/path was identified with multiple formats: {\n" +
                "format: format1, identification method: some method\n" +
                "format: format2, identification method: another method\n" +
                "}");
        issue.setIngestWorkflow(ingestWorkflow);
        ingestIssueStore.save(issue);

        IngestIssue issue2 = new IngestIssue();
        issue2.setSolvedByConfig(true);
        issue2.setConfigNote("\"used config: \" + false + \" at: \" + configPath \"/fixityCheck/continueOnInvalidChecksums");
        issue2.setTaskExecutor(BpmConstants.FixityCheck.class);
        issue2.setIssue("invalid checksum of files: /some/path/file");
        issue2.setIngestWorkflow(ingestWorkflow);
        ingestIssueStore.save(issue2);

        flushCache();

        startJob(PROCESS_INSTANCE_KEY, variables);

        Thread.sleep(3000);

        ingestWorkflow = ingestWorkflowStore.find(this.ingestWorkflow.getId());
        assertThat(this.ingestWorkflow.getProcessingState(), is(IngestWorkflowState.PROCESSED));

        Map<String, Object> aclibXmlIndexDocument = solrArclibXmlStore.findArclibXmlIndexDocument(EXTERNAL_ID);
        assertThat(aclibXmlIndexDocument, notNullValue());
    }
}
