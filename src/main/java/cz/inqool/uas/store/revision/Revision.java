package cz.inqool.uas.store.revision;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import javax.persistence.*;
import java.time.Instant;
import java.util.Set;

@Getter
@Setter
@Entity
@RevisionEntity(RevisionListener.class)
@Table(name = "uas_revision")
public class Revision {
    @Id
    @GeneratedValue
    @RevisionNumber
    protected long id;

    @JsonIgnore
    @RevisionTimestamp
    @Column(name = "rev_timestamp")
    private long timestamp;

    protected String author;

    @BatchSize(size = 100)
    @Fetch(FetchMode.SELECT)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name="uas_revision_item",
            joinColumns=@JoinColumn(name="revision_id")
    )
    protected Set<RevisionItem> items;

    public Instant getCreated() {
        return Instant.ofEpochMilli(timestamp);
    }
}
