package cz.cas.lib.arclib.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class IngestIssueDefinitionUpdateDto {
    private String id;
    private String name;
    private String number;
    private String description;
    private String solution;
}
