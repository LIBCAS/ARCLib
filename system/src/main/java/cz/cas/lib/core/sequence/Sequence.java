package cz.cas.lib.core.sequence;

import cz.cas.lib.arclib.domainbase.domain.DatedObject;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.text.DecimalFormat;

/**
 * Formatted sequence of numbers.
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "uas_sequence")
public class Sequence extends DatedObject {
    /**
     * Format of the sequence suitable for {@link DecimalFormat} usage.
     *
     * <p>
     * E.g. ISPV-2016/00 is escaped as ISPV'-2'016/00. For further details about the format
     * see <a href="https://docs.oracle.com/javase/8/docs/api/java/text/DecimalFormat.html">DecimalFormat</a>.
     * </p>
     */
    protected String format;

    /**
     * Current value of the counter
     */
    protected Long counter;
}
