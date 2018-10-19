package cz.cas.lib.core.store.general;

import cz.cas.lib.core.domain.DomainObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Table;

@Getter
@Setter
@Entity
@Table(name = "test_general")
public class GeneralTestEntity extends DomainObject {
    private String test;
}
