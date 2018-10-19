package cz.cas.lib.core.domain;

import cz.cas.lib.core.index.Labeled;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;

/**
 * Building block for dictionary like JPA entities with enum Code
 * <p>
 * See {@link DictionaryObject} for definition of DictionaryObject
 * <p>
 * This class adds enumarated code, which is useful, when one want to have an editable dictionary
 * but tle application logic depends on selected value (we can not use ID because it will be different on
 * every instance).
 * </p>
 */
@Getter
@Setter
@MappedSuperclass
public abstract class CodedObject<T extends Enum & Labeled> extends DictionaryObject {
    @Enumerated(EnumType.STRING)
    protected T code;
}
