package cz.cas.lib.arclib.dto;

import cz.cas.lib.arclib.domain.Producer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SipProfileDto {
    private String id;
    private String name;
    private Producer producer;
    private Instant created;
    private Instant updated;
    private Boolean editable;
    private String externalId;
}
