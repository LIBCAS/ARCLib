package cz.inqool.uas.export.pptx;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Definition of one slide.
 *
 */
@Getter
@Setter
public class Slide {
    /**
     * Index of master slide (can be null).
     */
    protected Integer master;

    /**
     * Name of the layout page.
     */
    protected String layout;

    /**
     * Provided data for string replace.
     */
    protected Map<String, Object> data;

    /**
     * Provided pictures for picture replace.
     */
    protected Map<String, Picture> pictures;
}
