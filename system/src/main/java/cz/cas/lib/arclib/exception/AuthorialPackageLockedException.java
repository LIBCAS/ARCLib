package cz.cas.lib.arclib.exception;

import java.time.Instant;

public class AuthorialPackageLockedException extends RuntimeException {
    private String lockedByUser;
    private Instant latestLockedInstant;

    public AuthorialPackageLockedException(String lockedByUser, Instant latestLockedInstant) {
        super(getMessage(lockedByUser, latestLockedInstant));

        this.lockedByUser = lockedByUser;
        this.latestLockedInstant = latestLockedInstant;
    }

    public static final String getMessage(String userHoldingLock, Instant latestLockedInstant) {
        return "{\"lockedByUser\": \"" + userHoldingLock + "\"" +
                ", \"latestLockedInstant\": \"" + latestLockedInstant +
                "\"}";
    }

    @Override
    public String toString() {
        return getMessage(lockedByUser, latestLockedInstant);
    }
}
