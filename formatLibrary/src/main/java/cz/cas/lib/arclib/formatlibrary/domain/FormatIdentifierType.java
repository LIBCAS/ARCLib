package cz.cas.lib.arclib.formatlibrary.domain;

import lombok.Getter;

/**
 * Typ identifikátora formátu
 */
@Getter
public enum FormatIdentifierType {
    PUID,
    GDFRCLASS("GDFRClass"),
    GDFRFORMAT("GDFRFormat"),
    GDFRREGISTRY("GDFRRegistry"),
    TOM,
    MIME,
    FOUR_CC("4CC"),
    ARK,
    DOI,
    PURL,
    URI,
    URL,
    URN,
    UUID_GUID("UUID/GUID"),
    HANDLE("Handle"),
    ISBN,
    ISSN,
    APPLE_UNIFORM_TYPE_IDENTIFIER,
    LIBRARY_OF_CONGRESS_FORMAT_DESCRIPTION_IDENTIFIER,
    UDC,
    DDC,
    LCC,
    LCCN,
    RFC,
    ANSI,
    ISO,
    BSI,
    OTHER("Other");

    private String label;

    FormatIdentifierType(String label) {
        this.label = label;
    }

    FormatIdentifierType() {
        this.label = name();
    }
}
