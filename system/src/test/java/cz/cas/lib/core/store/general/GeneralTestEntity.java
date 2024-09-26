package cz.cas.lib.core.store.general;

import cz.cas.lib.arclib.domainbase.domain.DomainObject;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Getter
@Setter
@Entity
@Table(name = "test_general")
public class GeneralTestEntity extends DomainObject {
    private String test;
}
