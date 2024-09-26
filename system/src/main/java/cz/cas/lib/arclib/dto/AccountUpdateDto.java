package cz.cas.lib.arclib.dto;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
public class AccountUpdateDto {
    @Email
    @NonNull
    private String email;
    private String newPassword;
}
