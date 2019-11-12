package cz.cas.lib.arclib.domain;

import lombok.Getter;

/**
 * Stav spracovania dávky
 */
@Getter
public enum BatchState {
    PROCESSING,
    SUSPENDED,
    CANCELED,
    PROCESSED,
    PROCESSED_WITH_FAILURES
}
