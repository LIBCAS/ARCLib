package cz.cas.lib.arclib.exception;

import cz.cas.lib.core.exception.GeneralException;

public class AuthorialPackageLockedException extends GeneralException {
    public AuthorialPackageLockedException(String message) {
        super(message);
    }
}