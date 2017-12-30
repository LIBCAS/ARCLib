package cz.inqool.uas.dictionary;

import cz.inqool.uas.domain.DictionaryObject;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "uas_dictionary")
public class Dictionary extends DictionaryObject {
    @Lob
    private String description;

    @Fetch(FetchMode.SELECT)
    @ManyToOne
    private Dictionary parent;

    private String code;
}
