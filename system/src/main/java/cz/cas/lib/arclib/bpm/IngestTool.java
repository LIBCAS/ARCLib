package cz.cas.lib.arclib.bpm;

public interface IngestTool {
    String getToolVersion();

    String getToolName();

    default String getShortToolVersion() {
        return getToolVersion();
    }
}
