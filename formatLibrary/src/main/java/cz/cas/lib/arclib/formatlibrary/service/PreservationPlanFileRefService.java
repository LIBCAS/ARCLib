package cz.cas.lib.arclib.formatlibrary.service;

import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import cz.cas.lib.arclib.domainbase.exception.ForbiddenObject;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.formatlibrary.domain.PreservationPlanFileRef;
import cz.cas.lib.arclib.formatlibrary.store.PreservationPlanFileRefStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static cz.cas.lib.arclib.domainbase.util.DomainBaseUtils.notNull;
import static java.nio.file.Files.*;

/**
 * Somewhat mirrored version of <b>PreservationPlanFileRefService</b> from module <b>system</b>.
 *
 * File storage manager.
 *
 * Inserted files are stored in file system in configured directory.
 * File's content can be indexed along the way.
 */
@Slf4j
@Service
public class PreservationPlanFileRefService {

    private PreservationPlanFileRefStore store;
    private String basePath;


    /**
     * Gets a single file for specified id.
     *
     * {@link PreservationPlanFileRef#size} will be initialized and {@link PreservationPlanFileRef#stream} will be
     * opened and prepared for reading.
     *
     * @param id Id of the file
     * @return A single {@link PreservationPlanFileRef}
     * @throws MissingObject If there is no corresponding {@link PreservationPlanFileRef} or the file does not exist on
     *                       file system
     * @throws BadArgument   If the specified id is not an {@link UUID}
     */
    @Transactional
    public PreservationPlanFileRef get(String id) {
        PreservationPlanFileRef PreservationPlanFileRef = store.find(id);
        notNull(PreservationPlanFileRef, () -> new MissingObject(PreservationPlanFileRef.class, id));

        Path path = Paths.get(basePath, id);

        if (isRegularFile(path)) {
            try {
                PreservationPlanFileRef.setStream(newInputStream(path));
                PreservationPlanFileRef.setSize(size(path));
            } catch (IOException e) {
                throw new GeneralException(e);
            }

            return PreservationPlanFileRef;
        } else {
            throw new MissingObject(Path.class, id);
        }
    }

    /**
     * Reopens the {@link PreservationPlanFileRef#stream} and reset it to the beginning.
     *
     * If the {@link PreservationPlanFileRef} was previously opened, the {@link PreservationPlanFileRef#stream} will be
     * firstly closed and
     * then reopened.
     *
     * @param ref Provided {@link PreservationPlanFileRef}
     * @throws MissingObject If the file does not exist on the file system
     */
    public void reset(PreservationPlanFileRef ref) {
        close(ref);

        Path path = Paths.get(basePath, ref.getId());

        if (isRegularFile(path)) {
            try {
                ref.setStream(newInputStream(path));
                ref.setSize(size(path));
            } catch (IOException e) {
                throw new GeneralException(e);
            }

        } else {
            throw new MissingObject(Path.class, ref.getId());
        }
    }

    /**
     * Closes the content stream on {@link PreservationPlanFileRef}.
     *
     * @param ref Provided {@link PreservationPlanFileRef}
     */
    public void close(PreservationPlanFileRef ref) {
        InputStream stream = ref.getStream();

        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                throw new GeneralException(e);
            }
        }

        ref.setStream(null);
        ref.setSize(null);
    }

    /**
     * Gets the {@link PreservationPlanFileRef} without opening the content stream.
     *
     * Developer can later open the {@link PreservationPlanFileRef#stream} with {@link #reset}.
     *
     * @param id Id of the file
     * @return A single {@link PreservationPlanFileRef}
     * @throws MissingObject If there is no corresponding {@link PreservationPlanFileRef}
     * @throws BadArgument   If the specified id is not an {@link UUID}
     */
    @Transactional
    public PreservationPlanFileRef getRef(String id) {
        PreservationPlanFileRef PreservationPlanFileRef = store.find(id);
        notNull(PreservationPlanFileRef, () -> new MissingObject(PreservationPlanFileRef.class, id));

        return PreservationPlanFileRef;
    }

    /**
     * Saves a file.
     *
     * Failure to index the content will not produce exception.
     *
     * @param stream      Content stream to save
     * @param name        Name of the file
     * @param contentType MIME type
     * @return Newly created {@link PreservationPlanFileRef}
     * @throws BadArgument If any argument is null
     */
    @Transactional
    public PreservationPlanFileRef create(InputStream stream, String name, String contentType) {
        notNull(stream, () -> new BadArgument("stream"));
        notNull(name, () -> new BadArgument("name"));
        notNull(contentType, () -> new BadArgument("contentType"));

        PreservationPlanFileRef ref = new PreservationPlanFileRef();
        ref.setName(name);
        ref.setContentType(contentType);

        try {
            Path folder = Paths.get(basePath);

            if (!isDirectory(folder) && exists(folder)) {
                throw new ForbiddenObject(Path.class, ref.getId());
            } else if (!isDirectory(folder)) {
                createDirectories(folder);
            }

            Path path = Paths.get(basePath, ref.getId());
            copy(stream, path);
        } catch (IOException e) {
            throw new GeneralException(e);
        }
        return store.save(ref);
    }

    /**
     * Deletes a file.
     *
     * Deleting non existing file will be silently ignored.
     *
     * @param PreservationPlanFileRef {@link PreservationPlanFileRef} to delete
     * @throws BadArgument If the provided {@link PreservationPlanFileRef} is null
     */
    @Transactional
    public void del(PreservationPlanFileRef PreservationPlanFileRef) {
        notNull(PreservationPlanFileRef, () -> new BadArgument("PreservationPlanFileRef"));

        Path path = Paths.get(basePath, PreservationPlanFileRef.getId());

        store.delete(PreservationPlanFileRef);

        if (exists(path)) {
            try {
                delete(path);
            } catch (IOException e) {
                throw new GeneralException(e);
            }
        } else {
            log.warn("File {} not found.", path);
        }
    }

    /**
     * Specifies the path on file system, where the files should be saved.
     *
     * Destination folder is shared with files from FileRefService.
     *
     * @param basePath Path on file system
     */
    @Inject
    public void setBasePath(@Value("${file.path:data}") String basePath) {
        this.basePath = basePath;
    }

    @Inject
    public void setStore(PreservationPlanFileRefStore store) {
        this.store = store;
    }
}
