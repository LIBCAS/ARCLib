package cz.cas.lib.arclib.formatlibrary.domain;

import cz.cas.lib.arclib.domainbase.domain.DatedObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Objects;

/**
 * Identifikátor formátu
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_format_identifier")
@NoArgsConstructor
public class FormatIdentifier extends DatedObject {

    public static final String FORMAT_ID_TYPE_PUID = "PUID";
    public static final String FORMAT_ID_TYPE_MIME = "MIME";

    private String identifierType;

    /**
     * Hodnota identifikátora formátu
     */
    private String identifier;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FormatIdentifier that = (FormatIdentifier) o;
        return identifierType == that.identifierType &&
                Objects.equals(identifier, that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifierType, identifier);
    }
}
