package cz.cas.lib.arclib.domainbase.domain;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.MappedSuperclass;

@Getter
@Setter
@MappedSuperclass
public class NamedObject extends DatedObject {
    protected String name;
}
