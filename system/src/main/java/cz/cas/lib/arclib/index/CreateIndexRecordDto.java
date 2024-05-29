package cz.cas.lib.arclib.index;

import cz.cas.lib.arclib.index.solr.arclibxml.IndexedAipState;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateIndexRecordDto {
    private byte[] arclibXml;
    private String producerId;
    private String producerName;
    private String userName;
    private IndexedAipState aipState;
    private boolean debuggingModeActive;
    private boolean latestVersion;
    private boolean latestDataVersion;

    public CreateIndexRecordDto(byte[] arclibXml, String producerId, String producerName, String userName, IndexedAipState aipState, boolean debuggingModeActive, boolean latestVersion, boolean latestDataVersion) {
        this.arclibXml = arclibXml;
        this.producerId = producerId;
        this.producerName = producerName;
        this.userName = userName;
        this.aipState = aipState;
        this.debuggingModeActive = debuggingModeActive;
        this.latestVersion = latestVersion;
        this.latestDataVersion = latestDataVersion;
    }
}
