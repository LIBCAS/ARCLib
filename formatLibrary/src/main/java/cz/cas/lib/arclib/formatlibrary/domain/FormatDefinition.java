package cz.cas.lib.arclib.formatlibrary.domain;

import cz.cas.lib.arclib.domainbase.domain.DatedObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import jakarta.persistence.*;
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
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
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
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE, mappedBy = "formatDefinition")
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
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinTable(name = "arclib_format_definition_d",
            joinColumns = {@JoinColumn(name = "format_definition_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "format_developer_id", referencedColumnName = "id")})
    private Set<FormatDeveloper> developers = new HashSet<>();

    /**
     * Poznámka
     */
    @Column(length = 10485760)
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
     * Prechádzajúca verzia formátovej definície
     */
    @OneToOne
    private FormatDefinition previousInternalDefinition;

    /**
     * Interné číslo verzie formátu, lokální a upstream definice jsou číslovány separátně
     */
    private Integer internalVersionNumber;

    /**
     * Jedná sa o lokálnu definíciu
     */
    boolean localDefinition;

    /**
     * Jedná sa o preferovanú verziu, ze všech verzí pro daný formát je taková pouze jedna
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
                Objects.equals(trim(formatDescription), trim(that.formatDescription)) &&
                Objects.equals(relatedFormats, that.relatedFormats) &&
                Objects.equals(releaseDate, that.releaseDate) &&
                Objects.equals(withdrawnDate, that.withdrawnDate) &&
                Objects.equals(developers, that.developers) &&
                Objects.equals(trim(formatNote), trim(that.formatNote)) &&
                Objects.equals(trim(nationalFormatGuarantor), trim(that.nationalFormatGuarantor)) &&
                Objects.equals(trim(preservationPlanDescription), trim(that.preservationPlanDescription));
    }

    @Override
    public int hashCode() {
        return Objects.hash(format, formatVersion, aliases, identifiers, formatFamilies,
                formatClassifications, formatDescription, relatedFormats, releaseDate, withdrawnDate,
                developers, formatNote, nationalFormatGuarantor, preservationPlanDescription);
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

    private String trim(String s) {
        if (s == null)
            return s;
        return s.replaceAll("\\s", "");
    }
}
