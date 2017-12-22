package cz.inqool.uas.action;

import cz.inqool.uas.domain.DictionaryObject;
import cz.inqool.uas.script.ScriptType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;

/**
 * Scriptable action
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "uas_action")
public class Action extends DictionaryObject {
    /**
     * Unique code to reference the action
     */
    protected String code;

    /**
     * Script executed when the action is called
     */
    @Lob
    protected String script;

    /**
     * Language of the script used
     */
    @Enumerated(EnumType.STRING)
    protected ScriptType scriptType;
}
