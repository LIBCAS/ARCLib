package cz.inqool.uas.index;

import cz.inqool.uas.domain.DomainObject;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldIndex;
import org.springframework.data.elasticsearch.annotations.FieldType;

import javax.persistence.Id;

/**
 * Basic building block for every Elasticsearch entity.
 *
 * <p>
 *     Defines attribute {@link IndexedDomainObject#id} of type {@link String}.
 * </p>
 * <p>
 *     Always needs to have no arg constructor, otherwise exceptions will be thrown
 *     in {@link IndexedStore#save(DomainObject)}.
 * </p>
 */
@Getter
@Setter
public abstract class IndexedDomainObject {
    @Id
    @Field(type = FieldType.String, index = FieldIndex.not_analyzed)
    protected String id;
}
