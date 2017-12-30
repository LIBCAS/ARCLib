package cz.inqool.uas.error;

import cz.inqool.uas.index.IndexedDatedObject;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.*;

/**
 * Indexed representation of {@link Error}.
 */
@Getter
@Setter
@Setting(settingPath = "/es_settings.json")
@Document(indexName = "uas" , type = "error")
public class IndexedError extends IndexedDatedObject {

    @Field(type = FieldType.Boolean, index = FieldIndex.not_analyzed)
    private Boolean clientSide;

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed)
    private String userId;

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed)
    private String ip;

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed)
    private String url;

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed)
    private String userAgent;
}
