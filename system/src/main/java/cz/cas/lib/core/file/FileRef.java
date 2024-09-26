package cz.cas.lib.core.file;

import cz.cas.lib.arclib.domainbase.domain.DatedObject;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.io.InputStream;

/**
 * Reference to a file stored in {@link FileRefService}.
 *
 * <p>
 *     Should be created only through {@link FileRefService} and not cascaded from another entity.
 * </p>
 */
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "uas_file")
public class FileRef extends DatedObject {
    /**
     * Filename
     */
    protected String name;

    /**
     * MIME type
     */
    private String contentType;

    /**
     * Indexing status
     */
    private Boolean indexedContent;

    /**
     * Opened stream to read file content
     *
     * <p>
     *     Initialized only if retrieved from {@link FileRefService}.
     * </p>
     */
    @Transient
    private InputStream stream;

    /**
     * Text representation of file content
     *
     * <p>
     *     Initialized only if saved through {@link FileRefService}.
     * </p>
     */
    @Transient
    private String content;

    /**
     * Size of the file content
     *
     * <p>
     *     Initialized only if retrieved from {@link FileRefService}.
     * </p>
     */
    @Transient
    private Long size;
}
