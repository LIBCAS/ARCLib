package cz.cas.lib.core.file;

import cz.cas.lib.core.store.DatedStore;
import org.springframework.stereotype.Repository;

/**
 * Implementation of {@link DatedStore} for storing {@link FileRef}.
 */
@Repository
public class FileRefStore
        extends DatedStore<FileRef, QFileRef> {
    public FileRefStore() {
        super(FileRef.class, QFileRef.class);
    }
}
