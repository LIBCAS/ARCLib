package cz.cas.lib.arclib.antivirus;

public class SIPAntivirusScannerException extends Exception {
    /**
     * Threw when error occurs during the antivirus scan process
     *
     * @param errOutput
     */
    public SIPAntivirusScannerException(String errOutput) {
        super(errOutput);
    }
}
