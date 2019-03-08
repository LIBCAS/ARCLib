package cz.cas.lib.arclib.domain.preservationPlanning;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.core.domain.DatedObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Počet výskytu formátu pro daný dodavatelský profil
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_format_occurrence")
@NoArgsConstructor
@AllArgsConstructor
public class FormatOccurrence extends DatedObject {

    /**
     * Definícia formátu
     */
    @ManyToOne
    @JoinColumn(name = "format_definition_id")
    @JsonIgnore
    private FormatDefinition formatDefinition;

    /**
     * Počet výskytů formátu
     */
    private long occurrences;

    /**
     * Dodavatelský profil
     */
    @ManyToOne
    private ProducerProfile producerProfile;
}
