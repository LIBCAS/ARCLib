package cz.cas.lib.arclib.bpm.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import cz.cas.lib.arclib.bpm.ArclibXmlExtractorDelegate;
import cz.cas.lib.arclib.bpm.ArclibXmlGeneratorDelegate;
import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.domain.*;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestEvent;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestIssue;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflowState;
import cz.cas.lib.arclib.domain.packages.AuthorialPackage;
import cz.cas.lib.arclib.domain.packages.FolderStructure;
import cz.cas.lib.arclib.domain.packages.Sip;
import cz.cas.lib.arclib.domain.preservationPlanning.IngestIssueDefinition;
import cz.cas.lib.arclib.domain.preservationPlanning.IngestIssueDefinitionCode;
import cz.cas.lib.arclib.domain.preservationPlanning.Tool;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.domain.profiles.SipProfile;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlDocument;
import cz.cas.lib.arclib.index.solr.arclibxml.SolrArclibXmlStore;
import cz.cas.lib.arclib.security.user.UserDetailsImpl;
import cz.cas.lib.arclib.service.IngestIssueService;
import cz.cas.lib.arclib.service.IngestWorkflowService;
import cz.cas.lib.arclib.service.SipProfileService;
import cz.cas.lib.arclib.service.UserService;
import cz.cas.lib.arclib.service.arclibxml.ArclibXmlGenerator;
import cz.cas.lib.arclib.service.arclibxml.ArclibXmlValidator;
import cz.cas.lib.arclib.service.arclibxml.ArclibXmlXsltExtractor;
import cz.cas.lib.arclib.service.fixity.MetsChecksumType;
import cz.cas.lib.arclib.service.fixity.Sha512Counter;
import cz.cas.lib.arclib.service.formatIdentification.FormatIdentificationToolType;
import cz.cas.lib.arclib.service.preservationPlanning.ToolService;
import cz.cas.lib.arclib.store.*;
import cz.cas.lib.arclib.utils.ArclibUtils;
import cz.cas.lib.core.sequence.Generator;
import cz.cas.lib.core.sequence.Sequence;
import cz.cas.lib.core.sequence.SequenceStore;
import cz.cas.lib.core.store.Transactional;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.dom4j.DocumentException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static cz.cas.lib.core.util.Utils.*;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Deployment(resources = {"bpmn/arclibXmlExtractor.bpmn", "bpmn/arclibXmlGenerator.bpmn"})
public class ArclibXmlGeneratorDelegateTest extends DelegateTest {

    private static final String PROCESS_INSTANCE_KEY_EXT = "arclibXmlExtractorProcess";
    private static final String PROCESS_INSTANCE_KEY_GEN = "arclibXmlGeneratorProcess";
    private static final String PATH_TO_METS = "mets_7033d800-0935-11e4-beed-5ef3fc9ae867.xml";
    private static final String SIP_FILE_NAME = SIP.getFileName().toString();

    private static final Integer SIP_VERSION = 2;
    private static final Integer XML_VERSION = 3;
    private static final String PRODUCER_ID = "2354235FD2";
    private static final String FILE_NAME = "7033d800-0935-11e4-beed-5ef3fc9ae867";

    private static final String SIP_HASH = "50FDE99373B04363727473D00AE938A4F4DEBFD0AFB1D428337D81905F6863B3CC303BB3" +
            "31FFB3361085C3A6A2EF4589FF9CD2014C90CE90010CD3805FA5FBC6";

    String filePath1 = "amdsec/amd_mets_7033d800-0935-11e4-beed-5ef3fc9ae867_0005.xml";
    String filePath2 = "amdsec/amd_mets_7033d800-0935-11e4-beed-5ef3fc9ae867_0006.xml";
    String filePath3 = "amdsec/amd_mets_7033d800-0935-11e4-beed-5ef3fc9ae867_0007.xml";

    private IngestEvent fixityGenerationEvent;
    private IngestEvent formatIdentificationEvent;
    private IngestEvent fixityCheckEvent;
    private ArclibXmlXsltExtractor extractor;
    private ArclibXmlGenerator arclibXmlGenerator;
    private Generator generator;
    private Sha512Counter sha512Counter;
    private IngestWorkflowStore ingestWorkflowStore;
    private SipProfileService sipProfileService;
    private SipStore sipStore;
    private ProducerProfileStore producerProfileStore;
    private ProducerStore producerStore;
    private AuthorialPackageStore authorialPackageStore;
    private BatchStore batchStore;
    private SolrArclibXmlStore indexedArclibXmlStore;
    private UserStore userStore;
    private AipQueryStore aipQueryStore;
    private IngestIssueStore ingestIssueStore;
    private SequenceStore sequenceStore;
    private HashStore hashStore;
    private ToolStore toolStore;
    private SipProfileStore sipProfileStore;

