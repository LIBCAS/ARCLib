package cz.cas.lib.core.file;

import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.ForbiddenObject;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.core.store.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static cz.cas.lib.core.util.Utils.checked;
import static cz.cas.lib.core.util.Utils.notNull;
import static java.nio.file.Files.*;

/**
 * File storage manager.
 *
 * <p>
 * Inserted files are stored in file system in configured directory.
 * </p>
 * <p>
 * File's content can be indexed along the way.
 * </p>
 */
@Slf4j
@Service
public class FileRepository {
    private FileRefStore store;

    private String basePath;

    /**
     * Gets a single file for specified id.
     *
     * <p>
     * {@link FileRef#size} will be initialized and {@link FileRef#stream} will be opened and prepared for reading.
     * </p>
     *
     * @param id Id of the file
     * @return A single {@link FileRef}
     * @throws MissingObject If there is no corresponding {@link FileRef} or the file does not exist on file system
     * @throws BadArgument   If the specified id is not an {@link UUID}
     */
    @Transactional
    public FileRef get(String id) {
        checkUUID(id);

        FileRef fileRef = store.find(id);
        notNull(fileRef, () -> new MissingObject(FileRef.class, id));

        Path path = Paths.get(basePath, id);

        if (isRegularFile(path)) {
            checked(() -> {
                fileRef.setStream(newInputStream(path));
                fileRef.setSize(size(path));
            });

            return fileRef;
        } else {
            throw new MissingObject(Path.class, id);
        }
    }

    /**
     * Reopens the {@link FileRef#stream} and reset it to the beginning.
     *
     * <p>
     * If the {@link FileRef} was previously opened, the {@link FileRef#stream} will be firstly closed and
     * then reopened.
     * </p>
     *
     * @param ref Provided {@link FileRef}
     * @throws MissingObject If the file does not exist on the file system
     */
    public void reset(FileRef ref) {
        close(ref);

        Path path = Paths.get(basePath, ref.getId());

        if (isRegularFile(path)) {
            checked(() -> {
                ref.setStream(newInputStream(path));
                ref.setSize(size(path));
            });

        } else {
            throw new MissingObject(Path.class, ref.getId());
        }
    }

    /**
     * Closes the content stream on {@link FileRef}.
     *
     * @param ref Provided {@link FileRef}
     */
    public void close(FileRef ref) {
        InputStream stream = ref.getStream();

        if (stream != null) {
            checked(stream::close);
        }

        ref.setStream(null);
        ref.setSize(null);
    }

    /**
     * Gets the {@link FileRef} without opening the content stream.
     *
     * <p>
     * Developer can later open the {@link FileRef#stream} with {@link FileRepository#reset(FileRef)}.
     * </p>
     *
     * @param id Id of the file
     * @return A single {@link FileRef}
     * @throws MissingObject If there is no corresponding {@link FileRef}
     * @throws BadArgument   If the specified id is not an {@link UUID}
     */
    @Transactional
    public FileRef getRef(String id) {
        checkUUID(id);

        FileRef fileRef = store.find(id);
        notNull(fileRef, () -> new MissingObject(FileRef.class, id));

        return fileRef;
    }

    /**
     * Saves a file.
     *
     * <p>
     * Failure to index the content will not produce exception.
     * </p>
     *
     * @param stream       Content stream to save
     * @param name         Name of the file
     * @param contentType  MIME type
     * @param indexContent Should the file content be indexed
     * @return Newly created {@link FileRef}
     * @throws BadArgument If any argument is null
     */
    @Transactional
    public FileRef create(InputStream stream, String name, String contentType, boolean indexContent) {
        notNull(stream, () -> new BadArgument("stream"));
        notNull(name, () -> new BadArgument("name"));
        notNull(contentType, () -> new BadArgument("contentType"));

        FileRef ref = new FileRef();
        ref.setName(name);
        ref.setContentType(contentType);
        ref.setIndexedContent(false);

        checked(() -> {
            Path folder = Paths.get(basePath);

            if (!isDirectory(folder) && exists(folder)) {
                throw new ForbiddenObject(Path.class, ref.getId());
            } else if (!isDirectory(folder)) {
                createDirectories(folder);
            }

            Path path = Paths.get(basePath, ref.getId());
            copy(stream, path);
        });

        return store.save(ref);
    }

    /**
     * Deletes a file.
     *
     * <p>
     * Deleting non existing file will be silently ignored.
     * </p>
     *
     * @param fileRef {@link FileRef} to delete
     * @throws BadArgument If the provided {@link FileRef} is null
     */
    @Transactional
    public void del(FileRef fileRef) {
        notNull(fileRef, () -> new BadArgument("fileRef"));

        Path path = Paths.get(basePath, fileRef.getId());

        store.delete(fileRef);

        if (exists(path)) {
            checked(() -> delete(path));
        } else {
            log.warn("File {} not found.", path);
        }
    }

    private void checkUUID(String id) {
        checked(() -> UUID.fromString(id), () -> new BadArgument(Path.class, id));
    }

    /**
     * Specifies the path on file system, where the files should be saved.
     *
     * @param basePath Path on file system
     */
    @Inject
    public void setBasePath(@Value("${file.path:data}") String basePath) {
        this.basePath = basePath;
    }

    @Inject
    public void setStore(FileRefStore store) {
        this.store = store;
    }
}
