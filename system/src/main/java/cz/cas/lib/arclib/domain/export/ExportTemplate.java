package cz.cas.lib.arclib.domain.export;

import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.domainbase.domain.NamedObject;
import cz.cas.lib.arclib.domainbase.util.ArrayJsonConverter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import java.util.List;

/**
 * Exportní šablona
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_export_template")
@NoArgsConstructor
public class ExportTemplate extends NamedObject {

    @ManyToOne
    private Producer producer;

    private String description;

    @Embedded
    private DataReduction dataReduction;

    private boolean generateInfoFile;

    @Convert(converter = ArrayJsonConverter.class)
    private List<String> metadataSelection;
}
