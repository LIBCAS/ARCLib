package cz.cas.lib.arclib.security.authorization.data;


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
