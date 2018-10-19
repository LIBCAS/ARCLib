package cz.cas.lib.core.domain;

import cz.cas.lib.core.store.InstantGenerator;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.GeneratorType;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.time.Instant;

/**
 * Building block for JPA entities, which want to track creation, update and delete times.
 *
 * <p>
 * Provides attributes {@link DatedObject#created}, {@link DatedObject#updated}, {@link DatedObject#deleted},
 * which are filled accordingly in {@link cz.cas.lib.core.store.DatedStore}.
 * </p>
 *
 * <p>
 * If used with {@link cz.cas.lib.core.store.DatedStore} upon deleting an instance, the instance will not be deleted
 * from database, instead only marked as deleted by setting the {@link DatedObject#deleted} to non null value.
 * </p>
 *
 * <p>
 * {@link DatedObject#updated} wont be changed if no other change happened to the object!
 * </p>
 */
@Getter
@Setter
@MappedSuperclass
public abstract class DatedObject extends DomainObject {
    @Column(updatable = false)
    @GeneratorType(type = InstantGenerator.class, when = GenerationTime.INSERT)
    protected Instant created;

    @GeneratorType(type = InstantGenerator.class, when = GenerationTime.ALWAYS)
    protected Instant updated;

    protected Instant deleted;
}