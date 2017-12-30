package cz.inqool.uas.index;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldIndex;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

/**
 * Reference to another Elasticsearch entity or {@link Enum} value.
 *
 * <p>
 *     Objects in Elasticsearch can not be connected like JPA entities. Therefore we store only the referencing
 *     {@link LabeledReference#id} and {@link LabeledReference#name} attribute, which corresponds to label of that
 *     entity.
 * </p>
 * <p>
 *     In case of linking to {@link IndexedDictionaryObject} the {@link LabeledReference#name} is the
 *     {@link IndexedDictionaryObject#name}.
 * </p>
 * <p>
 *     If used as reference to {@link Enum} then, the {@link LabeledReference#id} becomes the {@link Enum#name} and
 *     {@link LabeledReference#name} should be set to something the user see and filter on.
 * </p>
 *
 * folding was removed from id attribute = it might brake applications which send the enum filter value lower-cased
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Setting(settingPath = "/es_settings.json")
public class LabeledReference {
    // needs to be folding, because of enum ids
    @Field(type = FieldType.String, index = FieldIndex.not_analyzed)
    protected String id;

    @Field(type = FieldType.String, analyzer = "folding")
    protected String name;
}
