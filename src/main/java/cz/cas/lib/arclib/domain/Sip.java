package cz.cas.lib.arclib.domain;

import cz.inqool.uas.domain.DomainObject;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_sip")
public class Sip extends DomainObject {
    /**
    * Cesta k súboru
    */
    protected String path;

    /**
     * Stav spracovania
     */
    @Enumerated(EnumType.STRING)
    protected SipState state;
}
