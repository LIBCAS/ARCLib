package cz.cas.lib.arclib.domain.packages;

import cz.cas.lib.arclib.domain.Hash;
import cz.cas.lib.core.domain.DatedObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Sip balík
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "arclib_sip")
public class Sip extends DatedObject {
    /**
     * Autorský balíček
     */
    @ManyToOne
    private AuthorialPackage authorialPackage;

    /**
     * Zoznam hashov SIP balíku
     */
    @OneToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "arclib_sip_h",
            joinColumns = {@JoinColumn(name = "sip_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "hash_id", referencedColumnName = "id")})
    private Set<Hash> hashes = new HashSet<>();


    /**
     * Sip balík predchádzajúcej verzie
     */
    @OneToOne
    private Sip previousVersionSip;

    /**
     * Stromová štruktúra obsahu balíku
     */
    @Column(length = 10485760)
    private FolderStructure folderStructure;

    /**
     * Číslo verzie
     */
    private Integer versionNumber;
}
