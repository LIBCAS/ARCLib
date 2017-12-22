
package cz.inqool.uas.index;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;


@Getter
@Setter
@Setting(settingPath = "/es_settings.json")
@Document(indexName = "uas" , type = "test")
public class IndexedTestEntity extends IndexedDatedObject {

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed)
    protected String stringAttribute;

    @Field(type = FieldType.String, analyzer = "folding")
    protected String foldingAttribute;

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

    @Field(type = FieldType.Nested, index = FieldIndex.not_analyzed)
    private Set<LabeledReference> dependents;
}
