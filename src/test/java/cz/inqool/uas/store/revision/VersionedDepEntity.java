package cz.inqool.uas.store.revision;

import cz.inqool.uas.domain.DomainObject;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.Table;

@Audited
@Getter
@Setter
@Entity
@AuditTable("test_versioned_dep_aud")
@Table(name = "test_versioned_dep")
public class VersionedDepEntity extends DomainObject {
    private String value;
}
