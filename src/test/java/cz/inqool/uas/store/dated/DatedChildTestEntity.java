package cz.inqool.uas.store.dated;

import cz.inqool.uas.domain.DictionaryObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Table;

@Getter
@Setter
@Entity
@Table(name = "test_child_dated")
public class DatedChildTestEntity extends DictionaryObject {

}
