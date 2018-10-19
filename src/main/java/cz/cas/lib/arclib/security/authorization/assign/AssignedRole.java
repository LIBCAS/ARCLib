package cz.cas.lib.arclib.security.authorization.assign;

import cz.cas.lib.arclib.security.authorization.role.Role;
import cz.cas.lib.core.domain.DomainObject;
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
public class AssignedRole extends DomainObject {
    protected String userId;

    @ManyToOne
    protected Role role;
}
