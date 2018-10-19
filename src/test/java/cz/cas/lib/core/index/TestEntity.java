package cz.cas.lib.core.index;

import cz.cas.lib.core.domain.DatedObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "test_indexed")
public class TestEntity extends DatedObject {
    private String stringAttribute;

    private Integer intAttribute;

    private Double doubleAttribute;

    private LocalDate localDateAttribute;

    private Instant instantAttribute;

    @ManyToOne(cascade = CascadeType.ALL)
    private DependentEntity dependent;

//    @BatchSize(size = 100)
//    @Fetch(FetchMode.SELECT)
//    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
//    @JoinColumn(name="test_indexed_id")
//    private Set<DependentEntity> dependents;
}
