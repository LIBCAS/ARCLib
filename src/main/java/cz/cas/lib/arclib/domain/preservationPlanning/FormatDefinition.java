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
import java.util.Objects;
import java.util.Set;

/**
 * Definícia formátu
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_format_definition")
@NoArgsConstructor
public class FormatDefinition extends DatedObject {

    /**
     * Formát
     */
    @ManyToOne(cascade = CascadeType.MERGE)
    private Format format;

    /**
     * Verzia formátu
     */
    private String formatVersion;

    /**
     * Ďalšie pomenovania
     */
    @BatchSize(size = 100)
    @Fetch(FetchMode.SELECT)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "arclib_format_definition_a", joinColumns = @JoinColumn(name = "arclib_format_definition_id"))
    @Column(name = "alias")
    private Set<String> aliases = new HashSet<>();

    /**
     * Identifikátory
     */
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinTable(name = "arclib_format_definition_i",
            joinColumns = {@JoinColumn(name = "format_definition_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "format_identifier_id", referencedColumnName = "id")})
    private Set<FormatIdentifier> identifiers = new HashSet<>();

    /**
     * Formátové rodiny
     */
    @BatchSize(size = 100)
    @Fetch(FetchMode.SELECT)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "arclib_format_definition_f_f", joinColumns = @JoinColumn(name = "arclib_format_definition_id"))
    @Column(name = "format_family")
    private Set<String> formatFamilies = new HashSet<>();

    /**
     * Klasifikácie formátu
     */
    @BatchSize(size = 100)
    @Fetch(FetchMode.SELECT)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "arclib_format_definition_f_c", joinColumns = @JoinColumn(name = "arclib_format_definition_id"))
    @Column(name = "format_classification")
    @Enumerated(EnumType.STRING)
    private Set<FormatClassification> formatClassifications = new HashSet<>();

    /**
     * Popis
     */
    @Column(length = 10485760)
    private String formatDescription;

    /**
     * Súvisiace formáty
     */
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinTable(name = "arclib_format_definition_r_f",
            joinColumns = {@JoinColumn(name = "format_definition_id", referencedColumnName = "id")},
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
     * Vývojári formátu
     */
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinTable(name = "arclib_format_definition_d",
            joinColumns = {@JoinColumn(name = "format_definition_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "format_developer_id", referencedColumnName = "id")})
    private Set<FormatDeveloper> developers = new HashSet<>();

    /**
     * Poznámka
     */
    private String formatNote;

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
     * Prechádzajúca verzia formátovej definície
     */
    @OneToOne
    private FormatDefinition previousInternalDefinition;

    /**
     * Interné číslo verzie formátu
     */
    private Integer internalVersionNumber;

    /**
     * Výskyt formátů podle profilů producerů
     */
    @OneToMany(mappedBy = "formatDefinition", fetch = FetchType.EAGER)
    private Set<FormatOccurrence> formatOccurrences = new HashSet<>();

    /**
     * Jedná sa o lokálnu definíciu
     */
    boolean localDefinition;

    /**
     * Jedná sa o preferovanú verziu
     */
    boolean preferred;

    /**
     * Boli vyplnené interné informácie
     */
    boolean internalInformationFilled;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FormatDefinition that = (FormatDefinition) o;
        return Objects.equals(format, that.format) &&
                Objects.equals(formatVersion, that.formatVersion) &&
                Objects.equals(aliases, that.aliases) &&
                Objects.equals(identifiers, that.identifiers) &&
                Objects.equals(formatFamilies, that.formatFamilies) &&
                Objects.equals(formatClassifications, that.formatClassifications) &&
                Objects.equals(formatDescription, that.formatDescription) &&
                Objects.equals(relatedFormats, that.relatedFormats) &&
                Objects.equals(releaseDate, that.releaseDate) &&
                Objects.equals(withdrawnDate, that.withdrawnDate) &&
                Objects.equals(developers, that.developers) &&
                Objects.equals(formatNote, that.formatNote) &&
                Objects.equals(nationalFormatGuarantor, that.nationalFormatGuarantor) &&
                Objects.equals(preservationPlanDescription, that.preservationPlanDescription) &&
                Objects.equals(preservationPlanFile, that.preservationPlanFile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(format, formatVersion, aliases, identifiers, formatFamilies,
                formatClassifications, formatDescription, relatedFormats, releaseDate, withdrawnDate,
                developers, formatNote, nationalFormatGuarantor, preservationPlanDescription, preservationPlanFile);
    }

    @Override
    public String toString() {
        return "FormatDefinition{" +
                (format != null ? "puid=" + format.getPuid() + ", " : "") +
                "formatVersion=" + formatVersion +
                ", localDefinition=" + localDefinition +
                ", internalInformationFilled=" + internalInformationFilled +
                '}';
    }
}
