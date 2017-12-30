package cz.inqool.uas.security.authorization.role;

import cz.inqool.uas.domain.DatedObject;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.Set;

@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "uas_role")
public class Role extends DatedObject {
    protected String name;

    @Lob
    protected String description;

    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 100)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="uas_role_permission", joinColumns=@JoinColumn(name="role_id"))
    @Column(name = "permission")
    protected Set<String> permissions;

    @Fetch(FetchMode.SELECT)
    @ManyToOne
    protected Role parent;
}
