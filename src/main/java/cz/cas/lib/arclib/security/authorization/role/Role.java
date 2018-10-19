package cz.cas.lib.arclib.security.authorization.role;

import cz.cas.lib.core.domain.DatedObject;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Type;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;

@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_role")
public class Role extends DatedObject {

    protected String name;

    @Lob
    @Type(type = "org.hibernate.type.TextType")
    protected String description;
}
