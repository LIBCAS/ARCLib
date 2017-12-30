package cz.inqool.uas.export.pptx;

import lombok.Getter;
import lombok.Setter;

/**
 * Provided picture.
 *
 * Content is base64 encoded
 */
@Getter
@Setter
public class Picture {
    private String content;
    private ImageType type;
}
