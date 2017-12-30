package cz.inqool.uas.index.solr;

import cz.inqool.uas.index.Labeled;
import lombok.Getter;

@Getter
public enum MyObjectState implements Labeled {
    PRVY("prvy"),
    DRUHY("druhy");

    private String label;

    MyObjectState(String label) {
        this.label = label;
    }
}
