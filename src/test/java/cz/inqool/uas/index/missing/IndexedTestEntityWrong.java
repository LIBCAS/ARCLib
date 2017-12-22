package cz.inqool.uas.index.missing;

import cz.inqool.uas.index.IndexedDatedObject;
import cz.inqool.uas.index.LabeledReference;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldIndex;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.Instant;
import java.time.LocalDate;


@Getter
@Setter
@Setting(settingPath = "/es_settings.json")
public class IndexedTestEntityWrong extends IndexedDatedObject {

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed)
    protected String stringAttribute;

    @Field(type = FieldType.Integer, index = FieldIndex.not_analyzed)
    private Integer intAttribute;

    @Field(type = FieldType.Double, index = FieldIndex.not_analyzed)
    private Double doubleAttribute;

    @Field(type = FieldType.Date, index = FieldIndex.not_analyzed)
    private LocalDate localDateAttribute;

    @Field(type = FieldType.Date, index = FieldIndex.not_analyzed)
    private Instant instantAttribute;

    @Field(type = FieldType.Object, index = FieldIndex.not_analyzed)
    private LabeledReference dependent;
}
