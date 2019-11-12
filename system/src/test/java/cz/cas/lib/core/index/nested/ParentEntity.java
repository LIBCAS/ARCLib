package cz.cas.lib.core.index.nested;

import cz.cas.lib.arclib.domainbase.domain.DomainObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "test_nested_parent")
@NoArgsConstructor
public class ParentEntity extends DomainObject {
    @OneToMany(mappedBy = "parent")
    Set<ChildEntity> children = new HashSet<>();
    private String attr;

    public ParentEntity(String attr) {
        super();
        this.attr = attr;
    }
}
