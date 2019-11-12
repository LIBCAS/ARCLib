package cz.cas.lib.core.index.solr;

import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.index.solr.IndexQueryUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.solr.core.mapping.Indexed;

/**
 * Represents one index object field ES mapping, provides methods for retrieval of field names with proper suffixes
 * during search query build.
 * This class exists mainly to support and enforce following behavior:
 * <ul>
 * <li>
 * {@link #fieldType} is known when constructing filters using {@link IndexQueryUtils}, so that proper filter is constructed.
 * </li>
 * <li>
 * EQ/SUFFIX/PREFIX queries and sorting is not allowed on fields of type {@link IndexFieldType#TEXT} unless there is a {@link Indexed#copyTo()}
 * attribute with {@link IndexField#STRING_SUFFIX} (for EQ/SUFFIX/PREFIX queries) / {@link IndexField#SORT_SUFFIX} (for sort support). This is needed because otherwise EQ/SUFFIX/PREFIX queries does not work well on tokenized field types like ({@link IndexFieldType#TEXT}).
 * </li>
 * <li>
 * If there is a {@link Indexed#copyTo()} annotation with {@link IndexField#SORT_SUFFIX}, this field is used for sorting instead of the
 * main one. The same applies for {@link Indexed#copyTo()} annotation with {@link IndexField#STRING_SUFFIX} used for EQ/SUFFIX/PREFIX queries.
 * </li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
public class IndexField {
    public static final String SORT_SUFFIX = "_sort";
    public static final String STRING_SUFFIX = "_s";
    /**
     * used for CONTAINS and other queries
     */
    private String fieldName;
    private String fieldType;
    /**
     * used for sorting
     */
    private String sortField;
    /**
     * used for EQ, SUFFIX, PREFIX queries
     */
    private String keywordField;

    public IndexField(java.lang.reflect.Field field) {
        Field fieldA = field.getAnnotation(Field.class);
        Indexed indexedA = field.getAnnotation(Indexed.class);
        fieldName = parseFieldName(indexedA, fieldA, field.getName());
        fieldType = indexedA.type();
        if (fieldType.isEmpty())
            throw new GeneralException("missing @Indexed.fieldType property of field: " + field.getName() + " of class: " + field.getClass());
        if (!IndexFieldType.TEXT.equals(fieldType)) {
            keywordField = fieldName;
            sortField = fieldName;
        }
        for (String s : indexedA.copyTo()) {
            if (s.endsWith(STRING_SUFFIX)) {
                keywordField = s;
            }
            if (s.endsWith(SORT_SUFFIX)) {
                sortField = s;
            }
        }
    }

    public IndexField(String fieldName, String fieldType, String sortField, String keywordField) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        boolean notTextType = !IndexFieldType.TEXT.equals(fieldType);
        this.sortField = sortField == null && notTextType ? fieldName : sortField;
        this.keywordField = keywordField == null && notTextType ? fieldName : keywordField;
    }

    private String parseFieldName(Indexed indexedA, Field fieldA, String javaFieldName) {
        if (!"".equals(indexedA.name()))
            return indexedA.name();
        if (!"".equals(indexedA.value()))
            return indexedA.value();
        if (!DocumentObjectBinder.DEFAULT.equals(fieldA.value()))
            return fieldA.value();
        return javaFieldName;
    }
}

