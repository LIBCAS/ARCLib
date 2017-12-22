package cz.inqool.uas.security.authorization.assign;

import cz.inqool.uas.domain.DomainObject;
import cz.inqool.uas.security.authorization.role.Role;
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
@Table(name = "uas_assigned_role")
public class AssignedRole extends DomainObject {
    protected String userId;

    @ManyToOne
    protected Role role;
}
