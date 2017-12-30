package cz.cas.lib.arclib.antivirus;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface SIPAntivirusScanner {
    /**
     * Scans SIP package for viruses.
     *
     * @param pathToSIP absoulte path to SIP
     * @return list with paths to corrupted files if threat was detected, empty list otherwise
     * @throws FileNotFoundException
     * @throws IOException
     * @throws InterruptedException
     * @throws SIPAntivirusScannerException if error occurs during the antivirus scan process
     */
    List<Path> scan(String pathToSIP) throws IOException, InterruptedException, SIPAntivirusScannerException;

    /**
     * Moves infected files to quarantine.
     *
     * @param infectedFiles
     */
    void moveToQuarantine(List<Path> infectedFiles) throws IOException;
}
