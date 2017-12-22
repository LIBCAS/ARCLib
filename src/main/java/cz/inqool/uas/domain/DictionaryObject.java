package cz.inqool.uas.domain;

import cz.inqool.uas.store.DomainStore;
import lombok.Getter;
        import lombok.Setter;

        import javax.persistence.Column;
        import javax.persistence.MappedSuperclass;

/**
 * Building block for dictionary like JPA entities.
 *
 * <p>
 *     Dictionary is a sorted list of possible values for an attribute of another entity, where the value is represented
 *     by an instance of {@link DictionaryObject}. It is something like {@link Enum}, but the possible values are
 *     extensible by adding data to database.
 * </p>
 * <p>
 *     Every instance should have at least {@link DictionaryObject#id},
 *     which is unique, {@link DictionaryObject#name}, which represents the user friendly name,
 *     {@link DictionaryObject#order}, which defines the sorting and {@link DictionaryObject#active}, which allows
 *     the instance to be temporally hidden from further usage without deleting it. Difference between setting
 *     {@link DictionaryObject#active} to false and deleting the inherited {@link DatedObject} is that deactivated
 *     instance is accessible though standard {@link DomainStore#find(String)} API.
 *     It is responsibility of the developer to not show the deactivated instances to user.
 * </p>
 */
@Getter
@Setter
@MappedSuperclass
public abstract class DictionaryObject extends DatedObject {
    protected String name;

    @Column(name = "list_order")
    protected Long order;

    protected Boolean active;
}
