package cz.inqool.uas.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.util.UUID;

/**
 * Basic building block for every JPA entity.
 *
 * <p>
 * Defines attribute {@link DomainObject#id} of type {@link String}, which is initialized to a
 * random {@link UUID} upon creation.
 * </p>
 *
 * <p>
 * Also implements {@link DomainObject#equals} and {@link DomainObject#hashCode()} based on {@link DomainObject#id}
 * equivalence and {@link DomainObject#toString()} method returning concrete class name with {@link DomainObject#id}
 * to easily log interesting entity.
 * </p>
 */
@EqualsAndHashCode(of = "id")
@Getter
@Setter
@MappedSuperclass
public abstract class DomainObject {
    @Id
    protected String id = UUID.randomUUID().toString();

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + id;
    }
}
