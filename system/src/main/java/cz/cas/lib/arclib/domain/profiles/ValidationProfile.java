package cz.cas.lib.arclib.domain.profiles;

import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.domainbase.domain.NamedObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Validačný profil
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_validation_profile")
@NoArgsConstructor
public class ValidationProfile extends NamedObject {

    /**
     * Dodávateľ
     */
    @ManyToOne
    private Producer producer;

    /**
     * Externé id
     */
    private String externalId;

    /**
     * XML obsah profilu
     */
    @Column(length = 10485760)
    private String xml;

    /**
     * Validačný profil je možné editovať
     */
    private boolean editable;


    public ValidationProfile(String xml) {
        this.xml = xml;
    }

}
