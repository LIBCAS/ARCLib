package cz.cas.lib.arclib.domain;

import lombok.Getter;

/**
 * Úroveň verzovania
 */
@Getter
public enum VersioningLevel {
    NO_VERSIONING,
    ARCLIB_XML_VERSIONING,
    SIP_PACKAGE_VERSIONING
}
