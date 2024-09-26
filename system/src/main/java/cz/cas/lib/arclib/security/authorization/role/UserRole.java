package cz.cas.lib.arclib.security.authorization.role;

import cz.cas.lib.arclib.domainbase.domain.DomainObject;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_user_role")
public class UserRole extends DomainObject {

    private String name;

    /**
     * Czech description of a role.
     */
    private String description;

    /**
     * Permissions associated with a user's role.
     *
     * @see Permissions
     */
    @BatchSize(size = 100)
    @Fetch(FetchMode.SELECT)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "arclib_role_permission", joinColumns = @JoinColumn(name = "arclib_role_id"))
    @Column(name = "perm")
    private Set<String> permissions = new HashSet<>();

}
