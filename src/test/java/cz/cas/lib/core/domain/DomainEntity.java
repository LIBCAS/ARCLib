package cz.cas.lib.core.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Table;

@Getter
@Setter
@Entity
@Table(name = "test_entity")
public class DomainEntity extends DomainObject {
    private String test;
}
