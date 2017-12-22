package cz.inqool.uas.index;

import cz.inqool.uas.domain.DomainObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Table;

@Getter
@Setter
@Entity
@Table(name = "test_dependent")
public class DependentEntity extends DomainObject {
    private String name;
}
