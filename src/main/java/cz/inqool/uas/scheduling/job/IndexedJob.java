package cz.inqool.uas.scheduling.job;

import cz.inqool.uas.index.IndexedDatedObject;
import cz.inqool.uas.index.LabeledReference;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.*;

/**
 * Indexed representation of {@link Job}.
 */
@Getter
@Setter
@Setting(settingPath = "/es_settings.json")
@Document(indexName = "uas" , type = "job")
public class IndexedJob extends IndexedDatedObject {
    @Field(type = FieldType.String, analyzer = "folding")
    protected String name;

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed)
    protected String timing;

    @Field(type = FieldType.Object, index = FieldIndex.not_analyzed)
    protected LabeledReference scriptType;

    @Field(type = FieldType.Boolean, index = FieldIndex.not_analyzed)
    protected Boolean active;
}
