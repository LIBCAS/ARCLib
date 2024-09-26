package cz.cas.lib.arclib.domain.packages;

import cz.cas.lib.arclib.domain.Hash;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflowState;
import cz.cas.lib.arclib.domainbase.domain.DatedObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import jakarta.persistence.*;
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
     * Zoznam hashov SIP balíku (dodaných Dodavatelem, případně přegenerovaných po obohacení balíčku)
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
     * Stromová štruktúra obsahu AIP balíku (vygenerována na konci ingestu)
     */
    @Column(length = 10485760)
    private FolderStructure folderStructure;

    /**
     * Číslo verzie
     */
    private Integer versionNumber;

    /**
     * Velikost obsahu AIP balíku (velikost ZIPu, vygenerována na konci ingest)
     */
    private Long sizeInBytes;

    /**
     * Jedná sa o najnovšiu verziu SIP, flag je nastaven až po úspešném přepnutí do stavu {@link IngestWorkflowState#PERSISTED}
     */
    private boolean isLatestVersion;
}
