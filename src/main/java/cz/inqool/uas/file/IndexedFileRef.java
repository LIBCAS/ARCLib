package cz.inqool.uas.file;

import cz.inqool.uas.index.IndexedDatedObject;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.*;

/**
 * Indexed representation of {@link FileRef}.
 */
@Getter
@Setter
@Setting(settingPath = "/es_settings.json")
@Document(indexName = "uas" , type = "fileRef")
public class IndexedFileRef extends IndexedDatedObject {

    @Field(type = FieldType.String, analyzer = "folding")
    protected String name;

    @Field(type = FieldType.String, index = FieldIndex.analyzed)
    protected String content;
}
