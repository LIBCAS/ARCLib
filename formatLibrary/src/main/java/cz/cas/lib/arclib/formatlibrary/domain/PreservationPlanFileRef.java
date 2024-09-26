package cz.cas.lib.arclib.formatlibrary.domain;

import cz.cas.lib.arclib.domainbase.domain.DatedObject;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.io.InputStream;

/**
 * Somewhat mirrored version of <b>FileRef</b> from module <b>system</b>.
 *
 * Reference to a file.
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "arclib_format_preservation_plan_file")
public class PreservationPlanFileRef extends DatedObject {
    /**
     * Filename
     */
    protected String name;

    /**
     * MIME type
     */
    private String contentType;

    /**
     * Opened stream to read file content
     */
    @Transient
    private InputStream stream;

    /**
     * Size of the file content
     */
    @Transient
    private Long size;
}
