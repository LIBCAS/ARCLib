package cz.cas.lib.arclib.domain;

import cz.cas.lib.arclib.domainbase.domain.NamedObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Dodávateľ
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_producer")
@NoArgsConstructor
public class Producer extends NamedObject {

    public Producer(String id) {
        setId(id);
    }

    /**
     * Cesta do prekladiska
     */
    private String transferAreaPath;

    /**
     * Exportní složky
     */
    @BatchSize(size = 100)
    @Fetch(FetchMode.SELECT)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "arclib_producer_export_folders", joinColumns = @JoinColumn(name = "producer_id"))
    @Column(name = "folder")
    private Set<String> exportFolders = new HashSet<>();
}
