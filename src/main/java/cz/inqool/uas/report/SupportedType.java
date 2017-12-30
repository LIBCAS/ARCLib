package cz.inqool.uas.report;

import cz.inqool.uas.index.Labeled;
import lombok.Getter;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Supported report types
 *
 * <p>
 *     Each report has a name, a template content type and a result content type. If no result content type
 *     is specified, then input content type is used instead.
 * </p>
 */
@Getter
public enum SupportedType implements Labeled {
    XSLX ("Excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", null),
    DOCX ("Word", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", null),
    PPTX ("PowerPoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation", null),
    HTML ("Html", "text/html", "application/pdf");

    private String label;
    private String contentType;
    private String resultType;

    SupportedType(String label, String contentType, String resultType) {
        this.label = label;
        this.contentType = contentType;

        if (resultType != null) {
            this.resultType = resultType;
        } else {
            this.resultType = contentType;
        }
    }

    public static SupportedType getSupported(String contentType) {
        return Stream.of(values())
              .filter(type -> Objects.equals(type.getContentType(), contentType))
              .findFirst()
              .orElse(null);
    }
}
