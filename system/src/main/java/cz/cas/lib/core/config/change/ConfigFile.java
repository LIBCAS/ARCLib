package cz.cas.lib.core.config.change;

import cz.cas.lib.arclib.domainbase.domain.DomainObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.time.Instant;

/**
 * All Spring configuration parameters gathered into single JSON encoded {@link ConfigFile#value}.
 */
@Getter
@Setter
@Entity
@Table(name = "uas_config_file")
public class ConfigFile extends DomainObject {

    /**
     * Date of creation
     */
    protected Instant created;

    /**
     * JSON encoded configuration parameters
     */
    @Lob
    protected String value;
}
