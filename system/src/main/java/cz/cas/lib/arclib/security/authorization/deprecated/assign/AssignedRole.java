package cz.cas.lib.arclib.security.authorization.deprecated.assign;

import cz.cas.lib.arclib.security.authorization.deprecated.Role;
import cz.cas.lib.arclib.domainbase.domain.DomainObject;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_assigned_role")
@Deprecated // user new UserRole
public class AssignedRole extends DomainObject {
    protected String userId;

    @ManyToOne
    protected Role role;
}
