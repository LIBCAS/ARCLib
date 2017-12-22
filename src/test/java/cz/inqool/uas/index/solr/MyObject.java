package cz.inqool.uas.index.solr;

import cz.inqool.uas.domain.DictionaryObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;

@Getter
@Setter
@Entity
public class MyObject extends DictionaryObject {
    private MyObjectState state;
}
