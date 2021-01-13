package cz.cas.lib.core.script;

import cz.cas.lib.core.index.Labeled;
import lombok.Getter;

@Getter
public enum ScriptType implements Labeled {
    GROOVY("groovy"),
    JAVASCRIPT("javascript"),
    SHELL("shell");

    private final String label;

    ScriptType(String label) {
        this.label = label;
    }
}
