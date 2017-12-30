package cz.inqool.uas.action;

import cz.inqool.uas.index.IndexedDictionaryObject;
import cz.inqool.uas.index.LabeledReference;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.*;

/**
 * Indexed representation of {@link Action}.
 */
@Getter
@Setter
@Setting(settingPath = "/es_settings.json")
@Document(indexName = "uas" , type = "action")
public class IndexedAction extends IndexedDictionaryObject {

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed)
    protected String code;

    @Field(type = FieldType.Object, index = FieldIndex.not_analyzed)
    protected LabeledReference scriptType;
}
