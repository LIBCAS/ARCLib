package cz.cas.lib.arclib.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ToolUpdateDto {
    private String id;
    private String description;
    private String licenseInformation;
    private String documentation;
}
