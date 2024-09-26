package cz.cas.lib.arclib.domain.packages;

import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.domainbase.domain.DatedObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Autorský balíček
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "arclib_authorial_package")
public class AuthorialPackage extends DatedObject {
    /**
     * Autorské ID
     */
    private String authorialId;

    /**
     * Profil dodávateľa
     */
    @ManyToOne
    private ProducerProfile producerProfile;
}
