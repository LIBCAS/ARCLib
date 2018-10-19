package cz.cas.lib.core.index.example;

import cz.cas.lib.core.index.Labeled;
import lombok.Getter;

@Getter
public enum MyObjectState implements Labeled {
    FST("fst"),
    SND("snd");

    private String label;

    MyObjectState(String label) {
        this.label = label;
    }
}
