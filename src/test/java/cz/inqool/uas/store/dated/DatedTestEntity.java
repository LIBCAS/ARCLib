package cz.inqool.uas.store.dated;

import cz.inqool.uas.domain.DictionaryObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Getter
@Setter
@Entity
@Table(name = "test_dated")
public class DatedTestEntity extends DictionaryObject {
    @ManyToOne(cascade = CascadeType.ALL)
    private DatedChildTestEntity child;
}
