package cz.cas.lib.arclib.domain.preservationPlanning;


import cz.cas.lib.core.domain.DatedObject;
import cz.cas.lib.core.file.FileRef;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Formát
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_format")
@NoArgsConstructor
public class Format extends DatedObject {
    /**
     * Meno formátu
     */
    private String name;

    /**
     * Verzia formátu
     */
    private String version;

    /**
     * Ďalšie pomenovania
     */
    @BatchSize(size = 100)
    @Fetch(FetchMode.SELECT)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "arclib_format_o_n", joinColumns = @JoinColumn(name = "arclib_format_id"))
    @Column(name = "other_name")
    private Set<String> otherNames;

    /**
     * Identifikátory
     */
    @OneToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "arclib_format_i",
            joinColumns = {@JoinColumn(name = "format_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "format_identifier_id", referencedColumnName = "id")})
    private Set<FormatIdentifier> identifiers;

    /**
     * Formátové rodiny
     */
    @BatchSize(size = 100)
    @Fetch(FetchMode.SELECT)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "arclib_format_f_f", joinColumns = @JoinColumn(name = "arclib_format_id"))
    @Column(name = "format_family")
    private Set<String> formatFamilies = new HashSet<>();

    /**
     * Klasifikácia
     */
    private String classification;

    /**
     * Popis
     */
    private String description;

    /**
     * Súvisiace formáty
     */
    @OneToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "arclib_format_r_f",
            joinColumns = {@JoinColumn(name = "format_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "related_format_id", referencedColumnName = "id")})
    private Set<RelatedFormat> relatedFormats = new HashSet<>();

    /**
     * Dátum vydania
     */
    private Instant releaseDate;

    /**
     * Dátum odobratia
     */
    private Instant withdrawnDate;

    /**
     * Tvorcovia
     */
    @BatchSize(size = 100)
    @Fetch(FetchMode.SELECT)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "arclib_format_d", joinColumns = @JoinColumn(name = "arclib_format_id"))
    @Column(name = "developer")
    private Set<String> developers = new HashSet<>();

    /**
     * Poznámka
     */
    private String note;

    /**
     * Národný garant formátu
     */
    private String nationalFormatGuarantor;

    /**
     * Plán ochrany - popis
     */
    private String preservationPlanDescription;

    /**
     * Plán ochrany - súbor
     */
    @OneToOne
    private FileRef preservationPlanFile;

    /**
     * Predchádzajúca interná verzia formátu
     */
    @OneToOne
    private Format previousInternalVersion;

    /**
     * Interné číslo verzie formátu
     */
    private Integer internalVersionNumber;

    /**
     * Súvisiace nástroje
     */
    @OneToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "arclib_format_r_t",
            joinColumns = {@JoinColumn(name = "format_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "tool_id", referencedColumnName = "id")})
    private Set<Tool> relatedTools = new HashSet<>();

    /**
     * Súvisiace chyby
     */
    @OneToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "arclib_format_r_e",
            joinColumns = {@JoinColumn(name = "format_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "related_error_id", referencedColumnName = "id")})
    private Set<RelatedError> relatedErrors = new HashSet<>();

    /**
     * Súvisiace riziká
     */
    @OneToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "arclib_format_r_r",
            joinColumns = {@JoinColumn(name = "format_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "related_risk_id", referencedColumnName = "id")})
    private Set<Risk> relatedRisks = new HashSet<>();

    /**
     * Miera ohrozenia
     */
    @Enumerated(EnumType.STRING)
    private ThreatLevel threatLevel;
}
