package cz.cas.lib.arclib.domain;

import cz.cas.lib.arclib.domainbase.domain.DomainObject;
import lombok.AllArgsConstructor;
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
 * Seznam ids oblíbených/vybraných dokumentů uživatele.
 */
@Getter
@Setter
@Entity
@Table(name = "arclib_favorites_bucket")
@NoArgsConstructor
@AllArgsConstructor
public class FavoritesBucket extends DomainObject {

    /**
     * Užívateľ
     */
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    /**
     * IDs
     */
    @BatchSize(size = 100)
    @Fetch(FetchMode.SELECT)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "arclib_favorites_ids", joinColumns = @JoinColumn(name = "bucket_id"))
    @Column(name = "id")
    private Set<String> favoriteIds = new HashSet<>();
}
