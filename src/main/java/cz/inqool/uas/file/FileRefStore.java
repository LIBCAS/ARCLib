package cz.inqool.uas.file;

import cz.inqool.uas.index.IndexedDatedStore;
import org.springframework.stereotype.Repository;

/**
 * Implementation of {@link IndexedDatedStore} for storing {@link FileRef} and indexing {@link IndexedFileRef}.
 */
@Repository
public class FileRefStore extends IndexedDatedStore<FileRef, QFileRef, IndexedFileRef> {

    public FileRefStore() {
        super(FileRef.class, QFileRef.class, IndexedFileRef.class);
    }

    @Override
    public IndexedFileRef toIndexObject(FileRef o) {
        IndexedFileRef indexedFileRef = super.toIndexObject(o);

        indexedFileRef.setName(o.getName());
        indexedFileRef.setContent(o.getContent());

        return indexedFileRef;
    }
}
