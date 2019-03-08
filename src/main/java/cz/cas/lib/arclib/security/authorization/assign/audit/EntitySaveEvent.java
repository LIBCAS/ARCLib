package cz.cas.lib.arclib.security.authorization.assign.audit;

import cz.cas.lib.core.audit.AuditEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@NoArgsConstructor
@Getter
@Setter
public class EntitySaveEvent extends AuditEvent implements Serializable {
    private String userId;
    private String entityName;
    private String entityId;


    public EntitySaveEvent(Instant created, String userId, String entityName, String entityId) {
        super(created, "ENTITY_SAVE");
        this.userId = userId;
        this.entityName = entityName;
        this.entityId = entityId;
    }
}
