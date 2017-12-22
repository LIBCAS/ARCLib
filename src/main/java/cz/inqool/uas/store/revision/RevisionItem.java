package cz.inqool.uas.store.revision;

import lombok.*;
import org.hibernate.envers.RevisionType;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Embeddable
public class RevisionItem {
    protected String entityId;

    @Enumerated(EnumType.STRING)
    protected RevisionType operation;
}
