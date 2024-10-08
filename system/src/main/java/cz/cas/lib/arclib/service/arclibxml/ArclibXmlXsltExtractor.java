package cz.cas.lib.arclib.service.arclibxml;

import cz.cas.lib.arclib.bpm.BpmConstants;
import cz.cas.lib.arclib.domain.profiles.SipProfile;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.service.SipProfileService;
import cz.cas.lib.core.store.Transactional;
import lombok.extern.slf4j.Slf4j;
import net.sf.saxon.jaxp.SaxonTransformerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static cz.cas.lib.core.util.Utils.listFilesMatchingRegex;

@Slf4j
@Service
public class ArclibXmlXsltExtractor {

    private SipProfileService sipProfileService;

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
    public String extractMetadata(String sipProfileExternalId, Map<String, Object> bpmVariables) throws TransformerException, IOException {
        SipProfile sipProfile = sipProfileService.findByExternalId(sipProfileExternalId);

        String externalId = (String) bpmVariables.get(BpmConstants.ProcessVariables.ingestWorkflowExternalId);
        Path sipFolderWorkspacePath = Paths.get((String) bpmVariables.get(BpmConstants.ProcessVariables.sipFolderWorkspacePath));

        String sipMetadataPathRegex = sipProfile.getSipMetadataPathRegex();
        Path pathToSipAbsolute = sipFolderWorkspacePath.toAbsolutePath();

        List<File> matchingFiles = listFilesMatchingRegex(new File(pathToSipAbsolute.toString()), sipMetadataPathRegex,true);
        if (matchingFiles.size() == 0)
            throw new GeneralException(String.format("File with metadata for ingest workflow with external id %s does not exist at path given by regex: %s", externalId, sipMetadataPathRegex));

        if (matchingFiles.size() > 1)
            throw new GeneralException(String.format("Multiple files found at the path given by regex: %s", sipMetadataPathRegex));

        File metadataFile = matchingFiles.get(0);
        String sipProfileXsl = sipProfile.getXsl();

        log.debug("Extracting metadata for SIP at path " + sipFolderWorkspacePath + " using SIP profile with id " + sipProfileExternalId + ".");


        TransformerFactory tf = SaxonTransformerFactory.newInstance();

        Transformer xsltProc = tf.newTransformer(new StreamSource(new ByteArrayInputStream(sipProfileXsl.getBytes())));
        xsltProc.setParameter(PATH_TO_SIP_XSLT_PARAM, pathToSipAbsolute.toString().replace("\\", "/") + "/");
        xsltProc.setParameter(SIP_METADATA_PATH_XSLT_PARAM, metadataFile.getPath()
                .replace("\\", "/"));

        //passing an empty document because of accessing the input documents directly from the template
        StringReader source = new StringReader("<xml/>");
        StringWriter result = new StringWriter();

        xsltProc.transform(new StreamSource(source), new StreamResult(result));

        log.debug("Metadata extraction for ingest workflow with external id " + externalId + " finished successfully");
        return result.toString();
    }

    @Autowired
    public void setSipProfileService(SipProfileService sipProfileService) {
        this.sipProfileService = sipProfileService;
    }
}
