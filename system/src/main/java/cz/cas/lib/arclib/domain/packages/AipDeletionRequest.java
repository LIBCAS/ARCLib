package cz.cas.lib.arclib.domain.packages;

import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.domainbase.domain.DatedObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Žiadosť o zmazanie AIP balíka
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_aip_deletion_request")
@AllArgsConstructor
@NoArgsConstructor
public class AipDeletionRequest extends DatedObject {
    /**
     * Id balíka pre zmazanie
     */
    private String aipId;

    /**
     * Žiadateľ
     */
    @ManyToOne
    private User requester;

    /**
     * Prvá potvrdzujúca osoba
     */
    @ManyToOne
    private User confirmer1;

    /**
     * Druhá potvrdzujúca osoba
     */
    @ManyToOne
    private User confirmer2;

    /**
     * Osoba jenž zamítla
     */
    @ManyToOne
    private User rejectedBy;
}
