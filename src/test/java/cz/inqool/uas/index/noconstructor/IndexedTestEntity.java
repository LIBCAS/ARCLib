package cz.inqool.uas.index.noconstructor;

import cz.inqool.uas.index.IndexedDatedObject;
import cz.inqool.uas.index.LabeledReference;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.Instant;
import java.time.LocalDate;


@AllArgsConstructor
@Getter
@Setter
@Setting(settingPath = "/es_settings.json")
@Document(indexName = "uas" , type = "test_construct")
public class IndexedTestEntity extends IndexedDatedObject {

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
