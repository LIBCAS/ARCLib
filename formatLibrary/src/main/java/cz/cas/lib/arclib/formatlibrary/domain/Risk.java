package cz.cas.lib.arclib.formatlibrary.domain;

import cz.cas.lib.arclib.domainbase.domain.NamedObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Riziko viažuce sa ku formátu
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_format_risk")
@NoArgsConstructor
public class Risk extends NamedObject {
    /**
     * Popis
     */
    @Column(length = 6000)
    private String description;
}
