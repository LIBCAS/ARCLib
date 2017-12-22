package cz.inqool.uas.index;

import cz.inqool.uas.domain.DatedObject;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldIndex;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;
import java.util.Date;

/**
 * Building block for Elasticsearch entities, which want to track creation and update.
 *
 * <p>
 *     Provides attributes {@link IndexedDatedObject#created} and {@link IndexedDatedObject#updated}.
 * </p>
 *
 * <p>
 *     Unlike {@link DatedObject} there is no deleted attribute. Deleted entites are always removed from Elasticsearch.
 *     Also unlike {@link DatedObject} the attributes are of {@link Date} type, because Elasticsearch does not
 *     understand Java 8 Time classes.
 * </p>
 */

@Getter
@Setter
public abstract class IndexedDatedObject extends IndexedDomainObject {
    @Field(type = FieldType.Date, index = FieldIndex.not_analyzed)
    protected Instant created;

    @Field(type = FieldType.Date, index = FieldIndex.not_analyzed)
    protected Instant updated;
}
