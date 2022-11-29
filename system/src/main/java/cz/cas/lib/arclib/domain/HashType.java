package cz.cas.lib.arclib.domain;

import lombok.Getter;

/**
 * Typ fixity
 */
@Getter
public enum HashType {
    MD5,
    Crc32,
    Sha512,
    Sha256,
    Sha1,
}
