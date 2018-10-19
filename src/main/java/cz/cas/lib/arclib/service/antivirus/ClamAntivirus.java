package cz.cas.lib.arclib.service.antivirus;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cas.lib.arclib.exception.bpm.CommandLineProcessException;
import cz.cas.lib.arclib.exception.bpm.IncidentException;
import cz.cas.lib.arclib.store.IngestIssueStore;
import cz.cas.lib.arclib.store.IngestWorkflowStore;
import cz.cas.lib.core.util.Utils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cz.cas.lib.core.util.Utils.executeProcessCustomResultHandle;
import static cz.cas.lib.core.util.Utils.notNull;

@Slf4j
public class ClamAntivirus extends Antivirus {

    private static final String PATH_TO_INFECTED_FILE_REGEX = "(.+): .+ FOUND";

    /**
     * command to be executed
     */
    @Getter
    private String[] cmd;

    public ClamAntivirus(String[] cmd, IngestIssueStore ingestIssueStore, IngestWorkflowStore ingestWorkflowStore, Path quarantinePath) {
        super.setIngestIssueStore(ingestIssueStore);
        super.setIngestWorkflowStore(ingestWorkflowStore);
        super.setQuarantinePath(quarantinePath);
        this.cmd = cmd;
    }

    /**
     * Scans SIP package for viruses.
     *
     * @param pathToSIP  absoulte path to SIP
     * @param externalId external id of the ingest workflow
     * @param configRoot JSON config
     * @return list with paths to infected files if threat was detected, empty list otherwise
     * @throws FileNotFoundException       if the SIP is not found
     * @throws CommandLineProcessException
     */
    @Override
    public void scan(Path pathToSIP, String externalId, JsonNode configRoot) throws FileNotFoundException, IncidentException {
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
        Utils.Pair<Integer, List<String>> result = executeProcessCustomResultHandle(fullCmd);
        switch (result.getL()) {
            case 0:
                log.info("no infected file found");
                break;
            case 1:
                Pattern pattern = Pattern.compile(PATH_TO_INFECTED_FILE_REGEX);
                result.getR().stream().forEach(
                        line -> {
                            Matcher matcher = pattern.matcher(line);
                            if (matcher.find()) {
                                log.info(Paths.get(matcher.group(1)) + " is infected");
                                infectedFiles.add(Paths.get(matcher.group(1)));
                            }
                        }
                );
                log.info(infectedFiles.size() + " infected file/s found");
                invokeInfectedFilesIssue(infectedFiles, externalId, configRoot, pathToSIP);
                break;
            default:
                throw new CommandLineProcessException("scan process error: " + String.join(System.lineSeparator(), result.getR()));
        }
    }

    public String getToolVersion() {
        return "CLAMAV version: " + executeProcessCustomResultHandle(cmd[0], "-V").getR();
    }
}
