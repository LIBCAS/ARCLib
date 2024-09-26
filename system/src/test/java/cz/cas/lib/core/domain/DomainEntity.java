package cz.cas.lib.core.domain;

import cz.cas.lib.arclib.domainbase.domain.DomainObject;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Getter
@Setter
@Entity
@Table(name = "test_entity")
public class DomainEntity extends DomainObject {
    private String test;
}
