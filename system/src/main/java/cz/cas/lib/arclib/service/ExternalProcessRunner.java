package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class ExternalProcessRunner {
    private int timeoutSigterm;
    private int timeoutSigkill;

    public void executeProcessDefaultResultHandle(String... cmd) {
        String processLogString = Arrays.toString(cmd);
        File tmp = null;
        try {
            tmp = File.createTempFile("out", null);
            tmp.deleteOnExit();
            final ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            processBuilder.redirectErrorStream(true).redirectOutput(tmp);
            final Process process = processBuilder.start();
            final int exitCode = runProcessWithTimeout(process,processLogString);
            if (exitCode != 0)
                throw new IllegalStateException("Process: " + processLogString + " has failed " + Files.readAllLines(tmp.toPath()));
        } catch (IOException ex) {
            throw new GeneralException("unexpected error while executing process: " + processLogString, ex);
        } finally {
            if (tmp != null)
                tmp.delete();
        }
    }

    /**
     * @param mergeOutputs merge stdout and stderr outputs
     * @param cmd          cmd to execute
     * @return Pair with return code as key and output (list of lines) as value
     */
    public Pair<Integer, List<String>> executeProcessCustomResultHandle(boolean mergeOutputs, String... cmd) {
        File stdFile = null;
        File errFile = null;
        String processLogString = Arrays.toString(cmd);
        try {
            stdFile = File.createTempFile("std.out", null);
            errFile = File.createTempFile("err.out", null);
            final ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            if (mergeOutputs)
                processBuilder.redirectErrorStream(true);
            else
                processBuilder.redirectError(errFile);
            processBuilder.redirectOutput(stdFile);
            final Process process = processBuilder.start();
            final int exitCode = runProcessWithTimeout(process,processLogString);
            List<String> output;
            if (mergeOutputs || exitCode == 0)
                output = Files.readAllLines(stdFile.toPath());
            else
                output = Files.readAllLines(errFile.toPath());
            return Pair.of(exitCode, output);
        } catch (IOException ex) {
            throw new GeneralException("unexpected error while executing process", ex);
        } finally {
            if (stdFile != null)
                stdFile.delete();
            if (errFile != null)
                errFile.delete();
        }
    }

    private int runProcessWithTimeout(Process process, String processLogString) {
        try {
            boolean processFinished = process.waitFor(timeoutSigterm, TimeUnit.SECONDS);
            if (!processFinished) {
                process.destroy();
                boolean sigtermFinished = process.waitFor(timeoutSigkill, TimeUnit.SECONDS);
                if (!sigtermFinished) {
                    process.destroyForcibly();
                    process.waitFor();
                }
                throw new GeneralException("process killed as it reached timeout: " + processLogString);
            } else {
                return process.exitValue();
            }
        } catch (InterruptedException ex) {
            throw new GeneralException("unexpected error while executing process: " + processLogString, ex);
        }
    }

    @Autowired
    public void setTimeoutSigterm(@Value("${arclib.externalProcess.timeout.sigterm}") int timeoutSigterm) {
        this.timeoutSigterm = timeoutSigterm;
    }

    @Autowired
    public void setTimeoutSigkill(@Value("${arclib.externalProcess.timeout.sigkill}") int timeoutSigkill) {
        this.timeoutSigkill = timeoutSigkill;
    }
}
