package cz.cas.lib.core.detach.objects;

import cz.cas.lib.arclib.domainbase.domain.DatedObject;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "object_2")
public class Object2 extends DatedObject {

    @Fetch(FetchMode.SELECT)
    @ManyToOne
    protected Object1 object1;

}
