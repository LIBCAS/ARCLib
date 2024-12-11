package cz.cas.lib.arclib.domainbase.domain;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public class NamedObject extends DatedObject {
    protected String name;
}
