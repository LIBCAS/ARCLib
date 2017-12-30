package cz.inqool.uas.index;

import cz.inqool.uas.domain.DatedObject;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

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

    @BatchSize(size = 100)
    @Fetch(FetchMode.SELECT)
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name="test_indexed_id")
    private Set<DependentEntity> dependents;
}
