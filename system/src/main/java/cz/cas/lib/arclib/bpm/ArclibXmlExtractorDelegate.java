package cz.cas.lib.arclib.bpm;

import cz.cas.lib.arclib.domain.IngestToolFunction;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestEvent;
import cz.cas.lib.arclib.service.IngestWorkflowService;
import cz.cas.lib.arclib.service.arclibxml.ArclibXmlXsltExtractor;
import cz.cas.lib.arclib.service.preservationPlanning.ToolService;
import cz.cas.lib.arclib.store.IngestEventStore;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.xml.transform.TransformerException;
import java.io.IOException;

@Slf4j
@Service
public class ArclibXmlExtractorDelegate extends ArclibDelegate {

    @Getter
    private String toolName = "ARCLib_" + IngestToolFunction.metadata_extraction;
    private ArclibXmlXsltExtractor arclibXmlXsltExtractor;

    @Override
    public void executeArclibDelegate(DelegateExecution execution) throws TransformerException, IOException {
        String ingestWorkflowExternalId = getIngestWorkflowExternalId(execution);
        log.debug("Execution of ArclibXml extractor delegate started for ingest workflow " + ingestWorkflowExternalId + ".");

        //extract metadata from original SIP using XSLT
        String extractedMetadata = arclibXmlXsltExtractor.extractMetadata(execution.getVariables());
        execution.setVariable(BpmConstants.MetadataExtraction.result, extractedMetadata.getBytes());
        ingestEventStore.save(new IngestEvent(ingestWorkflowService.findByExternalId(ingestWorkflowExternalId), toolService.findByNameAndVersion(getToolName(), getToolVersion()), true, null));

        log.debug("Execution of ArclibXml extractor delegate finished for ingest workflow " + ingestWorkflowExternalId + ".");
    }

    @Inject
    public void setArclibXmlXsltExtractor(ArclibXmlXsltExtractor arclibXmlXsltExtractor) {
        this.arclibXmlXsltExtractor = arclibXmlXsltExtractor;
    }
}
