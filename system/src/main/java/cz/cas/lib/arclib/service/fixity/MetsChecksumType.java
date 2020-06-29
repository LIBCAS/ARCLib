package cz.cas.lib.arclib.service.fixity;

import lombok.Getter;

@Getter
public enum MetsChecksumType {
    ADLER("Adler-32"),
    CRC32("CRC32"),
    HAVAL("HAVAL"),
    MD5("MD5"),
    MNP("MNP"),
    SHA1("SHA-1"),
    SHA256("SHA-256"),
    SHA384("SHA-384"),
    SHA512("SHA-512"),
    TIGER("TIGER"),
    WHIRLPOOL("WHIRLPOOL");
    private String xmlValue;

    MetsChecksumType(String xmlValue) {
        this.xmlValue = xmlValue;
    }
}
