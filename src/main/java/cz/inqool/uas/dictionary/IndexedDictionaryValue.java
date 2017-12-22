package cz.inqool.uas.dictionary;

import cz.inqool.uas.index.IndexedDictionaryObject;
import cz.inqool.uas.index.LabeledReference;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;

/**
 * Indexed representation of {@link DictionaryValue}.
 */
@Getter
@Setter
@Setting(settingPath = "/es_settings.json")
@Document(indexName = "uas" , type = "dictionaryValue")
public class IndexedDictionaryValue extends IndexedDictionaryObject {
    // needs to be folding, because of enum ids
    @Field(type = FieldType.Object, index = FieldIndex.not_analyzed)
    protected LabeledReference dictionary;

    @Field(type = FieldType.Object, index = FieldIndex.not_analyzed)
    protected LabeledReference parent;

    @Field(type = FieldType.String, analyzer = "folding")
    protected String description;

    @Field(type = FieldType.Date, index = FieldIndex.not_analyzed)
    protected LocalDateTime validFrom;

    @Field(type = FieldType.Date, index = FieldIndex.not_analyzed)
    protected LocalDateTime validTo;

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed)
    protected String code;
}
