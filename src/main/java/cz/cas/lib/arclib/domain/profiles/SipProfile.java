package cz.cas.lib.arclib.domain.profiles;

import cz.cas.lib.arclib.domain.packages.PackageType;
import cz.cas.lib.core.domain.NamedObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 * Sip profil
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_sip_profile")
@NoArgsConstructor
@AllArgsConstructor
public class SipProfile extends NamedObject {

    /**
     * XSL obsah profilu
     */
    @Column(length = 10485760)
    private String xsl;

    /**
     * Cesta k unikátnemu identifikátoru balíku
     */
    @Embedded
    private PathToSipId pathToSipId = new PathToSipId();

    /**
     * Cesta k hlavému metadátovému súboru SIPu (napr. METS.xml)
     */
    @Column(length = 10485760)
    private String sipMetadataPath;

    /**
     * Typ SIP balíku
     */
    @Enumerated(EnumType.STRING)
    @NotNull
    private PackageType packageType;
}
