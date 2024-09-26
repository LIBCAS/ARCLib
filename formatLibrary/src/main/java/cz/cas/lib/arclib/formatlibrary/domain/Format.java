package cz.cas.lib.arclib.formatlibrary.domain;

import cz.cas.lib.arclib.domainbase.domain.DatedObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Formát
 */
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_format")
@NoArgsConstructor
@Getter
public class Format extends DatedObject {
    /**
     * ID formátu
     */
    private Integer formatId;

    /**
     * PUID
     */
    private String puid;

    /**
     * Meno formátu
     */
    private String formatName;

    /**
     * Súvisiace riziká
     */
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinTable(name = "arclib_format_r_r",
            joinColumns = {@JoinColumn(name = "format_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "related_risk_id", referencedColumnName = "id")})
    private Set<Risk> relatedRisks = new HashSet<>();

    /**
     * Miera ohrozenia
     */
    @Enumerated(EnumType.STRING)
    private ThreatLevel threatLevel;

    /**
     * Súbory
     */
    @Fetch(FetchMode.SELECT)
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "format_id")
    @OrderBy("created DESC, name")
    private Set<PreservationPlanFileRef> files = new HashSet<>();


    @Override
    public String toString() {
        return "Format{" +
                "puid='" + puid + '\'' +
                ", name='" + formatName + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Format format = (Format) o;
        return Objects.equals(formatId, format.formatId) &&
                Objects.equals(puid, format.puid) &&
                Objects.equals(formatName, format.formatName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(formatId, puid, formatName);
    }
}
