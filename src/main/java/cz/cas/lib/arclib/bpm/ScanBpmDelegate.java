package cz.cas.lib.arclib.bpm;

import cz.cas.lib.arclib.antivirus.SIPAntivirusScanner;
import cz.cas.lib.arclib.antivirus.SIPAntivirusScannerException;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ScanBpmDelegate implements JavaDelegate {

    private SIPAntivirusScanner scanner;

    /**
     * Scans SIP package for viruses.
     * <p>
     * Task expects String path to SIP stored in process variable <i>pathToSip</i>.
     * Task sets process variable <i>infectedFiles</i>  with list with String paths to infected files.
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws SIPAntivirusScannerException if error occurs during the antivirus scan process
     */
    @Override
    public void execute(DelegateExecution execution) throws InterruptedException, SIPAntivirusScannerException, IOException {
        List<Path> infectedFiles = scanner.scan((String) execution.getVariable("pathToSip"));

        execution.setVariable("infectedFiles",
                infectedFiles.stream().map(
                        filePath -> filePath.toString()
                ).collect(Collectors.toList())
        );
    }

    @Inject
    public void setScanner(SIPAntivirusScanner scanner) {
        this.scanner = scanner;
    }
}
