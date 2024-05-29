package cz.cas.lib.arclib.domain.packages;

import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.domainbase.domain.DatedObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

/**
 * Hromadné mazání
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "arclib_aip_bulk_deletion")
public class AipBulkDeletion extends DatedObject {

    /**
     * Uživatel
     */
    @ManyToOne
    @NotNull
    private User creator;

    /**
     * Dodávateľ
     */
    @ManyToOne
    private Producer producer;

    @Enumerated(EnumType.STRING)
    private AipBulkDeletionState state;

    private int deletedCount;

    /**
     * Seznam identifikátorů
     */
    @BatchSize(size = 100)
    @Fetch(FetchMode.SELECT)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "arclib_aip_bulk_deletion_ids", joinColumns = @JoinColumn(name = "aip_bulk_deletion_id"))
    @Column(name = "aip_id")
    private Set<String> aipIds = new HashSet<>();

    /**
     * Přeskočit pokud novější verze není persistentní
     */
    private boolean deleteIfNewerVersionsDeleted;

    public int getSize() {
        return aipIds.size();
    }
}
