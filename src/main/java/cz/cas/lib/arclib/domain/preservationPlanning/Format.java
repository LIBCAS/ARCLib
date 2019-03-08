package cz.cas.lib.arclib.domain.preservationPlanning;


import cz.cas.lib.core.domain.DatedObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Objects;
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
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinTable(name = "arclib_format_r_r",
            joinColumns = {@JoinColumn(name = "format_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "related_risk_id", referencedColumnName = "id")})
    private Set<Risk> relatedRisks = new HashSet<>();

    /**
     * Miera ohrozenia
     */
    @Enumerated(EnumType.STRING)
    private ThreatLevel threatLevel;


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
