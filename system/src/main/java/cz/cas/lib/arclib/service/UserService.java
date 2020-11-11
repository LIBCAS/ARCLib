package cz.cas.lib.arclib.service;

import com.google.common.collect.Sets;
import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.domainbase.audit.AuditLogger;
import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.dto.UserCreateOrUpdateDto;
import cz.cas.lib.arclib.dto.UserFullnameDto;
import cz.cas.lib.arclib.exception.BadRequestException;
import cz.cas.lib.arclib.exception.ForbiddenException;
import cz.cas.lib.arclib.security.authorization.data.Permissions;
import cz.cas.lib.arclib.security.authorization.data.UserRole;
import cz.cas.lib.arclib.security.authorization.deprecated.assign.audit.RoleAddEvent;
import cz.cas.lib.arclib.security.authorization.deprecated.assign.audit.RoleDelEvent;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.store.UserStore;
import cz.cas.lib.core.index.dto.*;
import cz.cas.lib.core.rest.data.DelegateAdapter;
import cz.cas.lib.core.store.Transactional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cz.cas.lib.arclib.utils.ArclibUtils.hasRole;
import static cz.cas.lib.core.util.Utils.*;

@Slf4j
@Service
public class UserService implements DelegateAdapter<User> {

    @Getter
    private UserStore delegate;
    private UserDetails userDetails;
    private AuditLogger logger;


    @Transactional
    public User createOrUpdate(String id, UserCreateOrUpdateDto userDto) {
        notNull(userDto, () -> new BadArgument("user is null"));
        eq(id, userDto.getId(), () -> new BadArgument("id"));
        notNull(userDto.getUsername(), () -> new BadArgument("missing username"));

        User userEntity = delegate.find(id);
        if (userEntity == null) {
            userEntity = new User();
            userEntity.setUsername(userDto.getUsername());
            userEntity.setId(userDto.getId());
        }
        userEntity.setRoles(userDto.getRoles());

        if (!hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE)) {
            userEntity.setProducer(new Producer(userDetails.getProducerId()));
            if (userEntity.jointPermissions().contains(Permissions.SUPER_ADMIN_PRIVILEGE)) {
                throw new ForbiddenException("You are not allowed to assign permission: " + Permissions.SUPER_ADMIN_PRIVILEGE);
            }
        } else {
            notNull(userDto.getProducer(), () -> new BadRequestException("user has to have producer assigned"));
            userEntity.setProducer(userDto.getProducer());
        }

        logRoleSaving(id, userEntity.getRoles(), userDto.getRoles());
        return delegate.save(userEntity);
    }

    @Override
    @Transactional
    public void delete(User user) throws ForbiddenException {
        if (!hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE)) {
            if (user.jointPermissions().contains(Permissions.SUPER_ADMIN_PRIVILEGE))
                throw new ForbiddenException("you are not allowed to delete a user that is a Super Admin (permission: " + Permissions.SUPER_ADMIN_PRIVILEGE + ")");
        }

        logRoleSaving(user.getId(), user.getRoles(), Collections.emptySet());
        user.setRoles(Collections.emptySet());
        save(user);

        delegate.delete(user);
    }

    public void revokeRole(String roleId) {
        List<User> usersWithRole = delegate.findByRole(roleId);
        usersWithRole.forEach(user -> {
            user.getRoles().removeIf(userRole -> userRole.getId().equals(roleId));
        });
        delegate.save(usersWithRole);
    }

    public User findUserByUsername(String username) {
        return delegate.findUserByUsername(username);
    }

    public List<User> findUsersByPermission(String permission) {
        return delegate.findByPermission(permission);
    }

    public List<UserFullnameDto> listUserNames() {
        Params params = new Params();
        params.setSorting(List.of(new SortSpecification("lastName", Order.ASC), new SortSpecification("firstName", Order.ASC)));
        params.setPageSize(null);
        if (!hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE))
            params.setFilter(List.of(new Filter("producerId", FilterOperation.EQ, userDetails.getProducerId(), asList())));
        return findAll(params).getItems().stream().map(u -> new UserFullnameDto(u.getId(), u.getFullName())).collect(Collectors.toList());
    }

    private void logRoleSaving(String userId, Set<UserRole> oldRoles, Set<UserRole> newRoles) {
        Sets.SetView<UserRole> removedRoles = Sets.difference(oldRoles, newRoles);
        Sets.SetView<UserRole> addedRoles = Sets.difference(newRoles, oldRoles);

        removedRoles.forEach(role -> logger.logEvent(new RoleDelEvent(Instant.now(), userId, role.getId(), role.getName())));
        addedRoles.forEach(role -> logger.logEvent(new RoleAddEvent(Instant.now(), userId, role.getId(), role.getName())));
    }

    @Inject
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }

    @Inject
    public void setDelegate(UserStore delegate) {
        this.delegate = delegate;
    }

    @Inject
    public void setLogger(AuditLogger logger) {
        this.logger = logger;
    }
}
