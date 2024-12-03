package cz.cas.lib.arclib.service.antivirus;

import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.exception.bpm.CommandLineProcessException;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import cz.cas.lib.arclib.service.ExternalProcessRunner;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cz.cas.lib.core.util.Utils.notNull;

@Slf4j
public class ClamAntivirus extends Antivirus {

    private static final String PATH_TO_INFECTED_FILE_REGEX = "(.+): .+ FOUND";
    public static final String ANTIVIRUS_NAME = AntivirusType.CLAMAV.toString();
    private static final Pattern SHORT_TOOL_VERSION_PATTERN = Pattern.compile(".*(ClamAV [\\d,\\.]+).*");

    /**
     * command to be executed
     */
    @Getter
    private String[] cmd;
    private ExternalProcessRunner externalProcessRunner;

    public ClamAntivirus(ExternalProcessRunner externalProcessRunner, String[] cmd) {
        this.externalProcessRunner = externalProcessRunner;
        this.cmd = cmd;
    }

    /**
     * Scans SIP package for viruses.
     *
     * @param pathToSIP absoulte path to SIP
     * @param iw        external ingest workflow
     * @throws FileNotFoundException       if the SIP is not found
     * @throws CommandLineProcessException
     */
    @Override
    public void scan(Path pathToSIP, IngestWorkflow iw) throws FileNotFoundException, IncidentException {
        log.info("scanning file at path: " + pathToSIP);
        notNull(pathToSIP, () -> {
            throw new IllegalArgumentException("null path to SIP package");
        });
        if (!pathToSIP.toFile().exists())
            throw new FileNotFoundException("no file/folder found at: " + pathToSIP);
        String[] fullCmd = Arrays.copyOf(cmd, cmd.length + 1);
        fullCmd[cmd.length] = pathToSIP.toString();

        log.info("running '" + String.join(" ", fullCmd) + "' process");
        List<Path> infectedFiles = new ArrayList<>();
        Pair<Integer, List<String>> result = externalProcessRunner.executeProcessCustomResultHandle(true, fullCmd);
        switch (result.getLeft()) {
            case 0:
                log.info("no infected file found");
                break;
            case 1:
                Pattern pattern = Pattern.compile(PATH_TO_INFECTED_FILE_REGEX);
                result.getRight().stream().forEach(
                        line -> {
                            Matcher matcher = pattern.matcher(line);
                            if (matcher.find()) {
                                log.info(Paths.get(matcher.group(1)) + " is infected");
                                infectedFiles.add(Paths.get(matcher.group(1)));
                            }
                        }
                );
                log.info(infectedFiles.size() + " infected file/s found");
                invokeInfectedFilesIssue(infectedFiles, iw, pathToSIP);
                break;
            default:
                throw new CommandLineProcessException("scan process error: " + String.join(System.lineSeparator(), result.getRight()));
        }
    }

    @Override
    public String getToolName() {
        return ANTIVIRUS_NAME;
    }

    //e.g. 'CLAMAV version: [ClamAV 0.100.2/25043/Tue Oct 16 23:06:18 2018]'
    @Override
    public String getToolVersion() {
        Pair<Integer, List<String>> result = externalProcessRunner.executeProcessCustomResultHandle(false, cmd[0], "-V");
        if (result.getLeft() != 0)
            throw new IllegalStateException("CLAMAV version CMD has failed: " + result.getRight());
        return "" + result.getRight();
    }

    //e.g. 'CLAMAV version: ClamAV 0.100.2'
    @Override
    public String getShortToolVersion() {
        String longToolVersion = getToolVersion();
        Matcher matcher = SHORT_TOOL_VERSION_PATTERN.matcher(longToolVersion);
        boolean matches = matcher.matches();
        return matches ? matcher.group(1) : longToolVersion;
    }
}
