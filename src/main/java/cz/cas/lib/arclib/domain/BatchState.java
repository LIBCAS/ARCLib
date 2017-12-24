package cz.cas.lib.arclib.domain;

import lombok.Getter;

@Getter
public enum BatchState {
    PROCESSING,
    SUSPENDED,
    CANCELED,
    PROCESSED
}
