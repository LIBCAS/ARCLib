package cz.cas.lib.core.index.nested;

import cz.cas.lib.arclib.domainbase.domain.DomainObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Getter
@Setter
@Entity
@Table(name = "test_nested_child")
@NoArgsConstructor
@AllArgsConstructor
public class ChildEntity extends DomainObject {

    @ManyToOne
    private ParentEntity parent;
    private String attr;
}
