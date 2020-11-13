package cz.cas.lib.arclib.dto;

import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.security.authorization.data.UserRole;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

@Getter
public class UserCreateOrUpdateDto {
    private String id;
    private String username;
    private Producer producer;
    private Set<UserRole> roles = new HashSet<>();
}