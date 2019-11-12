package cz.cas.lib.arclib.domain;

import cz.cas.lib.arclib.domainbase.domain.NamedObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Dodávateľ
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_producer")
@NoArgsConstructor
public class Producer extends NamedObject {

    public Producer(String id) {
        setId(id);
    }

    /**
     * Cesta do prekladiska
     */
    private String transferAreaPath;
}
