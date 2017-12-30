package cz.cas.lib.arclib.bpm;

import cz.cas.lib.arclib.fixity.SipFixityVerifier;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

@Component
public class FixityBpmDelegate implements JavaDelegate {

    private SipFixityVerifier verifier;

    /**
     * Verifies fixity of files specified in SIP META XML.
     * <p>
     * Expects <i>sipMetaPath</i> String variable to be set in process. This variable should contain path to SIP META XML.
     * Task sets process variable <i>invalidChecksumFiles</i>  with list with String paths to files with invalid checksum.
     *
     * @throws IOException
     */
    @Override
    public void execute(DelegateExecution execution) throws IOException {
        Path pathToSIP = Paths.get((String) execution.getVariable("sipMetaPath"));
        execution.setVariable("invalidChecksumFiles",
                verifier.verifySIP(pathToSIP).stream().map(
                        filePath -> filePath.toString()
                ).collect(Collectors.toList())
        );
    }

    @Inject
    public void setFixityVerifier(SipFixityVerifier verifier) {
        this.verifier = verifier;
    }
}