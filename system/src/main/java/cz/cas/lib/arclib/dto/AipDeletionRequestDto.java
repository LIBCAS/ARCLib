package cz.cas.lib.arclib.dto;

import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.domainbase.domain.DatedObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class AipDeletionRequestDto extends DatedObject {
    private String aipId;
    private User requester;
    private User confirmer1;
    private User confirmer2;
    private User rejectedBy;
}
