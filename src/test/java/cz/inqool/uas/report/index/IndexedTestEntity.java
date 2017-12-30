package cz.inqool.uas.report.index;

import cz.inqool.uas.index.IndexedDatedObject;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.*;


@Getter
@Setter
@Setting(settingPath = "/es_settings.json")
@Document(indexName = "uas" , type = "test_report_indexed")
public class IndexedTestEntity extends IndexedDatedObject {

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed)
    protected String stringAttribute;
}
