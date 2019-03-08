package cz.cas.lib.arclib.domain.preservationPlanning;

import cz.cas.lib.core.domain.DatedObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Objects;

/**
 * Vývojár formátu
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_format_developer")
@NoArgsConstructor
public class FormatDeveloper extends DatedObject {
    /**
     * Id vývojára
     */
    private Integer developerId;

    /**
     * Meno vývojára
     */
    private String developerName;

    /**
     * Meno organizácie
     */
    private String organisationName;

    /**
     * Zložené meno vývojára
     */
    private String developerCompoundName;

    /**
     * Interné číslo verzie
     */
    private Integer internalVersionNumber;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FormatDeveloper that = (FormatDeveloper) o;
        return Objects.equals(developerId, that.developerId) &&
                Objects.equals(developerName, that.developerName) &&
                Objects.equals(organisationName, that.organisationName) &&
                    Objects.equals(developerCompoundName, that.developerCompoundName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(developerId, developerName, organisationName, developerCompoundName);
    }
}
