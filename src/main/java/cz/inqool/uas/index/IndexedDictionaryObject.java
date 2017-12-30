package cz.inqool.uas.index;

import cz.inqool.uas.domain.DictionaryObject;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldIndex;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

/**
 * Building block for dictionary like Elasticsearch entities.
 *
 * <p>
 *     To understand the dictionary concept {@link DictionaryObject }.
 * </p>
 */
@Getter
@Setter
@Setting(settingPath = "/es_settings.json")
public class IndexedDictionaryObject extends IndexedDatedObject {
    @Field(type = FieldType.String, analyzer = "folding")
    protected String name;

    @Field(type = FieldType.Long, index = FieldIndex.not_analyzed)
    protected Long order;

    @Field(type = FieldType.Boolean, index = FieldIndex.not_analyzed)
    protected Boolean active;
}
