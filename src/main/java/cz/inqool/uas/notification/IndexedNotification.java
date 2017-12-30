package cz.inqool.uas.notification;

import cz.inqool.uas.index.IndexedDatedObject;
import cz.inqool.uas.index.LabeledReference;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.*;

/**
 * Indexed representation of {@link Notification}.
 */
@Getter
@Setter
@Setting(settingPath = "/es_settings.json")
@Document(indexName = "uas" , type = "notification")
public class IndexedNotification extends IndexedDatedObject {

    @Field(type = FieldType.String, analyzer = "folding")
    protected String title;

    @Field(type = FieldType.Object, index = FieldIndex.not_analyzed)
    protected LabeledReference author;

    @Field(type = FieldType.Object, index = FieldIndex.not_analyzed)
    protected LabeledReference recipient;

    @Field(type = FieldType.Boolean, index = FieldIndex.not_analyzed)
    protected Boolean flash;

    @Field(type = FieldType.Boolean, index = FieldIndex.not_analyzed)
    protected Boolean read;

    @Field(type = FieldType.Boolean, index = FieldIndex.not_analyzed)
    protected Boolean emailing;
}
