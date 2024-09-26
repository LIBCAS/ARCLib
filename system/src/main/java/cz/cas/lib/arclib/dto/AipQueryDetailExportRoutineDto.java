package cz.cas.lib.arclib.dto;

import cz.cas.lib.arclib.domain.export.ExportConfig;
import cz.cas.lib.arclib.domainbase.domain.DatedObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Embedded;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AipQueryDetailExportRoutineDto extends DatedObject {
    /**
     * Čas plánovaného spustenia exportu
     */
    @NotNull
    private Instant exportTime;

    @Embedded
    @NotNull
    private ExportConfig config;
}
