package cz.cas.lib.core.dictionary;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.cas.lib.core.domain.DictionaryObject;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "uas_dictionary_value")
public class DictionaryValue extends DictionaryObject {
    @JsonIgnore
    @Fetch(FetchMode.SELECT)
    @ManyToOne
    private Dictionary dictionary;

    @Fetch(FetchMode.SELECT)
    @ManyToOne
    private DictionaryValue parent;

    @Lob
    private String description;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;

    private String code;
}
