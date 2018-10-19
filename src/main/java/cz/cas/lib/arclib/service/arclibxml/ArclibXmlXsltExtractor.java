package cz.cas.lib.arclib.service.arclibxml;

import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.domain.profiles.SipProfile;
import cz.cas.lib.arclib.store.SipProfileStore;
import cz.cas.lib.arclib.utils.ArclibUtils;
import cz.cas.lib.core.store.Transactional;
import lombok.extern.slf4j.Slf4j;
import net.sf.saxon.lib.FeatureKeys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Map;

@Slf4j
@Service
public class ArclibXmlXsltExtractor {

    private SipProfileStore sipProfileStore;
    private String workspace;

    private static final String PATH_TO_SIP_XSLT_PARAM = "pathToSip";
    private static final String SIP_METADATA_PATH_XSLT_PARAM = "sipMetadataPath";

    /**
     * Generates ARCLib XML from SIP using the SIP profile
     *
     * @param bpmVariables BPM execution variables
     * @return extracted metadata
     * @throws TransformerException extraction of metadata from SIP using XSLT failed
     */
    @Transactional
    public String extractMetadata(Map<String, Object> bpmVariables) throws TransformerException {
        String sipProfileId = (String) bpmVariables.get(BpmConstants.ProcessVariables.sipProfileId);
        SipProfile sipProfile = sipProfileStore.find(sipProfileId);

        String sipMetadataPath = sipProfile.getSipMetadataPath();
        String sipProfileXsd = sipProfile.getXsl();

        String externalId = (String) bpmVariables.get(BpmConstants.ProcessVariables.ingestWorkflowExternalId);

        String originalSipFileName = (String) bpmVariables.get(BpmConstants.Ingestion.originalSipFileName);
        Path pathToSip = ArclibUtils.getSipFolderWorkspacePath(externalId, workspace, originalSipFileName);

        log.info("Extracting metadata for SIP at path " + pathToSip + " using SIP profile with id " + sipProfileId + ".");

        TransformerFactory tf = javax.xml.transform.TransformerFactory.newInstance();
        /*
         Otherwise the Release 2.0.9.S01.E1 provokes this warning
         "SXXP0005: The source document is in namespace http://formex.publications.europa.eu/ted/schema/export/R2.0.9.S01.E01, but none of the
         template rules match elements in this namespace (Use --suppressXsltNamespaceCheck:on to avoid this warning)
         See error explanation in {@URL https://stackoverflow.com/questions/33256226/warning-messages-appeared-after-upgrade-saxon-to-9-5-1-8}.
         Transformation XSL-Ts should be independent of the XSD Schema or select specific XSL-T per Schema, which right now is not the  case.
         */
        tf.setFeature(FeatureKeys.SUPPRESS_XSLT_NAMESPACE_CHECK, true);

        Transformer xsltProc = tf.newTransformer(new StreamSource(new ByteArrayInputStream(sipProfileXsd.getBytes())));

        xsltProc.setParameter(PATH_TO_SIP_XSLT_PARAM, pathToSip.toAbsolutePath().toString().replace("\\", "/") + "/");
        xsltProc.setParameter(SIP_METADATA_PATH_XSLT_PARAM, sipMetadataPath);

        //passing an empty document because of accessing the input documents directly from the template
        StringReader source = new StringReader("<xml/>");
        StringWriter result = new StringWriter();

        xsltProc.transform(new StreamSource(source), new StreamResult(result));

        log.info("Metadata extraction for ingest workflow with external id " + externalId + " finished successfully");
        return result.toString();
    }

    @Inject
    public void setSipProfileStore(SipProfileStore sipProfileStore) {
        this.sipProfileStore = sipProfileStore;
    }

    @Inject
    public void setWorkspace(@Value("${arclib.path.workspace}") String workspace) {
        this.workspace = workspace;
    }
}
