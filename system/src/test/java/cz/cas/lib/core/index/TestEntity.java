package cz.cas.lib.core.index;

import cz.cas.lib.arclib.domainbase.domain.DatedObject;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "test_indexed")
public class TestEntity extends DatedObject {
    private String textualAttribute;

    private Integer intAttribute;

    private Double doubleAttribute;

    private LocalDate localDateAttribute;

    private Instant instantAttribute;

    @Override
    public String toString() {
        return "TestEntity{" +
                "textualAttribute='" + textualAttribute + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
