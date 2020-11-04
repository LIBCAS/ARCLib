package cz.cas.lib.arclib.security.authorization.deprecated.assign.audit;

import cz.cas.lib.arclib.domainbase.audit.AuditEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@NoArgsConstructor
@Getter
@Setter
public class RoleAddEvent extends AuditEvent implements Serializable {
    private String userId;
    private String roleId;
    private String roleName;


    public RoleAddEvent(Instant created, String userId, String roleId, String roleName) {
        super(created, "ROLE_ADD");
        this.userId = userId;
        this.roleId = roleId;
        this.roleName = roleName;
    }
}
