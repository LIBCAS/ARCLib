package cz.inqool.uas.script;

import cz.inqool.uas.index.Labeled;
import lombok.Getter;

@Getter
public enum ScriptType implements Labeled {
    GROOVY("groovy"),
    JAVASCRIPT("javascript"),
    SHELL("shell");

    private String label;

    ScriptType(String label) {
        this.label = label;
    }
}
