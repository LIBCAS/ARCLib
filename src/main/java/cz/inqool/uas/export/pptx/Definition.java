package cz.inqool.uas.export.pptx;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Definition of slides.
 */
@Getter
@Setter
public class Definition {
    private List<Slide> slides;
}
