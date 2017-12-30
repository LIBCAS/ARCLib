package cz.inqool.uas.store.revision;

import cz.inqool.uas.domain.DomainObject;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Audited
@Getter
@Setter
@Entity
@AuditTable("test_versioned_aud")
@Table(name = "test_versioned")
public class VersionedEntity extends DomainObject {
    private String test;

    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 100)
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "parent")
    @AuditJoinTable(name = "test_versioned_deps_aud")
    private Set<VersionedDepEntity> deps = new HashSet<>();
}
