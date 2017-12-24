package cz.cas.lib.arclib.domain;

import lombok.Getter;

@Getter
public enum SipState {
    NEW,
    PROCESSING,
    PROCESSED,
    FAILED
}
