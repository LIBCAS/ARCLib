package cz.cas.lib.arclib.security.authorization.role;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateRoleDto extends CreateRoleDto {
    private String id;
}
