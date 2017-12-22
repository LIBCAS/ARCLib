package cz.inqool.uas.dictionary;

import cz.inqool.uas.index.IndexedDictionaryObject;
import cz.inqool.uas.index.LabeledReference;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.*;

/**
 * Indexed representation of {@link Dictionary}.
 */
@Getter
@Setter
@Setting(settingPath = "/es_settings.json")
@Document(indexName = "uas" , type = "dictionary")
public class IndexedDictionary extends IndexedDictionaryObject {
    @Field(type = FieldType.String, analyzer = "folding")
    protected String description;

    @Field(type = FieldType.Object, index = FieldIndex.not_analyzed)
    protected LabeledReference parent;

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed)
    protected String code;
}
