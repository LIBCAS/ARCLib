package cz.cas.lib.arclib.bpm;

import cz.cas.lib.arclib.antivirus.SIPAntivirusScanner;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class QuarantineBpmDelegate implements JavaDelegate {

    private SIPAntivirusScanner scanner;

    /**
     * Moves infected files to quarantine.
     * <p>
     * Task expects that list with String paths to infected files is stored in process variable <i>infectedFiles</i>.
     */
    @Override
    public void execute(DelegateExecution execution) throws IOException {
        List<String> infectedFiles = (List<String>) execution.getVariable("infectedFiles");
        scanner.moveToQuarantine(infectedFiles.stream().map(
                pathString -> Paths.get(pathString)
        ).collect(Collectors.toList()));
    }

    @Inject
    public void setScanner(SIPAntivirusScanner scanner) {
        this.scanner = scanner;
    }
}
