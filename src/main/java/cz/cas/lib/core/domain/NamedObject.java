package cz.cas.lib.core.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.MappedSuperclass;

@Getter
@Setter
@MappedSuperclass
public class NamedObject extends DatedObject {
    protected String name;
}
