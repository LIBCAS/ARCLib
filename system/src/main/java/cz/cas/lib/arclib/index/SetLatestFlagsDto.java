package cz.cas.lib.arclib.index;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SetLatestFlagsDto {
    private String arclibXmlDocumentId;
    private boolean latest;
    private boolean latestData;
    private byte[] aipXml;

    public SetLatestFlagsDto(String arclibXmlDocumentId, boolean latest, boolean latestData, byte[] aipXml) {
        this.arclibXmlDocumentId = arclibXmlDocumentId;
        this.latest = latest;
        this.latestData = latestData;
        this.aipXml = aipXml;
    }
}
