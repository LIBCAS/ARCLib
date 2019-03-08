package cz.cas.lib.arclib.domain.preservationPlanning;

import cz.cas.lib.core.domain.DatedObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.Objects;

/**
 * Vzťah medzi formátmi
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_related_format")
@NoArgsConstructor
public class RelatedFormat extends DatedObject {
    /**
     * Typ vťahu
     */
    @Enumerated(EnumType.STRING)
    private FormatRelationshipType relationshipType;

    /**
     * Formátové id súvisiaceho formátu
     */
    private Integer relatedFormatId;

    /**
     * Meno súvisiaceho formátu
     */
    private String relatedFormatName;

    /**
     * Verzia súvisiaceho formátu
     */
    private String relatedFormatVersion;

    /**
     * Interné číslo verzie
     */
    private Integer internalVersionNumber;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RelatedFormat that = (RelatedFormat) o;
        return relationshipType == that.relationshipType &&
                Objects.equals(relatedFormatId, that.relatedFormatId) &&
                Objects.equals(relatedFormatName, that.relatedFormatName) &&
                Objects.equals(relatedFormatVersion, that.relatedFormatVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relationshipType, relatedFormatId, relatedFormatName, relatedFormatVersion);
    }
}
