package cz.inqool.uas.scheduling.run;

import cz.inqool.uas.index.IndexedDatedObject;
import cz.inqool.uas.index.LabeledReference;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.*;

/**
 * Indexed representation of {@link JobRun}.
 */
@Getter
@Setter
@Setting(settingPath = "/es_settings.json")
@Document(indexName = "uas" , type = "job_run")
public class IndexedJobRun extends IndexedDatedObject {
    @Field(type = FieldType.Object, index = FieldIndex.not_analyzed)
    protected LabeledReference job;

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed)
    protected String result;

    @Field(type = FieldType.Boolean, index = FieldIndex.not_analyzed)
    protected Boolean success;

}
