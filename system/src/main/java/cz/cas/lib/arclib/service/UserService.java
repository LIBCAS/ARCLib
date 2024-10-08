package cz.cas.lib.arclib.service;

import com.google.common.collect.Sets;
import com.querydsl.core.util.StringUtils;
import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.domainbase.audit.AuditLogger;
import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.dto.AccountUpdateDto;
import cz.cas.lib.arclib.dto.UserCreateOrUpdateDto;
import cz.cas.lib.arclib.dto.UserFullnameDto;
import cz.cas.lib.arclib.exception.BadRequestException;
import cz.cas.lib.arclib.exception.ForbiddenException;
import cz.cas.lib.arclib.security.authorization.business.audit.RoleAddEvent;
import cz.cas.lib.arclib.security.authorization.business.audit.RoleDelEvent;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.security.authorization.role.UserRole;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.store.ProducerStore;
import cz.cas.lib.arclib.store.UserStore;
import cz.cas.lib.core.rest.data.DelegateAdapter;
import cz.cas.lib.core.store.Transactional;
import cz.cas.lib.core.util.Utils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cz.cas.lib.arclib.utils.ArclibUtils.hasRole;
import static cz.cas.lib.core.util.Utils.eq;
import static cz.cas.lib.core.util.Utils.notNull;

@Slf4j
@Service
public class UserService implements DelegateAdapter<User> {

    @Getter
    private UserStore delegate;
    private UserDetails userDetails;
    private AuditLogger logger;
    private ProducerStore producerStore;
    private PasswordEncoder passwordEncoder;

    @Value("${security.local.enabled}")
    private boolean localAuth;

    @Transactional
    public User updateAccount(AccountUpdateDto updateDto) {
        User user = find(userDetails.getId());
        user.setEmail(updateDto.getEmail());
        if (!StringUtils.isNullOrEmpty(updateDto.getNewPassword())) {
            user.setPassword(passwordEncoder.encode(updateDto.getNewPassword()));
        }
        return save(user);
    }

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
        // quick fix for forcing change of .updated.
        // if only roles are changed then .updated would not be generated by Hibernate.
        userEntity.setUpdated(Instant.now());

        if (!hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE)) {
            userEntity.setProducer(new Producer(userDetails.getProducerId()));
            if (userEntity.jointPermissions().contains(Permissions.SUPER_ADMIN_PRIVILEGE)) {
                throw new ForbiddenException("You are not allowed to assign permission: " + Permissions.SUPER_ADMIN_PRIVILEGE);
            }
        } else {
            notNull(userDto.getProducer(), () -> new BadRequestException("user has to have producer assigned"));
            userEntity.setProducer(userDto.getProducer());
        }

        Producer producerOfNewUser = producerStore.find(userEntity.getProducer().getId());
        userDto.getExportFolders().forEach(exportFolderOfUser -> {
            boolean userCanExportToSelectedFolder = Utils.pathIsNestedInParent(exportFolderOfUser, producerOfNewUser.getExportFolders());
            Utils.eq(userCanExportToSelectedFolder, true, () -> new ForbiddenException("Users of this producer are not allowed to export to folder: " + exportFolderOfUser
                    + " allowed export folders for this producer are: " + String.join(",", producerOfNewUser.getExportFolders())));
        });
        userEntity.setExportFolders(userDto.getExportFolders());

        if(localAuth){
            userEntity.setFirstName(userDto.getFirstName());
            userEntity.setLastName(userDto.getLastName());
            userEntity.setInstitution(userDto.getInstitution());
            userEntity.setEmail(userDto.getEmail());
            userEntity.setUsername(userDto.getUsername());
            if (!StringUtils.isNullOrEmpty(userDto.getNewPassword())) {
                userEntity.setPassword(passwordEncoder.encode(userDto.getNewPassword()));
            }
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
        List<User> users;
        if (!hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE)) {
            users = delegate.findAllUsersOrderByName(true, userDetails.getProducerId());
        } else {
            users = delegate.findAllUsersOrderByName(false, userDetails.getProducerId());
        }
        return users.stream().map(u -> new UserFullnameDto(u.getId(), u.getFullName())).collect(Collectors.toList());
    }

    private void logRoleSaving(String userId, Set<UserRole> oldRoles, Set<UserRole> newRoles) {
        Sets.SetView<UserRole> removedRoles = Sets.difference(oldRoles, newRoles);
        Sets.SetView<UserRole> addedRoles = Sets.difference(newRoles, oldRoles);

        removedRoles.forEach(role -> logger.logEvent(new RoleDelEvent(Instant.now(), userId, role.getId(), role.getName())));
        addedRoles.forEach(role -> logger.logEvent(new RoleAddEvent(Instant.now(), userId, role.getId(), role.getName())));
    }

    @Autowired
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }

    @Autowired
    public void setDelegate(UserStore delegate) {
        this.delegate = delegate;
    }

    @Autowired
    public void setLogger(AuditLogger logger) {
        this.logger = logger;
    }

    @Autowired
    public void setProducerStore(ProducerStore producerStore) {
        this.producerStore = producerStore;
    }

    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }
}
