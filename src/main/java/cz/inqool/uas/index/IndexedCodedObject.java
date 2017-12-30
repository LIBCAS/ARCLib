package cz.inqool.uas.index;

import cz.inqool.uas.domain.CodedObject;
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
 *     To understand the dictionary concept {@link DictionaryObject } and {@link CodedObject}.
 * </p>
 */
@Getter
@Setter
@Setting(settingPath = "/es_settings.json")
public class IndexedCodedObject extends IndexedDictionaryObject {
    @Field(type = FieldType.Object, index = FieldIndex.not_analyzed)
    protected LabeledReference code;
}
