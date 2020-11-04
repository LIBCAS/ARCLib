package cz.cas.lib.arclib.dto;

import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.security.authorization.deprecated.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserWithRoleDto {
    private User user;
    private List<Role> roles;
}
