package cz.cas.lib.arclib.domain.profiles;

import cz.cas.lib.arclib.domainbase.domain.NamedObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

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
     * Externé id
     */
    private String externalId;

    /**
     * XML obsah profilu
     */
    @Column(length = 10485760)
    private String xml;

    public ValidationProfile(String xml) {
        this.xml = xml;
    }
}
