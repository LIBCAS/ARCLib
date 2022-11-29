package cz.cas.lib.arclib.domain;

import cz.cas.lib.arclib.domainbase.domain.DatedObject;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.security.authorization.role.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Užívateľ
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_user")
public class User extends DatedObject {

    /**
     * Používateľské meno
     */
    private String username;

    /**
     * Krstné meno
     */
    private String firstName;

    /**
     * Priezvisko
     */
    private String lastName;

    /**
     * Email
     */
    private String email;

    /**
     * Unikátny identifikátor v rámci LDAPu
     */
    private String ldapDn;

    /**
     * Dodávateľ
     */
    @ManyToOne
    private Producer producer;

    /**
     * Instituce
     */
    private String institution;

    /**
     * Authorization Roles of User
     * User can have multiple roles. Each {@link UserRole} has its own {@link Permissions}.
     * Permissions (their String representation) are validated by @PreAuthorize annotation.
     */
    @BatchSize(size = 100)
    @Fetch(FetchMode.SELECT)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "arclib_assigned_user_role",
            joinColumns = @JoinColumn(name = "arclib_user_id"),
            inverseJoinColumns = @JoinColumn(name = "arclib_role_id"))
    private Set<UserRole> roles = new HashSet<>();

    /**
     * Exportní složky
     */
    @BatchSize(size = 100)
    @Fetch(FetchMode.SELECT)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "arclib_user_export_folders", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "folder")
    private Set<String> exportFolders = new HashSet<>();


    public User(String id) {
        setId(id);
    }

    public User(String id, Producer p, Set<UserRole> roles) {
        setId(id);
        setProducer(p);
        setRoles(roles);
    }

    /**
     * All permissions of user deducted from user's roles
     *
     * @return user's permission set
     */
    public Set<String> jointPermissions() {
        Set<UserRole> userRoles = getRoles();
        Set<String> userPermissions = new HashSet<>();
        userRoles.forEach(role -> userPermissions.addAll(role.getPermissions()));
        return userPermissions;
    }

    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        } else {
            return username;
        }
    }
}
