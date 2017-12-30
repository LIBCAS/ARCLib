package cz.cas.lib.arclib.bpm;

import cz.cas.lib.arclib.formatidentifier.FormatIdentifier;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class FormatIdentifierBpmDelegate implements JavaDelegate {

    private FormatIdentifier formatIdentifier;

    /**
     * Performs the analysis of formats for the specified SIP.
     * <p>
     * Task expects id of the SIP stored in process variable <i>sipId</i>.
     * Task sets process variable <i>mapOfFilesToFormats</i> with a map of key-value pairs where the key is the path to a file from SIP and
     * value is a list of formats that have been identified for the respective file.
     *
     * @throws InterruptedException
     * @throws IOException
     */
    @Override
    public void execute(DelegateExecution execution) throws InterruptedException, IOException {
        String pathToSip = (String) execution.getVariable("pathToSip");

        Map<String, List<String>> mapOfFilesToFormats = formatIdentifier.analyze(pathToSip);
        execution.setVariable("mapOfFilesToFormats", mapOfFilesToFormats);
    }

    @Inject
    public void setFormatIdentifier(FormatIdentifier formatIdentifier) {
        this.formatIdentifier = formatIdentifier;
    }
}
