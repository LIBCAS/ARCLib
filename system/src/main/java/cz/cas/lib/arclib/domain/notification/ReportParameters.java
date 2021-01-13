package cz.cas.lib.arclib.domain.notification;

import cz.cas.lib.arclib.report.ExportFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportParameters {
    private ExportFormat format;
    private Map<String, String> params;
}
