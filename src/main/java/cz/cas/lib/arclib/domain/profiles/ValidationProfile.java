package cz.cas.lib.arclib.domain.profiles;

import cz.cas.lib.core.domain.NamedObject;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
@NoArgsConstructor
public class ValidationProfile extends NamedObject {

    /**
     * XML obsah profilu
     */
    @Column(length = 10485760)
    private String xml;
}
