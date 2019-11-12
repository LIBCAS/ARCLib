package cz.cas.lib.core.security.audit;

import cz.cas.lib.arclib.domainbase.audit.AuditEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@NoArgsConstructor
@Getter
@Setter
public class LoginEvent extends AuditEvent implements Serializable {
    private String username;
    private boolean success;

    public LoginEvent(Instant created, String username, boolean success) {
        super(created, "LOGIN");
        this.username = username;
        this.success = success;
    }
}
