package cz.inqool.uas.report;

import cz.inqool.uas.index.IndexedDictionaryObject;
import cz.inqool.uas.index.LabeledReference;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.*;

/**
 * Indexed representation of {@link Report}.
 */
@Getter
@Setter
@Setting(settingPath = "/es_settings.json")
@Document( indexName = "uas" , type = "report")
public class IndexedReport extends IndexedDictionaryObject {

    @Field(type = FieldType.String, analyzer = "folding")
    private String label;

    @Field(type = FieldType.String, analyzer = "folding")
    private String provider;

    @Field(type = FieldType.String, analyzer = "folding")
    private String params;

    @Field(type = FieldType.Object, index = FieldIndex.not_analyzed)
    private LabeledReference location;

    @Field(type = FieldType.Object, index = FieldIndex.not_analyzed)
    private LabeledReference form;

    @Field(type = FieldType.String, analyzer = "folding")
    private String fileName;
}
