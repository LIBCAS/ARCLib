package cz.cas.lib.arclib.dto;

import cz.cas.lib.arclib.domain.export.ExportConfig;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class BucketExportRequestDto {

    @NotNull
    private ExportConfig exportConfig;

    private Set<String> ids = new HashSet<>();
}
