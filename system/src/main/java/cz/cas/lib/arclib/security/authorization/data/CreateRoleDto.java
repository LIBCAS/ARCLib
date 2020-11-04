package cz.cas.lib.arclib.security.authorization.data;

import cz.cas.lib.arclib.security.authorization.logic.ValidPermission;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateRoleDto {
    private String name;
    private String description;
    private Set<@ValidPermission String> permissions = new HashSet<>();
}
