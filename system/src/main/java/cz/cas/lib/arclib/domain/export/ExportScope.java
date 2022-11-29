package cz.cas.lib.arclib.domain.export;

import lombok.Getter;

public enum ExportScope {
    IDS("ids.csv", "text/csv"),
    METADATA("metadata.csv", "text/csv"),
    AIP_XML("xml", null),
    DATA_AND_LAST_XML("data_with_xml", null),
    DATA_AND_ALL_XMLS("data_with_xmls", null);

    @Getter
    private String fsName;

    @Getter
    private String contentType;

    ExportScope(String fsName, String contentType) {
        this.fsName = fsName;
        this.contentType = contentType;
    }
}
