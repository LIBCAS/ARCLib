package cz.cas.lib.arclib.formatlibrary.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import cz.cas.lib.arclib.domainbase.domain.DatedObject;
import cz.cas.lib.arclib.domainbase.dto.IdSerializer;
import cz.cas.lib.arclib.formatlibrary.dto.FormatDefinitionIdDeserializer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import java.util.Objects;

/**
 * Vzťah medzi formátmi
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_format_related_format")
@NoArgsConstructor
public class RelatedFormat extends DatedObject {

    @ManyToOne
    @JsonSerialize(using = IdSerializer.class)
    @JsonDeserialize(using = FormatDefinitionIdDeserializer.class)
    private FormatDefinition formatDefinition;
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
     * WARNING Equals does not check for related format definition equality.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RelatedFormat that = (RelatedFormat) o;
        return
                relationshipType == that.relationshipType &&
                        Objects.equals(relatedFormatId, that.relatedFormatId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relationshipType, relatedFormatId);
    }
}
