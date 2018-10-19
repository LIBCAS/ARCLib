package cz.cas.lib.core.index.example;

import cz.cas.lib.core.domain.DictionaryObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;

@Getter
@Setter
@Entity
public class MyObject extends DictionaryObject {
    private MyObjectState state;
}
