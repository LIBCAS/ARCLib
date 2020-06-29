package cz.cas.lib.arclib.bpm;

import cz.cas.lib.arclib.domain.*;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflowState;
import cz.cas.lib.arclib.domain.packages.Sip;
import cz.cas.lib.arclib.index.IndexArclibXmlStore;
import cz.cas.lib.arclib.service.UserService;
import cz.cas.lib.arclib.service.arclibxml.ArclibXmlGenerator;
import cz.cas.lib.arclib.service.arclibxml.ArclibXmlValidator;
import cz.cas.lib.arclib.service.fixity.Sha512Counter;
import cz.cas.lib.arclib.store.ProducerStore;
import cz.cas.lib.arclib.store.SipStore;
import cz.cas.lib.arclib.utils.ArclibUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;

import static cz.cas.lib.core.util.Utils.bytesToHexString;

@Slf4j
@Service
public class ArclibXmlGeneratorDelegate extends ArclibDelegate {

    private IndexArclibXmlStore indexArclibXmlStore;
    private ArclibXmlGenerator arclibXmlGenerator;
    private Sha512Counter sha512Counter;
    private UserService userService;
    private ProducerStore producerStore;
    private ArclibXmlValidator validator;
    private SipStore sipStore;
    @Getter
    private String toolName = "ARCLib_" + IngestToolFunction.metadata_modification;

    /**
     * Generates ArclibXml according to the BPM variables stored in the <code>execution</code> and changes processingState of
     * ingest workflow to PROCESSED.
     */
    @Override
    public void executeArclibDelegate(DelegateExecution execution) throws DocumentException, IOException, ParserConfigurationException, SAXException {
        String ingestWorkflowExternalId = getIngestWorkflowExternalId(execution);
        log.debug("Execution of ArclibXml generator delegate started for ingest workflow " + ingestWorkflowExternalId + ".");

        String extractedMetadata = new String((byte[]) execution.getVariable(BpmConstants.MetadataExtraction.result));

        //prepend the metadata generated by Arclib
        Document arclibXml = arclibXmlGenerator.generateMetadata(extractedMetadata, execution.getVariables());
        String arclibXmlString = ArclibUtils.prettyPrint(arclibXml);

        //validate against ARCLib XML definition
        String authorialId = getStringVariable(execution, BpmConstants.Ingestion.authorialId);
        String sipId = getStringVariable(execution, BpmConstants.ProcessVariables.sipId);
        Sip sip = sipStore.find(sipId);
        Sip previousVersionSip = sip.getPreviousVersionSip();
        String previousVersionSipId = previousVersionSip != null ? previousVersionSip.getId() : ArclibXmlGenerator.INITIAL_VERSION;
        validator.validateArclibXml(new ByteArrayInputStream(arclibXmlString.getBytes()), sip.getId(), authorialId, sip.getVersionNumber(), previousVersionSipId);

        //store arclib xml to index
        String producerId = (String) execution.getVariable(BpmConstants.ProcessVariables.producerId);
        String responsiblePerson = (String) execution.getVariable(BpmConstants.ProcessVariables.responsiblePerson);
        User user = userService.find(responsiblePerson);
        Producer producer = producerStore.find(producerId);
        indexArclibXmlStore.createIndex(arclibXmlString.getBytes(), producer.getId(), producer.getName(), user.getUsername(), null, isInDebugMode(execution), true);
        String externalId = (String) execution.getVariable(BpmConstants.ProcessVariables.ingestWorkflowExternalId);
        Files.write(getAipXmlPath(execution), arclibXmlString.getBytes());

        log.debug("ArclibXml of IngestWorkflow with external id " + externalId + " has been indexed.");

        String arclibXmlHashValue = bytesToHexString(sha512Counter.computeDigest(new ByteArrayInputStream(arclibXmlString.getBytes())));
        Hash arclibXmlHash = new Hash(arclibXmlHashValue, HashType.Sha512);
        IngestWorkflow ingestWorkflow = ingestWorkflowService.findByExternalId(ingestWorkflowExternalId);
        ingestWorkflow.setArclibXmlHash(arclibXmlHash);
        log.debug("Hash of ARClibXml of Ingest workflow with external ID " + ingestWorkflowExternalId + ": "
                + arclibXmlHash.getHashValue() + ", hash type: " + arclibXmlHash.getHashType().name());

        ingestWorkflow.setProcessingState(IngestWorkflowState.PROCESSED);
        ingestWorkflowService.save(ingestWorkflow);
        log.info("State of ingest workflow with external id " + ingestWorkflowExternalId + " has changed to " +
                IngestWorkflowState.PROCESSED.toString() + ".");

        log.debug("Execution of ArclibXml generator delegate finished for ingest workflow " + ingestWorkflowExternalId + ".");
    }

    @Inject
    public void setSipStore(SipStore sipStore) {
        this.sipStore = sipStore;
    }

    @Inject
    public void setValidator(ArclibXmlValidator validator) {
        this.validator = validator;
    }

    @Inject
    public void setIndexArclibXmlStore(IndexArclibXmlStore indexArclibXmlStore) {
        this.indexArclibXmlStore = indexArclibXmlStore;
    }

    @Inject
    public void setArclibXmlGenerator(ArclibXmlGenerator arclibXmlGenerator) {
        this.arclibXmlGenerator = arclibXmlGenerator;
    }

    @Inject
    public void setSha512Counter(Sha512Counter sha512Counter) {
        this.sha512Counter = sha512Counter;
    }

    @Inject
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Inject
    public void setProducerStore(ProducerStore producerStore) {
        this.producerStore = producerStore;
    }
}
