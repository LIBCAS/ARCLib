package cz.cas.lib.arclib.dto;

import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.security.authorization.role.UserRole;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

@Getter
public class UserCreateOrUpdateDto {
    private String id;
    private String username;
    private Producer producer;
    private Set<String> exportFolders = new HashSet<>();
    private Set<UserRole> roles = new HashSet<>();

    private String firstName;
    private String lastName;
    private String institution;
    private String email;
    private String newPassword;
}
