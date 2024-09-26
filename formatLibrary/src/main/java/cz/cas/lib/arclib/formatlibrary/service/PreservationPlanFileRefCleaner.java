package cz.cas.lib.arclib.formatlibrary.service;

import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.formatlibrary.domain.Format;
import cz.cas.lib.arclib.formatlibrary.domain.PreservationPlanFileRef;
import cz.cas.lib.arclib.formatlibrary.store.FormatStore;
import cz.cas.lib.arclib.formatlibrary.store.PreservationPlanFileRefStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cleans up file system and DB from orphaned {@link PreservationPlanFileRef} files.
 */
@Slf4j
@Service
public class PreservationPlanFileRefCleaner {

    /**
     * Path to folder where files are stored.
     */
    private String basePath;
    private PreservationPlanFileRefStore store;
    private PreservationPlanFileRefService service;
    private FormatStore formatStore;

    /**
     * Periodically removes orphaned {@link PreservationPlanFileRef} files from DB and filesystem.
     *
     * Files are uploaded before entity saving, therefore
     * if entity was not saved, or file was removed from entity mapping then
     * the file remains in the DB and on the filesystem. These orphaned files must be removed.
     */
    @Scheduled(cron = "${file.cleaning.schedule:0 0 1 * * *}", zone = "Europe/Prague") // Default: 01:00 every day
    public void fileStorageCronCleanup() {
        log.debug("Cleaning of orphaned file entities begins...");

        if (!Files.isDirectory(Paths.get(basePath))) {
            log.debug("Directory: {} was not found. Cleaning is skipped.", basePath);
            return;
        }

        Set<PreservationPlanFileRef> orphanedFiles = findOrphanedFiles();
        log.debug("Found {} orphaned files. Attempting to remove them from DB and filesystem...", orphanedFiles.size());

        orphanedFiles.stream()
                .filter(this::fileIsOldEnough)
                .forEach(service::hardDelete);

        log.debug("Cleaning of orphaned file entities has finished.");
    }

    private Set<PreservationPlanFileRef> findOrphanedFiles() {
        Set<PreservationPlanFileRef> files = new HashSet<>(store.findAll());

        Collection<Format> allFormats = formatStore.findAll();
        Collection<PreservationPlanFileRef> allMappedFiles = allFormats.stream().flatMap(format -> format.getFiles().stream()).collect(Collectors.toSet());

        // drop mapped files from all files so only non-mapped files remain
        allMappedFiles.forEach(files::remove);
        return files;
    }

    private boolean fileIsOldEnough(PreservationPlanFileRef file) {
        try {
            BasicFileAttributes attributes = Files.readAttributes(Paths.get(basePath, file.getId()), BasicFileAttributes.class);
            boolean isOldEnough = attributes.isRegularFile() &&
                    // retain only files that are older than 1 hour (form might not be send yet, transactions are running, etc.)
                    Instant.now().minus(1, ChronoUnit.HOURS).isAfter(attributes.creationTime().toInstant());
            if (!isOldEnough) {
                log.debug("File {} is not ready for removal.", file.getId());
            }
            return isOldEnough;
        } catch (NoSuchFileException e) {
            // entity exists, but there is no file in file system, therefore keep the entity and remove it with service#hardDelete
            return true;
        } catch (IOException e) {
            throw new GeneralException(e);
        }
    }

    /**
     * Specifies the path on file system, where the files should be saved.
     *
     * Destination folder is shared with files from FileRefService.
     *
     * @param basePath Path on file system
     */
    @Autowired
    public void setBasePath(@Value("${file.path:data}") String basePath) {
        this.basePath = basePath;
    }

    @Autowired
    public void setStore(PreservationPlanFileRefStore store) {
        this.store = store;
    }

    @Autowired
    public void setFormatStore(FormatStore formatStore) {
        this.formatStore = formatStore;
    }

    @Autowired
    public void setService(PreservationPlanFileRefService service) {
        this.service = service;
    }
}
