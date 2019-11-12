package cz.cas.lib.arclib.exception.bpm;

public class CommandLineProcessException extends IncidentException {
    public CommandLineProcessException(String e) {
        super(e);
    }

    public CommandLineProcessException(String e, Throwable cause) {
        super(e, cause);
    }
}
