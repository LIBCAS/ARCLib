package cz.cas.lib.core.store.dated;

import cz.cas.lib.core.domain.DictionaryObject;
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
