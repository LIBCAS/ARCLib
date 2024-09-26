package cz.cas.lib.arclib.domainbase.domain;

import cz.cas.lib.arclib.domainbase.store.DatedStore;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Building block for JPA entities, which want to track creation, update and delete times.
 *
 * <p>
 * Provides attributes {@link DatedObject#created}, {@link DatedObject#updated}, {@link DatedObject#deleted},
 * which are filled accordingly in {@link DatedStore}.
 * </p>
 *
 * <p>
 * If used with {@link DatedStore} upon deleting an instance, the instance will not be deleted
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
    @CreationTimestamp
    protected Instant created;

    @UpdateTimestamp
    protected Instant updated;

    protected Instant deleted;
}