    @Mock
    private ArclibXmlValidator validator;
    private IngestEventStore ingestEventStore;
    @Mock
    private UserService userService;
    @Mock
    private ProducerStore mockedProducerStore;
    private IngestIssueDefinitionStore ingestIssueDefinitionStore;
    private IngestIssueService ingestIssueService;
    private ToolService toolService;
    private ArclibXmlGeneratorDelegate arclibXmlGeneratorDelegate;
    private ArclibXmlExtractorDelegate arclibXmlExtractorDelegate;
    private SipProfile sipProfile;
    private IngestWorkflow ingestWorkflow;
    private IngestWorkflow relatedIngestWorkflow;
    private Batch batch;
    private UserDetailsImpl userDetailsImpl;
    private User user;
    private Sip sip;
    private Sip previousVersionSip;
    private AuthorialPackage authorialPackage;
    private static final String ARCLIB_XML_INDEX_CONFIG = "index/arclibXmlIndexConfig.csv";


    @Before
    @Transactional
    public void before() throws IOException, DocumentException {
        hashStore = new HashStore();
        MockitoAnnotations.initMocks(this);

        aipQueryStore = new AipQueryStore();
        sequenceStore = new SequenceStore();

        indexedArclibXmlStore = new SolrArclibXmlStore();
        indexedArclibXmlStore.setCoreName(arclibXmlCoreName);
        indexedArclibXmlStore.setSolrClient(getClient());
        indexedArclibXmlStore.setUris("http://www.loc.gov/METS/",
                "http://www.w3.org/2001/XMLSchema-instance",
                "http://arclib.lib.cas.cz/ARCLIB_XSD",
                "info:lc/xmlns/premis-v2",
                "http://www.openarchives.org/OAI/2.0/oai_dc/",
                "http://purl.org/dc/elements/1.1/",
                "http://www.w3.org/1999/xlink");
        indexedArclibXmlStore.setArclibXmlIndexConfig(new ClassPathResource(ARCLIB_XML_INDEX_CONFIG));
        indexedArclibXmlStore.init();

        userStore = new UserStore();
        ingestWorkflowStore = new IngestWorkflowStore();
        sipProfileService = new SipProfileService();
        sipStore = new SipStore();
        authorialPackageStore = new AuthorialPackageStore();
        producerStore = new ProducerStore();
        ingestIssueDefinitionStore = new IngestIssueDefinitionStore();
        batchStore = new BatchStore();
        producerProfileStore = new ProducerProfileStore();
        toolStore = new ToolStore();
        ingestIssueStore = new IngestIssueStore();
        sipProfileStore = new SipProfileStore();
        ingestEventStore = new IngestEventStore();
        initializeStores(hashStore, toolStore, ingestIssueDefinitionStore, sequenceStore, ingestIssueStore, aipQueryStore, ingestWorkflowStore, sipProfileStore,
                sipStore, batchStore, producerProfileStore, producerStore, authorialPackageStore, userStore, ingestEventStore);

        ingestIssueService = new IngestIssueService();
        ingestIssueService.setIngestIssueStore(ingestIssueStore);

        toolService = new ToolService();
        toolService.setToolStore(toolStore);

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

        Sequence sequence3 = new Sequence();
        sequence3.setCounter(2L);
        sequence3.setFormat("'#'#");
        sequence3.setId(ingestIssueDefinitionStore.getSEQUENCE_ID());
        sequenceStore.save(sequence3);

        Sequence sequence4 = new Sequence();
        sequence4.setCounter(2L);
        sequence4.setFormat("'#'#");
        sequence4.setId(sipProfileStore.getSEQUENCE_ID());
        sequenceStore.save(sequence4);

        producerProfileStore.setGenerator(generator);
        ingestWorkflowStore.setGenerator(generator);
        ingestIssueDefinitionStore.setGenerator(generator);
        sipProfileStore.setGenerator(generator);

        extractor = new ArclibXmlXsltExtractor();
        extractor.setSipProfileService(sipProfileService);

        arclibXmlGenerator = new ArclibXmlGenerator();
        arclibXmlGenerator.setUris("http://www.loc.gov/METS/",
                "http://www.w3.org/2001/XMLSchema-instance",
                "http://arclib.lib.cas.cz/ARCLIB_XSD",
                "info:lc/xmlns/premis-v2",
                "http://www.openarchives.org/OAI/2.0/oai_dc/",
                "http://purl.org/dc/elements/1.1/",
                "http://www.w3.org/1999/xlink"
        );
        arclibXmlGenerator.setArclibVersion("1.0");
        arclibXmlGenerator.setIngestWorkflowStore(ingestWorkflowStore);
        arclibXmlGenerator.setIngestEventStore(ingestEventStore);

        user = new User();
        userDetailsImpl = new UserDetailsImpl(user);

        arclibXmlExtractorDelegate = new ArclibXmlExtractorDelegate();
        arclibXmlExtractorDelegate.setObjectMapper(new ObjectMapper());
        arclibXmlExtractorDelegate.setWorkspace(WS.toString());
        arclibXmlExtractorDelegate.setArclibXmlXsltExtractor(extractor);
        arclibXmlExtractorDelegate.setIngestEventStore(ingestEventStore);
        IngestWorkflowService ingestWorkflowService = new IngestWorkflowService();
        ingestWorkflowService.setStore(ingestWorkflowStore);
        arclibXmlExtractorDelegate.setIngestWorkflowService(ingestWorkflowService);
        arclibXmlExtractorDelegate.setToolService(toolService);

        arclibXmlGeneratorDelegate = new ArclibXmlGeneratorDelegate();
        arclibXmlGeneratorDelegate.setIngestEventStore(ingestEventStore);
        arclibXmlGeneratorDelegate.setObjectMapper(new ObjectMapper());
        arclibXmlGeneratorDelegate.setWorkspace(WS.toString());
        arclibXmlGeneratorDelegate.setArclibXmlGenerator(arclibXmlGenerator);
        arclibXmlGeneratorDelegate.setIngestWorkflowService(ingestWorkflowService);
        arclibXmlGeneratorDelegate.setSha512Counter(sha512Counter);
        arclibXmlGeneratorDelegate.setToolService(toolService);
        arclibXmlGeneratorDelegate.setSipStore(sipStore);
        arclibXmlGeneratorDelegate.setValidator(validator);

        sipProfile = new SipProfile();
        String sipProfileXml = Resources.toString(this.getClass().getResource(
                "/sipProfiles/comprehensiveSipProfile.xsl"), StandardCharsets.UTF_8);
        sipProfile.setXsl(sipProfileXml);
        sipProfile.setSipMetadataPathRegex(PATH_TO_METS);
        sipProfileService.setStore(sipProfileStore);
        sipProfileService.save(sipProfile);

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
        sip.setSizeInBytes(566L);
        sipStore.save(sip);

        relatedIngestWorkflow = new IngestWorkflow();
        ingestWorkflowStore.save(relatedIngestWorkflow);

        ingestWorkflow = new IngestWorkflow();
        ingestWorkflow.setSip(sip);
        ingestWorkflow.setId(INGEST_WORKFLOW_ID);
        ingestWorkflow.setFileName(SIP_FILE_NAME);
        ingestWorkflow.setBatch(batch);
        ingestWorkflow.setRelatedWorkflow(relatedIngestWorkflow);
        ingestWorkflow.setExternalId(EXTERNAL_ID);
        ingestWorkflow.setVersioningLevel(VersioningLevel.NO_VERSIONING);
        ingestWorkflow.setProcessingState(IngestWorkflowState.PROCESSED);
        ingestWorkflow.setXmlVersionNumber(XML_VERSION);
        ingestWorkflowStore.save(ingestWorkflow);

        when(userService.find(any())).thenReturn(user);
        when(mockedProducerStore.find(any())).thenReturn(producer);

        Tool arclibXmlExtractorTool = new Tool();
        arclibXmlExtractorTool.setInternal(true);
        arclibXmlExtractorTool.setToolFunction(IngestToolFunction.metadata_extraction);
        arclibXmlExtractorTool.setName(arclibXmlExtractorDelegate.getToolName());
        toolStore.save(arclibXmlExtractorTool);

        Mocks.register("arclibXmlExtractorDelegate", arclibXmlExtractorDelegate);
        Mocks.register("arclibXmlGeneratorDelegate", arclibXmlGeneratorDelegate);

        Tool fixityGenerationTool = new Tool();
        fixityGenerationTool.setInternal(true);
        fixityGenerationTool.setToolFunction(IngestToolFunction.message_digest_calculation);
        toolStore.save(fixityGenerationTool);

        fixityGenerationEvent = new IngestEvent();
        fixityGenerationEvent.setSuccess(true);
        fixityGenerationEvent.setTool(fixityGenerationTool);
        fixityGenerationEvent.setIngestWorkflow(ingestWorkflow);
        ingestEventStore.save(fixityGenerationEvent);

        Tool fixityCheckTool = new Tool();
        fixityCheckTool.setInternal(true);
        fixityCheckTool.setToolFunction(IngestToolFunction.fixity_check);
        toolStore.save(fixityCheckTool);

        fixityCheckEvent = new IngestEvent();
        fixityCheckEvent.setSuccess(true);
        fixityCheckEvent.setTool(fixityCheckTool);
        fixityCheckEvent.setIngestWorkflow(ingestWorkflow);
        ingestEventStore.save(fixityCheckEvent);
    }

