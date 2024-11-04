package cz.cas.lib.arclib.domain;

import cz.cas.lib.arclib.domainbase.domain.DatedObject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.BatchSize;

@Setter
@Getter
@FieldNameConstants
@Entity
@Table(name = "arclib_user_settings")
@BatchSize(size = 100)
@NoArgsConstructor
@AllArgsConstructor
public class UserSettings extends DatedObject {

    /**
     * JSON settings
     */
    @Column(length = 10485760)
    private String settings;

    /**
     * Užívateľ
     */
    @ManyToOne
    private User belongsTo;
}
