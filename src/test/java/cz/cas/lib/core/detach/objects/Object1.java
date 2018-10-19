package cz.cas.lib.core.detach.objects;

import cz.cas.lib.core.domain.DatedObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Set;

@NoArgsConstructor
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "object_1")
public class Object1 extends DatedObject {

    @BatchSize(size = 100)
    @Fetch(FetchMode.SELECT)
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "object1")
    protected Set<Object2> object2Set;
}
