package cz.inqool.uas.sequence;

import cz.inqool.uas.index.IndexedDictionaryObject;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.*;

/**
 * Indexed representation of {@link Sequence}.
 */
@Getter
@Setter
@Setting(settingPath = "/es_settings.json")
@Document(indexName = "uas" , type = "sequence")
public class IndexedSequence extends IndexedDictionaryObject {

    @Field(type = FieldType.String, analyzer = "folding")
    protected String format;

    @Field(type = FieldType.Long, index = FieldIndex.not_analyzed)
    protected Long counter;
}