    @Test
    public void testArclibXmlGeneratorDelegate() throws InterruptedException, IOException {
        Map<String, Object> variables = asMap(BpmConstants.ProcessVariables.batchId, batch.getId());

        TreeMap<String, Pair<String, String>> identifiedFormats = new TreeMap<>();

        identifiedFormats.put(filePath1, Pair.of("fmt/248", "extension"));
        identifiedFormats.put(filePath2, Pair.of("x-fmt/111", "content"));
        identifiedFormats.put(filePath3, Pair.of("fmt/248", "extension"));

        HashMap<String, Map<String, Triple<Long, String, String>>> fixityGeneratorEventMap = new HashMap<>();
        Map<String, Triple<Long, String, String>> fixityGenerationResultMap = new HashMap<>();
        fixityGenerationResultMap.put(filePath1, Triple.of(23523423L, MetsChecksumType.MD5.toString(), "2a6d072e0f741b52148abe308bb506d8"));
        fixityGenerationResultMap.put(filePath2, Triple.of(5342235L, MetsChecksumType.MD5.toString(), "d906978fbcd8c9488d604ccc30f323fd"));
        fixityGenerationResultMap.put(filePath3, Triple.of(632432L, MetsChecksumType.MD5.toString(), "e4b3c5adf26ebe80cf0a718be5ace66f"));
        fixityGeneratorEventMap.put(fixityGenerationEvent.getId(), fixityGenerationResultMap);

        Map<String, String> mapOfEventIdsToSha512Calculations = new HashMap<>();
        mapOfEventIdsToSha512Calculations.put(fixityGenerationEvent.getId(), "554677FCE42A7FB189BEF9A9B520183EF80A4EF828964FBA2D1C49C4A771D898CFFC6B1967FF706AB0EE48E5FAF22E7D340AD1FD751C5B99BD2CD9F51CCD3126");
        variables.put(BpmConstants.FixityGeneration.mapOfEventIdsToSipSha512, mapOfEventIdsToSha512Calculations);

        Map<String, String> mapOfEventIdsToCrc32Calculations = new HashMap<>();
        mapOfEventIdsToCrc32Calculations.put(fixityGenerationEvent.getId(), "28cc2aae");
        variables.put(BpmConstants.FixityGeneration.mapOfEventIdsToSipCrc32, mapOfEventIdsToCrc32Calculations);

        Map<String, String> mapOfEventIdsToMd5Calculations = new HashMap<>();
        mapOfEventIdsToMd5Calculations.put(fixityGenerationEvent.getId(), "6d80ab7440370f3c68c75fa4642fb24a");
        variables.put(BpmConstants.FixityGeneration.mapOfEventIdsToSipMd5, mapOfEventIdsToMd5Calculations);

        variables.put(BpmConstants.ProcessVariables.ingestWorkflowId, INGEST_WORKFLOW_ID);
        variables.put(BpmConstants.ProcessVariables.ingestWorkflowExternalId, EXTERNAL_ID);
        variables.put(BpmConstants.ProcessVariables.sipVersion, SIP_VERSION);
        variables.put(BpmConstants.ProcessVariables.xmlVersion, XML_VERSION);
        variables.put(BpmConstants.ProcessVariables.responsiblePerson, user.getId());
        variables.put(BpmConstants.ProcessVariables.sipId, sip.getId());
        variables.put(BpmConstants.ProcessVariables.producerId, PRODUCER_ID);
        variables.put(BpmConstants.ProcessVariables.debuggingModeActive, true);

        variables.put(BpmConstants.ProcessVariables.sipFileName, SIP_FILE_NAME);
        variables.put(BpmConstants.ProcessVariables.extractedAuthorialId, authorialPackage.getAuthorialId());
        variables.put(BpmConstants.ProcessVariables.latestConfig, String.format("{\"%s\":\"%s\"}", ArclibXmlExtractorDelegate.SIP_PROFILE_CONFIG_ENTRY, sipProfile.getExternalId()));

        variables.put(BpmConstants.FixityGeneration.preferredFixityGenerationEventId, fixityGenerationEvent.getId());

        Tool droidTool = new Tool();
        droidTool.setName(FormatIdentificationToolType.DROID.toString());
        droidTool.setInternal(false);
        droidTool.setToolFunction(IngestToolFunction.format_identification);
        droidTool.setVersion("DROID: version: 6.4, Signature files: 1. Type: Binary Version: 93 File name: DROID_SignatureFile_V93.xml 2. Type: Container Version: 20171130 File name: container-signature-20171130.xml");
        toolService.save(droidTool);

        formatIdentificationEvent = new IngestEvent();
        formatIdentificationEvent.setTool(droidTool);
        formatIdentificationEvent.setSuccess(true);
        formatIdentificationEvent.setIngestWorkflow(ingestWorkflow);
        ingestEventStore.save(formatIdentificationEvent);

        Map<String, TreeMap<String, Pair<String, String>>> mapOfEventIdsToMapsOfFilesToFormats = new HashMap<>();
        mapOfEventIdsToMapsOfFilesToFormats.put(formatIdentificationEvent.getId(), identifiedFormats);

        variables.put(BpmConstants.FormatIdentification.mapOfEventIdsToMapsOfFilesToFormats, mapOfEventIdsToMapsOfFilesToFormats);
        variables.put(BpmConstants.FormatIdentification.preferredFormatIdentificationEventId, formatIdentificationEvent.getId());

        variables.put(BpmConstants.ProcessVariables.sipFolderWorkspacePath, SIP.toAbsolutePath().toString());
        variables.put(BpmConstants.FixityCheck.fixityCheckToolCounter, 0);

        IngestIssueDefinition def = new IngestIssueDefinition();
        def.setName("def");
        def.setCode(IngestIssueDefinitionCode.FILE_FORMAT_UNRESOLVABLE);
        ingestIssueDefinitionStore.save(def);

        IngestIssue issue = new IngestIssue();
        issue.setSuccess(false);
        issue.setDescription("File at path: this/is/a/file/path was identified with multiple formats: {\n" +
                "format: format1, identification method: some method\n" +
                "format: format2, identification method: another method\n" +
                "}");
        issue.setTool(droidTool);
        issue.setIngestWorkflow(ingestWorkflow);
        issue.setIngestIssueDefinition(def);
        ingestIssueStore.save(issue);

        IngestIssue issue2 = new IngestIssue();
        issue2.setSuccess(true);
        issue2.setTool(droidTool);
        issue2.setDescription("\"used config: \" + false + \" at: \" + configPath \"/fixityCheck/continueOnInvalidChecksums");
        issue2.setIngestWorkflow(ingestWorkflow);
        issue2.setIngestIssueDefinition(def);
        ingestIssueStore.save(issue2);

        flushCache();

        startJob(PROCESS_INSTANCE_KEY_EXT, variables);
        Thread.sleep(3000);
        List<HistoricVariableInstance> list = rule
                .getHistoryService()
                .createHistoricVariableInstanceQuery()
                .variableName(BpmConstants.MetadataExtraction.result)
                .list();
        assertThat(list.size(), is(1));
        assertThat(list.get(0).getValue(), notNullValue());
        byte[] extract = (byte[]) list.get(0).getValue();
        variables.put(BpmConstants.MetadataExtraction.result, extract);
        variables.put(BpmConstants.FixityGeneration.preferredFixityGenerationEventId, fixityGenerationEvent.getId());
        variables.put(BpmConstants.FixityGeneration.mapOfEventIdsToSipContentFixityData, fixityGeneratorEventMap);
        startJob(PROCESS_INSTANCE_KEY_GEN, variables);
        Thread.sleep(3000);

        ingestWorkflow = ingestWorkflowStore.find(this.ingestWorkflow.getId());
        assertThat(this.ingestWorkflow.getProcessingState(), is(IngestWorkflowState.PROCESSED));

        IndexedArclibXmlDocument doc = indexedArclibXmlStore.findArclibXmlIndexDocument(EXTERNAL_ID);
        assertThat(doc.getFields(), notNullValue());
        byte[] xmlFromTmpStorage = Files.readAllBytes(ArclibUtils.getAipXmlWorkspacePath(ingestWorkflow.getExternalId(), WS.toString()));
        assertThat(xmlFromTmpStorage.length, greaterThan(0));

    }
}
