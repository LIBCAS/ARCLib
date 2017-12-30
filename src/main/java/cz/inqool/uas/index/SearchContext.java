package cz.inqool.uas.index;

import lombok.Getter;
import lombok.Setter;

import java.util.function.BiFunction;

/**
 * Holds all information necessary for indexed searching
 */
@Getter
@Setter
public class SearchContext {
    private String type;
    private String[] indexNames;
    private BiFunction<String, String, String> normalizer;
}
