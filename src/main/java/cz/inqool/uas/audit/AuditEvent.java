package cz.inqool.uas.audit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

/**
 * Base audit log event.
 *
 * Developer can create own implementation of audit event by sub-classing this class.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AuditEvent implements Serializable {
    /**
     * Time of event
     */
    protected Instant created;

    /**
     * Type of event.
     *
     * <p>
     *     Used for better filtering.
     * </p>
     */
    protected String type;
}
