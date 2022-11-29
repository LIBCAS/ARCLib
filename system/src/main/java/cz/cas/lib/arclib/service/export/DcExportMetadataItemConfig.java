package cz.cas.lib.arclib.service.export;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.regex.Pattern;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DcExportMetadataItemConfig {
    private @NotNull DcExportPackageType packageType;
    private @NotNull DcExportMetadataKey metadataKey;
    private @NotNull List<String> dmdSecIdLike;
    private @NotNull String indexField;
    private String valueRegex;
    private Pattern compiledValueRegex;
    private String invalidValueRegex;
    private Pattern compiledInvalidValueRegex;

    public void compileRegexes() {
        if (valueRegex != null) {
            compiledValueRegex = Pattern.compile(valueRegex);
        }
        if (invalidValueRegex != null) {
            compiledInvalidValueRegex = Pattern.compile(invalidValueRegex);
        }
    }
}
