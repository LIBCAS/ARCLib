package cz.cas.lib.arclib.security.authorization.logic;

import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.security.authorization.data.CreateRoleDto;
import cz.cas.lib.arclib.security.authorization.data.Permissions;
import cz.cas.lib.arclib.security.authorization.data.UpdateRoleDto;
import cz.cas.lib.arclib.security.authorization.data.UserRole;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.service.UserService;
import cz.cas.lib.core.index.dto.Result;
import cz.cas.lib.core.store.Transactional;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cz.cas.lib.arclib.utils.ArclibUtils.hasRole;
import static cz.cas.lib.core.util.Utils.notNull;

@Service
public class UserRoleService {

    private UserRoleStore store;
    private UserService userService;
    private UserDetails userDetails;


    @Transactional
    public UserRole find(String id) {
        UserRole entity = store.find(id);
        notNull(entity, () -> new MissingObject(UserRole.class, id));
        return entity;
    }

    @Transactional
    public UserRole create(CreateRoleDto dto) {
        UserRole entity = new UserRole();
        entity.setName(dto.getName());
        entity.setPermissions(dto.getPermissions());
        entity.setDescription(dto.getDescription());

        return store.save(entity);
    }

    @Transactional
    public UserRole update(UpdateRoleDto dto) {
        UserRole entity = this.find(dto.getId());
        entity.setName(dto.getName());
        entity.setPermissions(dto.getPermissions());
        entity.setDescription(dto.getDescription());

        return store.save(entity);
    }

    @Transactional
    public void delete(String id) {
        UserRole entity = this.find(id);

        userService.revokeRole(id);
        store.delete(entity);
    }

    @Transactional
    public Collection<UserRole> findAll() {
        return store.findAll();
    }

    public Collection<User> findForPermission(String perm) {
        return userService.findUsersByPermission(perm);
    }

    public Result<String> findAllAssignablePermissions() {
        Result<String> perms = new Result<>();
        Set<String> assignablePerms = new HashSet<>(Permissions.ALL_PERMISSIONS);
        if (!hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE)) {
            assignablePerms.removeIf(perm -> perm.equals(Permissions.SUPER_ADMIN_PRIVILEGE));
        }
        perms.setItems(assignablePerms.stream().sorted().collect(Collectors.toList()));
        perms.setCount((long) perms.getItems().size());
        return perms;
    }

    public Collection<User> getUsersWithPermission(String permission) {
        return userService.findUsersByPermission(permission);
    }

    public Collection<String> getEmailsOfUsersWithPermission(String permission) {
        return userService.findUsersByPermission(permission).stream()
                .map(User::getEmail)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    @Inject
    public void setStore(UserRoleStore store) {
        this.store = store;
    }

    @Inject
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Inject
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }
}
