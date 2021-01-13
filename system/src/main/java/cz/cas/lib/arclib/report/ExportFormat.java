package cz.cas.lib.arclib.report;

import lombok.Getter;

public enum ExportFormat {
    PDF(".pdf"),
    XLSX(".xlsx"),
    CSV(".csv"),
    HTML(".html");

    @Getter
    private final String extension;

    ExportFormat(String extension) {
        this.extension = extension;
    }
}
