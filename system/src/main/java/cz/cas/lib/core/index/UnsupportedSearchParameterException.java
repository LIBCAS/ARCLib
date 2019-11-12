package cz.cas.lib.core.index;

public class UnsupportedSearchParameterException extends RuntimeException {

    public UnsupportedSearchParameterException() {
        super();
    }

    public UnsupportedSearchParameterException(String message) {
        super(message);
    }

    public UnsupportedSearchParameterException(Throwable cause) {
        super(cause);
    }

    protected UnsupportedSearchParameterException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
